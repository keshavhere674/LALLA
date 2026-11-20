package app.lalla.data.speech

import java.util.Locale

object TextNormalizer {

    private val THINK_TAG_REGEX = Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE)
    private val MEMORY_TAG_REGEX = Regex("\\[MEMORY][^\n]*", RegexOption.IGNORE_CASE)
    private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```")
    private val INLINE_CODE_REGEX = Regex("`([^`]*)`")
    private val MARKDOWN_LINK_REGEX = Regex("\\[([^]]+)]\\([^)]+\\)")
    private val HEADER_REGEX = Regex("(^|\\s)#+\\s*")
    private val BULLET_REGEX = Regex("(^|\\n)\\s*[-*•>]+\\s*")
    private val BOLD_ITALIC_REGEX = Regex("[*_]{1,3}([^*_]+)[*_]{1,3}")
    private val EMOJI_REGEX = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|[\\u2600-\\u27BF]")
    private val MULTI_DASH_REGEX = Regex("[-—–]{2,}")
    private val MULTI_DOT_REGEX = Regex("\\.{2,}")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Cleans and normalizes text so Android TTS produces clear, fluent speech
     * without reciting asterisks, markdown syntax, code tags, or bizarre symbols.
     */
    fun normalizeForSpeech(text: String, isHindi: Boolean = false): String {
        if (text.isBlank()) return ""

        var processed = text
        // 1. Remove internal reasoning / tags
        processed = THINK_TAG_REGEX.replace(processed, "")
        processed = MEMORY_TAG_REGEX.replace(processed, "")
        processed = CODE_BLOCK_REGEX.replace(processed, "")

        // 2. Clean markdown
        processed = INLINE_CODE_REGEX.replace(processed, "$1")
        processed = MARKDOWN_LINK_REGEX.replace(processed, "$1")
        processed = BOLD_ITALIC_REGEX.replace(processed, "$1")
        processed = HEADER_REGEX.replace(processed, "")

        // Turn bullets into punctuated pauses so sentences don't collide
        processed = BULLET_REGEX.replace(processed, "")
        processed = processed.replace(Regex("\\n+"), ". ")

        // 3. Currency normalization
        if (isHindi) {
            processed = processed.replace(Regex("[₹]\\s*(\\d+)"), "$1 रुपये")
            processed = processed.replace(Regex("(?i)\\b(rs\\.?|inr)\\s*(\\d+)"), "$2 रुपये")
            processed = processed.replace(Regex("[$]\\s*(\\d+)"), "$1 डॉलर")
        } else {
            processed = processed.replace(Regex("[₹]\\s*(\\d+)"), "$1 rupees")
            processed = processed.replace(Regex("(?i)\\b(rs\\.?|inr)\\s*(\\d+)"), "$2 rupees")
            processed = processed.replace(Regex("[$]\\s*(\\d+)"), "$1 dollars")
        }

        // 4. Common abbreviations expansion
        processed = processed.replace(Regex("(?i)\\bdr\\."), "Doctor")
        processed = processed.replace(Regex("(?i)\\bmr\\."), "Mister")
        processed = processed.replace(Regex("(?i)\\bmrs\\."), "Missus")
        processed = processed.replace(Regex("(?i)\\bapprox\\."), "approximately")
        processed = processed.replace(Regex("(?i)\\be\\.g\\."), "for example")
        processed = processed.replace(Regex("(?i)\\bi\\.e\\."), "that is")
        processed = processed.replace(Regex("(?i)\\bvs\\.?"), "versus")
        processed = processed.replace(Regex("(?i)\\bmin\\b"), "minute")
        processed = processed.replace(Regex("(?i)\\bmins\\b"), "minutes")
        processed = processed.replace(Regex("(?i)\\bhr\\b"), "hour")
        processed = processed.replace(Regex("(?i)\\bhrs\\b"), "hours")
        processed = processed.replace(Regex("(?i)\\bsec\\b"), "second")
        processed = processed.replace(Regex("(?i)\\bsecs\\b"), "seconds")
        processed = processed.replace(Regex("(?i)\\bkm/h\\b"), "kilometer per hour")
        processed = processed.replace(Regex("\\bAI\\b"), "A I")

        // 5. Cadence, pauses, and punctuation
        processed = MULTI_DASH_REGEX.replace(processed, ", ")
        processed = MULTI_DOT_REGEX.replace(processed, "... ")
        processed = EMOJI_REGEX.replace(processed, " ")
        processed = MULTI_SPACE_REGEX.replace(processed, " ")

        return processed.trim()
    }

    /**
     * Determines whether text contains Devanagari script.
     */
    fun hasDevanagari(text: String): Boolean {
        return text.any { it.code in 0x0900..0x097F }
    }

    /**
     * Resolves the best TTS locale for natural pronunciation.
     * Uses hi_IN for Devanagari, and en_IN (Indian English) for Latin script / Hinglish,
     * ensuring proper phonetics rather than robotic American English.
     */
    fun selectLocale(text: String): Locale {
        return if (hasDevanagari(text)) {
            Locale("hi", "IN")
        } else {
            // Indian English locale provides natural phonetics for both English and Hinglish
            Locale("en", "IN")
        }
    }
}
