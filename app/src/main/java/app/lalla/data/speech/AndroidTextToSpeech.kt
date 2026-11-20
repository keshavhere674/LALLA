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
            // Fallback to English
            tts?.setLanguage(Locale.US)
        }
    }

    override fun speak(text: String) {
        if (!isReady) return
        val cleanText = stripMarkdown(text)
        if (cleanText.isBlank()) return

        // Auto-detect language from content
        val locale = detectLocale(cleanText)
        setLanguageForLocale(locale)

        tts?.setSpeechRate(0.9f)
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

    /** Rough heuristic: check if text contains Devanagari script → use Hindi locale */
    private fun detectLocale(text: String): Locale {
        val hasDevanagari = text.any { it.code in 0x0900..0x097F }
        return if (hasDevanagari) Locale("hi", "IN") else Locale.US
    }

    /** Strip basic markdown so TTS doesn't read asterisks and hashtags */
    private fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("#+\\s*"), "")          // headings
            .replace(Regex("\\*+([^*]+)\\*+"), "$1") // bold/italic
            .replace(Regex("`([^`]*)`"), "$1")       // inline code
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1") // links
            .replace(Regex("^[-*>] ", RegexOption.MULTILINE), "") // bullets/blockquotes
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
