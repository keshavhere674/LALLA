package app.lalla.data.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TextNormalizerTest {

    @Test
    fun testStripMarkdownAndEmojis() {
        val input = "Hello **world**! # Title\n- Item 1\n- Item 2 😊 [Click](https://example.com) `code`"
        val output = TextNormalizer.normalizeForSpeech(input, isHindi = false)
        assertFalse(output.contains("#"))
        assertFalse(output.contains("**"))
        assertFalse(output.contains("`"))
        assertFalse(output.contains("["))
        assertFalse(output.contains("😊"))
        assertTrue(output.contains("Hello world!"))
    }

    @Test
    fun testCurrencyExpansionHindi() {
        val input = "इसकी कीमत ₹500 है।"
        val output = TextNormalizer.normalizeForSpeech(input, isHindi = true)
        assertEquals("इसकी कीमत 500 रुपये है।", output)
    }

    @Test
    fun testCurrencyExpansionEnglish() {
        val input = "The price is ₹500 or $10."
        val output = TextNormalizer.normalizeForSpeech(input, isHindi = false)
        assertEquals("The price is 500 rupees or 10 dollars.", output)
    }

    @Test
    fun testAbbreviationExpansion() {
        val input = "Dr. Sharma will arrive in 15 mins. Speed is 60 km/h."
        val output = TextNormalizer.normalizeForSpeech(input, isHindi = false)
        assertTrue(output.contains("Doctor Sharma"))
        assertTrue(output.contains("15 minutes"))
        assertTrue(output.contains("60 kilometer per hour"))
    }

    @Test
    fun testLocaleSelection() {
        val hindi = "नमस्ते, आप कैसे हैं?"
        val hinglish = "Namaste, aap kaise ho?"
        val english = "Good morning, how are you?"

        assertEquals(Locale("hi", "IN"), TextNormalizer.selectLocale(hindi))
        assertEquals(Locale("en", "IN"), TextNormalizer.selectLocale(hinglish))
        assertEquals(Locale("en", "IN"), TextNormalizer.selectLocale(english))
    }
}
