package app.lalla.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Dedicated, lightweight, offline wake-word and Voice Activity Detection (VAD) engine.
 *
 * KEY RELIABILITY ADVANTAGES:
 * 1. Single continuous AudioRecord session — NO start/stop mic loops or system chimes.
 * 2. Energy + Zero-Crossing Rate (ZCR) VAD distinguishes human voice from environmental hiss/hum.
 * 3. Two-syllable acoustic envelope matching for "LAL-LA" (dual peak with inter-syllable dip).
 * 4. Refractory debounce window prevents false rapid re-triggers.
 * 5. Instant stop & resource release.
 */
class AudioWakeWordDetector(
    private var sensitivityThreshold: Float = 1200f,
    private val onWakeWordDetected: () -> Unit,
    private val onVoiceActivity: (rms: Float) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "AudioWakeWordDetector"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_MS = 80 // 80ms per frame
        private const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_MS) / 1000 // 1280 samples
        private const val COOLDOWN_MS = 2500L // cooldown after wake word
    }

    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var workerThread: Thread? = null

    private var lastDetectionTime = 0L

    // Sliding window of recent frame energies (stores last 15 frames = 1.2s)
    private val energyHistory = FloatArray(15)
    private val zcrHistory = FloatArray(15)
    private var historyIndex = 0

    fun updateSensitivity(threshold: Float) {
        sensitivityThreshold = threshold.coerceIn(400f, 3000f)
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning.get()) return true

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufSize, SAMPLES_PER_FRAME * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRunning.set(true)
            isPaused.set(false)

            workerThread = Thread({ processAudioLoop() }, "LallaWakeWordThread").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
            Log.i(TAG, "AudioWakeWordDetector started successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            onError(e.message ?: "Failed to record audio")
            releaseAudioRecord()
            return false
        }
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    fun stop() {
        isRunning.set(false)
        isPaused.set(false)
        try {
            workerThread?.interrupt()
            workerThread?.join(500)
        } catch (_: Exception) {}
        workerThread = null
        releaseAudioRecord()
        Log.i(TAG, "AudioWakeWordDetector stopped")
    }

    private fun releaseAudioRecord() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    private fun processAudioLoop() {
        val audioBuffer = ShortArray(SAMPLES_PER_FRAME)

        while (isRunning.get()) {
            if (isPaused.get()) {
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            val record = audioRecord ?: break
            val readCount = record.read(audioBuffer, 0, audioBuffer.size)

            if (readCount <= 0) {
                if (readCount == AudioRecord.ERROR_INVALID_OPERATION || readCount == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord read error: $readCount")
                    Thread.sleep(100)
                }
                continue
            }

            // 1. Calculate RMS energy
            var sumOfSquares = 0.0
            var zeroCrossings = 0
            for (i in 0 until readCount) {
                val sample = audioBuffer[i]
                sumOfSquares += sample.toLong() * sample.toLong()
                if (i > 0 && ((audioBuffer[i] >= 0 && audioBuffer[i - 1] < 0) ||
                            (audioBuffer[i] < 0 && audioBuffer[i - 1] >= 0))) {
                    zeroCrossings++
                }
            }

            val rms = sqrt(sumOfSquares / readCount).toFloat()
            val zcr = zeroCrossings.toFloat() / readCount

            onVoiceActivity(rms)

            // 2. Store in circular history buffer
            energyHistory[historyIndex] = rms
            zcrHistory[historyIndex] = zcr
            historyIndex = (historyIndex + 1) % energyHistory.size

            // 3. Evaluate wake-word envelope if above threshold
            val now = System.currentTimeMillis()
            if (now - lastDetectionTime > COOLDOWN_MS) {
                if (matchesLallaEnvelope()) {
                    lastDetectionTime = now
                    Log.i(TAG, "Wake word 'LALLA' detected! RMS=$rms, threshold=$sensitivityThreshold")
                    onWakeWordDetected()
                }
            }
        }
    }

    /**
     * Dual-peak syllable pattern matching for "LAL-LA" / "LA-LA".
     * Checks if the last 8-12 frames (640ms - 960ms) exhibit:
     * - First peak (syllable 1) > threshold
     * - Middle dip (inter-syllable consonant/pause) with >= 25% energy drop
     * - Second peak (syllable 2) > threshold
     * - Vowel-like ZCR (< 0.32) during both peaks
     */
    private fun matchesLallaEnvelope(): Boolean {
        val n = energyHistory.size
        // Unroll history into chronological order
        val orderedEnergy = FloatArray(n)
        val orderedZcr = FloatArray(n)
        for (i in 0 until n) {
            val idx = (historyIndex + i) % n
            orderedEnergy[i] = energyHistory[idx]
            orderedZcr[i] = zcrHistory[idx]
        }

        // Look for pattern in the last 12 frames (approx 960ms window)
        // Window segments:
        // [start..peak1..dip..peak2..end]
        val searchLen = minOf(12, n)
        val offset = n - searchLen

        var peak1Idx = -1
        var peak1Val = 0f
        var peak2Idx = -1
        var peak2Val = 0f
        var dipIdx = -1
        var dipVal = Float.MAX_VALUE

        // Find peak 1 in first half
        val half = searchLen / 2
        for (i in 0 until half) {
            val e = orderedEnergy[offset + i]
            if (e > peak1Val && e >= sensitivityThreshold) {
                peak1Val = e
                peak1Idx = i
            }
        }
        if (peak1Idx == -1) return false

        // Find peak 2 in second half
        for (i in half until searchLen) {
            val e = orderedEnergy[offset + i]
            if (e > peak2Val && e >= sensitivityThreshold * 0.85f) {
                peak2Val = e
                peak2Idx = i
            }
        }
        if (peak2Idx == -1) return false

        // Ensure spacing between peaks: at least 2 frames (160ms) apart
        if (peak2Idx - peak1Idx < 2) return false

        // Find minimum dip between peak 1 and peak 2
        for (i in peak1Idx + 1 until peak2Idx) {
            val e = orderedEnergy[offset + i]
            if (e < dipVal) {
                dipVal = e
                dipIdx = i
            }
        }
        if (dipIdx == -1) return false

        // Verify dip is noticeably lower than the average of both peaks (at least 20% lower)
        val avgPeak = (peak1Val + peak2Val) / 2f
        val hasClearDip = dipVal < avgPeak * 0.78f

        // Verify voiced ZCR (not white noise or friction sound)
        val p1Zcr = orderedZcr[offset + peak1Idx]
        val p2Zcr = orderedZcr[offset + peak2Idx]
        val isVoiced = p1Zcr < 0.32f && p2Zcr < 0.32f

        return hasClearDip && isVoiced
    }
}
