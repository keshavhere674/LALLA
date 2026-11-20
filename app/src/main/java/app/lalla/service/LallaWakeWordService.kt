package app.lalla.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.lalla.MainActivity
import app.lalla.R
import app.lalla.data.actions.ActionParser
import app.lalla.data.actions.ActionType
import app.lalla.data.actions.AlarmScheduler
import app.lalla.data.actions.WhatsAppSender
import app.lalla.data.local.db.LallaDatabase
import app.lalla.data.local.db.entity.TaskEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Always-on voice assistant service.
 *
 * HOW IT WORKS:
 * 1. Runs as a foreground service (persistent "LALLA is listening" notification).
 * 2. Continuously listens for the wake word "LALLA" using Android SpeechRecognizer.
 * 3. When wake word detected → plays chime + starts a full command listening session.
 * 4. Parses command for alarms, reminders, WhatsApp, etc. and executes immediately.
 * 5. Responds via TTS — no need to open the app.
 * 6. Loops back to wake-word listening.
 *
 * NOTE: Android SpeechRecognizer requires internet (Google STT cloud).
 *       TTS is fully offline (Google TTS with downloaded voices).
 */
class LallaWakeWordService : Service() {

    companion object {
        const val ACTION_START = "app.lalla.WAKE_START"
        const val ACTION_STOP  = "app.lalla.WAKE_STOP"
        const val CHANNEL_ID   = "lalla_foreground"
        const val NOTIF_ID     = 1001

        private val WAKE_WORDS = listOf(
            "lalla", "ला ला", "लल्ला", "lala", "halla", "hello lalla", "hey lalla", "la la"
        )
    }

    // ── Hilt EntryPoint for non-AndroidEntryPoint services ───────────────────
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WakeServiceEntryPoint {
        fun database(): LallaDatabase
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: LallaDatabase

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private enum class State { IDLE, WAKE_LISTENING, COMMAND_LISTENING, PROCESSING }
    private var state = State.IDLE

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        ensureChannels()
        // Inject via EntryPoint (plain Service can't use @Inject directly with Hilt)
        val ep = EntryPointAccessors.fromApplication(applicationContext, WakeServiceEntryPoint::class.java)
        database = ep.database()
        initTts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdown()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIF_ID, buildNotification("Listening for \"LALLA\"..."))
        startWakeListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        shutdown()
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                ttsReady = true
            }
        }
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!ttsReady) { mainHandler.post { onDone?.invoke() }; return }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { mainHandler.post { onDone?.invoke() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { mainHandler.post { onDone?.invoke() } }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lalla_wake")
    }

    // ── Wake-word loop ────────────────────────────────────────────────────────

    private fun startWakeListening() {
        if (!hasAudioPermission()) return
        state = State.WAKE_LISTENING
        updateNotification("Listening for \"LALLA\"...")

        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(WakeListener())
            speechRecognizer?.startListening(buildIntent(isWake = true))
        }
    }

    private inner class WakeListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.lowercase() ?: return
            if (WAKE_WORDS.any { partial.contains(it) }) {
                speechRecognizer?.stopListening()
                onWakeWordDetected()
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.lowercase() ?: ""
            if (WAKE_WORDS.any { text.contains(it) }) {
                onWakeWordDetected()
            } else {
                mainHandler.postDelayed({ startWakeListening() }, 300)
            }
        }

        override fun onError(error: Int) {
            val delay = when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2000L
                else -> 500L
            }
            mainHandler.postDelayed({ startWakeListening() }, delay)
        }
    }

    // ── Command listening ─────────────────────────────────────────────────────

    private fun onWakeWordDetected() {
        state = State.COMMAND_LISTENING
        updateNotification("Listening for command...")
        speak("Haan, boliye") {
            mainHandler.postDelayed({ startCommandListening() }, 400)
        }
    }

    private fun startCommandListening() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(CommandListener())
            speechRecognizer?.startListening(buildIntent(isWake = false))
        }
    }

    private inner class CommandListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onResults(results: Bundle?) {
            val command = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: ""
            processCommand(command)
        }

        override fun onError(error: Int) {
            speak("Mujhe sunai nahi diya, phir boliye") {
                mainHandler.postDelayed({ startWakeListening() }, 500)
            }
        }
    }

    // ── Command processing ────────────────────────────────────────────────────

    private fun processCommand(command: String) {
        if (command.isBlank()) {
            speak("Koi command nahi mili. Main phir sun raha hoon.") {
                mainHandler.postDelayed({ startWakeListening() }, 500)
            }
            return
        }

        state = State.PROCESSING
        updateNotification("Processing: \"$command\"")

        serviceScope.launch {
            val action = ActionParser.parse(command)

            if (action == null) {
                val response = "Yeh command samajh nahi aayi. App khol ke poochiye."
                mainHandler.post {
                    speak(response) {
                        openAppWithCommand(command)
                        mainHandler.postDelayed({ startWakeListening() }, 1500)
                    }
                }
                return@launch
            }

            val taskId = database.taskDao().insert(
                TaskEntity(
                    description = command,
                    type = action.type.name,
                    targetContact = action.targetContact,
                    targetMessage = action.targetMessage,
                    triggerTimeMs = action.triggerTimeMs,
                    isCompleted = false
                )
            )

            val responseText = when (action.type) {
                ActionType.ALARM -> {
                    val timeStr = action.triggerTimeMs?.let {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
                    } ?: "requested time"
                    action.triggerTimeMs?.let { ms ->
                        AlarmScheduler.setAlarm(this@LallaWakeWordService, ms, command, taskId)
                        val (h, m) = AlarmScheduler.hourMinuteFromMs(ms)
                        AlarmScheduler.openSystemAlarm(this@LallaWakeWordService, h, m, command)
                    }
                    "Alarm set kar diya $timeStr ke liye."
                }
                ActionType.REMINDER -> {
                    val timeStr = action.triggerTimeMs?.let {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
                    } ?: "requested time"
                    action.triggerTimeMs?.let { ms ->
                        AlarmScheduler.setAlarm(this@LallaWakeWordService, ms, command, taskId)
                    }
                    "Reminder set kar diya $timeStr ke liye."
                }
                ActionType.WHATSAPP -> {
                    if (WhatsAppSender.isInstalled(this@LallaWakeWordService)) {
                        WhatsAppSender.send(this@LallaWakeWordService, action.targetContact, action.targetMessage)
                        "WhatsApp khol raha hoon ${action.targetContact ?: ""} ke liye."
                    } else {
                        "WhatsApp install nahi hai."
                    }
                }
                else -> "Kaam ho gaya."
            }

            mainHandler.post {
                speak(responseText) {
                    mainHandler.postDelayed({ startWakeListening() }, 500)
                }
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun openAppWithCommand(command: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("voice_command", command)
        }
        startActivity(intent)
    }

    private fun buildIntent(isWake: Boolean) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        if (isWake) {
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        } else {
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }
    }

    private fun buildNotification(text: String) = run {
        val tapIntent = PendingIntent.getActivity(
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
            .setContentTitle("LALLA 👂")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun shutdown() {
        state = State.IDLE
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    android.app.NotificationChannel(CHANNEL_ID, "LALLA Active", NotificationManager.IMPORTANCE_LOW)
                )
            }
            if (nm.getNotificationChannel(TaskExecutorReceiver.CHANNEL_TASKS) == null) {
                nm.createNotificationChannel(
                    android.app.NotificationChannel(TaskExecutorReceiver.CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }
}
