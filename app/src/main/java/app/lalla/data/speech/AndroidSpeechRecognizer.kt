package app.lalla.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import app.lalla.ui.speech.SttResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android SpeechRecognizer implementation.
 * NOTE: Requires internet on most Android devices (Google STT cloud).
 * Also requires RECORD_AUDIO permission granted at runtime.
 */
@Singleton
class AndroidSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) : app.lalla.ui.speech.SpeechRecognizer {

    private var recognizer: SpeechRecognizer? = null

    override fun startListening(): Flow<SttResult> = callbackFlow {
        // Create on UI thread isn't required for Android 13+, but we do it safely
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Support Hindi + English; fallback to device locale
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(SttResult.Partial(partial))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                trySend(SttResult.Final(text))
                close()
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Please try again."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Voice recognition requires internet."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Check your connection."
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone error. Check permissions."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
                    else -> "Voice recognition error ($error)."
                }
                trySend(SttResult.Error(msg))
                close()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        sr.startListening(intent)

        awaitClose {
            sr.stopListening()
            sr.destroy()
            recognizer = null
        }
    }

    override fun stopListening() {
        recognizer?.stopListening()
    }
}
