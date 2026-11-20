package app.lalla.data.actions

import java.util.Calendar
import java.util.Locale

data class ParsedAction(
    val type: ActionType,
    val triggerTimeMs: Long? = null,
    val targetContact: String? = null,
    val targetMessage: String? = null,
    val description: String = ""
)

enum class ActionType { ALARM, WHATSAPP, REMINDER, NONE }

object ActionParser {

    private val alarmKeywords = listOf(
        "alarm", "अलार्म", "wake me", "wake up",
        "alarm laga", "alarm lagao", "alarm set", "baja do", "jagao",
        "subah jagana", "uthana", "uthaana"
    )
    private val reminderKeywords = listOf(
        "remind", "reminder", "yaad dila", "yaad kara", "mujhe yaad",
        "याद दिलाना", "याद कराना", "recall", "don't forget", "notify me"
    )
    private val whatsappKeywords = listOf(
        "whatsapp", "व्हाट्सएप", "message bhej", "message send", "msg bhej",
        "ko bhej", "ko message", "ko likho", "text karo", "bata do",
        "send message", "whatsapp karo", "message kar"
    )

    /**
     * Parse a free-form user message and extract an actionable task.
     * Returns null if no actionable intent is detected.
     */
    fun parse(text: String): ParsedAction? {
        val lower = text.lowercase(Locale.ROOT)

        return when {
            alarmKeywords.any { lower.contains(it) } -> parseAlarm(text, lower)
            reminderKeywords.any { lower.contains(it) } -> parseReminder(text, lower)
            whatsappKeywords.any { lower.contains(it) } -> parseWhatsApp(text, lower)
            else -> null
        }
    }

    // ── Alarm parsing ────────────────────────────────────────────────────────

    private fun parseAlarm(original: String, lower: String): ParsedAction {
        val timeMs = extractTimeMs(lower)
        return ParsedAction(
            type = ActionType.ALARM,
            triggerTimeMs = timeMs,
            description = original
        )
    }

    // ── Reminder parsing ─────────────────────────────────────────────────────

    private fun parseReminder(original: String, lower: String): ParsedAction {
        val timeMs = extractTimeMs(lower)
        return ParsedAction(
            type = ActionType.REMINDER,
            triggerTimeMs = timeMs,
            description = original
        )
    }

    // ── WhatsApp parsing ─────────────────────────────────────────────────────

    private fun parseWhatsApp(original: String, lower: String): ParsedAction {
        // Try to extract contact name: "send message to X" / "X ko message bhejo"
        val contact = extractContact(lower)
        // Try to extract message body after "saying"/"likho"/"ke bolo"/"message:"
        val messageBody = extractMessageBody(original, lower)

        return ParsedAction(
            type = ActionType.WHATSAPP,
            targetContact = contact,
            targetMessage = messageBody,
            description = original
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts a time in milliseconds from text like:
     * "7 AM", "7 baje", "7:30 PM", "subah 6", "raat 10", "kal subah 8"
     */
    fun extractTimeMs(lower: String): Long? {
        val cal = Calendar.getInstance()

        // Detect "kal" / "tomorrow"
        val isTomorrow = lower.contains("kal ") || lower.contains("tomorrow")
        if (isTomorrow) cal.add(Calendar.DAY_OF_YEAR, 1)

        // Morning hint: subah → AM; raat/sham/evening → PM
        val forcePm = lower.contains("raat") || lower.contains("sham") ||
                lower.contains("evening") || lower.contains("night") || lower.contains("pm")
        val forceAm = lower.contains("subah") || lower.contains("morning") ||
                lower.contains("सुबह") || lower.contains("am")

        // Regex patterns: "7:30", "7 30", "07:00", plain "7", "7 baje"
        val timeRegex = Regex("""(\d{1,2})(?:[: ](\d{2}))?(?:\s*(?:am|pm|baje|बजे))?""")
        val match = timeRegex.find(lower) ?: return null

        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0

        // Apply AM/PM heuristics
        if (hour in 1..6 && !forceAm) hour += 12  // ambiguous small numbers → PM
        if (forcePm && hour < 12) hour += 12
        if (forceAm && hour == 12) hour = 0
        if (hour >= 24) hour -= 12

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If time is already past today and no "tomorrow" hint, schedule for next occurrence
        if (!isTomorrow && cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }

    private fun extractContact(lower: String): String? {
        // "send message to X" / "X ko message"
        val toPattern = Regex("""(?:to|ko|ke liye)\s+([a-zA-Z\u0900-\u097F]+(?:\s+[a-zA-Z\u0900-\u097F]+)?)""")
        return toPattern.find(lower)?.groupValues?.get(1)?.trim()?.replaceFirstChar { it.uppercase() }
    }

    private fun extractMessageBody(original: String, lower: String): String? {
        // After "saying", "bolna", "likho", "that", "ke bolo", "message:"
        val bodyMarkers = listOf("saying ", "bolna ", "bolke ", "likhna ", "that ", "ke bolo ", "message: ", "bolo ki ")
        for (marker in bodyMarkers) {
            val idx = lower.indexOf(marker)
            if (idx >= 0) {
                val body = original.substring(idx + marker.length).trim()
                if (body.isNotBlank()) return body
            }
        }
        return null
    }
}
