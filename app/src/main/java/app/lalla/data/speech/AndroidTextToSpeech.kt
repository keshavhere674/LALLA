package app.lalla.data.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class TtsState {
    object Idle : TtsState()
    object Speaking : TtsState()
    object Error : TtsState()
}

/**
 * Android TTS implementation — fully OFFLINE, no internet required.
 */
@Singleton
class AndroidTextToSpeech @Inject constructor(
    @ApplicationContext private val context: Context
) : app.lalla.ui.speech.TextToSpeech {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state

    init {
        initialize()
    }

    private fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                // Set default to Hindi; will auto-detect per utterance
                setLanguageForLocale(Locale("hi", "IN"))
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking
            }
            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Idle
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error
            }
        })
    }

    private fun setLanguageForLocale(locale: Locale) {
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to en_IN or US
            val fallback = tts?.setLanguage(Locale("en", "IN"))
            if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        }

        // Try to prefer an offline, high-quality voice for this locale
        try {
            val matchingVoices = tts?.voices?.filter { voice ->
                voice.locale.language == locale.language &&
                (locale.country.isEmpty() || voice.locale.country == locale.country) &&
                !voice.isNetworkConnectionRequired
            }
            matchingVoices?.firstOrNull()?.let { offlineVoice ->
                tts?.voice = offlineVoice
            }
        } catch (_: Exception) {
            // Voice selection is optional; locale is already set
        }
    }

    override fun speak(text: String) {
        if (!isReady) return
        val isHindi = TextNormalizer.hasDevanagari(text)
        val cleanText = TextNormalizer.normalizeForSpeech(text, isHindi)
        if (cleanText.isBlank()) return

        val locale = TextNormalizer.selectLocale(cleanText)
        setLanguageForLocale(locale)

        tts?.setSpeechRate(1.0f)
        tts?.setPitch(1.0f)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    override fun stop() {
        tts?.stop()
        _state.value = TtsState.Idle
    }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
