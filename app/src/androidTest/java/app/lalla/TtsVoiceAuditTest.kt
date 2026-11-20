package app.lalla

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class TtsVoiceAuditTest {

    @Test
    fun auditAvailableTtsVoices() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initDeferred = CompletableDeferred<TextToSpeech>()

        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initDeferred.complete(ttsInstance!!)
            } else {
                initDeferred.completeExceptionally(RuntimeException("TTS init failed with status: $status"))
            }
        }

        val tts = withTimeoutOrNull(5000) { initDeferred.await() }
        if (tts == null) {
            println("TTS Audit: Failed to initialize TextToSpeech within 5 seconds")
            return@runBlocking
        }

        println("=== TTS AUDIT REPORT ===")
        val defaultEngine = tts.defaultEngine
        println("Default TTS Engine: $defaultEngine")

        val hiInAvailable = tts.isLanguageAvailable(Locale("hi", "IN"))
        val enInAvailable = tts.isLanguageAvailable(Locale("en", "IN"))
        val enUsAvailable = tts.isLanguageAvailable(Locale.US)

        println("Language availability:")
        println("  hi_IN: $hiInAvailable (LANG_AVAILABLE=${TextToSpeech.LANG_AVAILABLE}, LANG_COUNTRY_AVAILABLE=${TextToSpeech.LANG_COUNTRY_AVAILABLE}, LANG_COUNTRY_VAR_AVAILABLE=${TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE})")
        println("  en_IN: $enInAvailable")
        println("  en_US: $enUsAvailable")

        val voices = try {
            tts.voices
        } catch (e: Exception) {
            emptySet<Voice>()
        }

        println("Total Voices Found: ${voices?.size ?: 0}")
        voices?.filter { voice ->
            val lang = voice.locale.language
            val country = voice.locale.country
            lang == "hi" || (lang == "en" && country == "IN")
        }?.forEach { voice ->
            println("  Voice: name=${voice.name}, locale=${voice.locale}, requiresNetwork=${voice.isNetworkConnectionRequired}, latency=${voice.latency}, quality=${voice.quality}")
        }

        tts.shutdown()
    }
}
