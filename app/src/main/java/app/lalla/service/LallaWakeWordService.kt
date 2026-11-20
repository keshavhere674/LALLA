package app.lalla.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.lalla.MainActivity
import app.lalla.R
import app.lalla.data.actions.ActionParser
import app.lalla.data.actions.ActionType
import app.lalla.data.actions.ParsedAction
import app.lalla.data.settings.AppSettings
import app.lalla.data.tasks.TaskExecutionEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class LallaWakeWordService : Service() {

    companion object {
        private const val TAG = "LallaWakeWordService"
        const val ACTION_START = "app.lalla.WAKE_START"
        const val ACTION_STOP  = "app.lalla.WAKE_STOP"
        const val CHANNEL_ID   = "lalla_foreground"
        const val NOTIF_ID     = 1001
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WakeServiceEntryPoint {
        fun appSettings(): AppSettings
        fun taskExecutionEngine(): TaskExecutionEngine
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var appSettings: AppSettings
    private lateinit var taskExecutionEngine: TaskExecutionEngine

    private var detector: AudioWakeWordDetector? = null
    private var commandRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()

        val ep = EntryPointAccessors.fromApplication(applicationContext, WakeServiceEntryPoint::class.java)
        appSettings = ep.appSettings()
        taskExecutionEngine = ep.taskExecutionEngine()

        initTts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !appSettings.isBackgroundListeningEnabledSync()) {
            Log.i(TAG, "Stop action received or background listening is disabled")
            appSettings.setBackgroundListeningEnabled(false)
            shutdown()
            return START_NOT_STICKY
        }

        // Verify microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start service: RECORD_AUDIO permission missing")
            shutdown()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification("Listening for \"LALLA\"..."))
        startWakeWordDetector()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        shutdown()
    }

    // ── TTS Initialization ───────────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                isTtsReady = true
            }
        }
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isTtsReady || tts == null) {
            onDone?.invoke()
            return
        }
        val utteranceId = "lalla_spoken_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                mainHandler.post { onDone?.invoke() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                mainHandler.post { onDone?.invoke() }
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    // ── Wake Word Detector ───────────────────────────────────────────────────

    private fun startWakeWordDetector() {
        val sensitivity = appSettings.getWakeWordSensitivitySync()

        detector?.stop()
        detector = AudioWakeWordDetector(
            sensitivityThreshold = sensitivity,
            onWakeWordDetected = {
                mainHandler.post { onWakeWordTriggered() }
            },
            onError = { error ->
                Log.e(TAG, "AudioWakeWordDetector error: $error")
            }
        )

        val success = detector?.start() ?: false
        if (!success) {
            Log.e(TAG, "Failed to start AudioWakeWordDetector")
            updateNotification("Microphone busy or unavailable")
        } else {
            updateNotification("Active: Say \"LALLA\" to command")
        }
    }

    private fun onWakeWordTriggered() {
        Log.i(TAG, "Wake word triggered! Prompting user...")
        detector?.pause()
        updateNotification("LALLA heard! Listening for command...")

        speak("Haan, boliye") {
            startCommandCapture()
        }
    }

    // ── Command Capture (Single-shot) ────────────────────────────────────────

    private fun startCommandCapture() {
        mainHandler.post {
            commandRecognizer?.destroy()
            commandRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            }

            commandRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    handleCommandText(text)
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "Command recognizer error: $error")
                    // Do not speak an annoying error loop. Simply resume wake listening.
                    resumeWakeListening()
                }
            })

            commandRecognizer?.startListening(intent)
        }
    }

    private fun handleCommandText(command: String) {
        if (command.isBlank()) {
            resumeWakeListening()
            return
        }

        Log.i(TAG, "User command received: \"$command\"")
        updateNotification("Processing: \"$command\"")

        serviceScope.launch {
            val parsedAction = ActionParser.parse(command)

            if (parsedAction != null && parsedAction.type != ActionType.NONE) {
                // Execute deterministic task
                val result = taskExecutionEngine.executeAction(parsedAction)
                speak(result.spokenFeedback) {
                    resumeWakeListening()
                }
            } else {
                // Not an automated task; prompt user to open app for chat
                speak("Maine suna: $command. Iska uttar app mein dekhein.") {
                    resumeWakeListening()
                }
            }
        }
    }

    private fun resumeWakeListening() {
        mainHandler.post {
            commandRecognizer?.destroy()
            commandRecognizer = null
            detector?.resume()
            updateNotification("Active: Say \"LALLA\" to command")
        }
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private fun buildNotification(statusText: String) = run {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LallaWakeWordService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LALLA Background Voice")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn OFF", stopIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(statusText))
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "LALLA Background Listening",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows status while LALLA is waiting for the wake word"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    // ── Clean Resource Teardown ──────────────────────────────────────────────

    private fun shutdown() {
        Log.i(TAG, "Shutting down LallaWakeWordService")
        mainHandler.removeCallbacksAndMessages(null)
        detector?.stop()
        detector = null
        commandRecognizer?.destroy()
        commandRecognizer = null
        tts?.shutdown()
        tts = null
        isTtsReady = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
