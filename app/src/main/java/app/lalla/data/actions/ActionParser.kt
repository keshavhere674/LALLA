package app.lalla.data.actions

import java.util.Calendar
import java.util.Locale

data class ParsedAction(
    val type: ActionType,
    val triggerTimeMs: Long? = null,
    val targetContact: String? = null,
    val targetMessage: String? = null,
    val emailSubject: String? = null,
    val isEmailRead: Boolean = false,
    val searchQuery: String? = null,
    val description: String = ""
)

enum class ActionType {
    ALARM,
    WHATSAPP,
    GMAIL,
    FILE_SEARCH,
    CALL_HISTORY,
    REMINDER,
    GENERIC,
    NONE
}

object ActionParser {

    private val alarmKeywords = listOf(
        "alarm", "अलार्म", "wake me", "wake up",
        "alarm laga", "alarm lagao", "alarm set", "baja do", "jagao",
        "subah jagana", "uthana", "uthaana", "jaga dena"
    )

    private val reminderKeywords = listOf(
        "remind", "reminder", "yaad dila", "yaad kara", "mujhe yaad",
        "याद दिलाना", "याद कराना", "recall", "don't forget", "notify me"
    )

    private val whatsappKeywords = listOf(
        "whatsapp", "व्हाट्सएप", "व्हाट्सएप्प", "message bhej", "message send", "msg bhej",
        "ko bhej", "ko message", "text karo", "whatsapp karo", "message kar"
    )

    private val gmailKeywords = listOf(
        "gmail", "email", "mail", "ईमेल", "मेल", "जीमेल"
    )

    private val fileSearchKeywords = listOf(
        "search file", "find file", "file search", "folder search", "search folder",
        "find folder", "file dhoondo", "folder dhoondo", "dhoondo file", "dhoondho"
    )

    private val callHistoryKeywords = listOf(
        "call history", "call log", "call list", "kiska phone", "kiski call",
        "last call", "missed call", "recent call", "phone aaya tha", "call aayi thi"
    )

    /**
     * Parse user command into a deterministic action.
     */
    fun parse(text: String): ParsedAction? {
        val lower = text.lowercase(Locale.ROOT).trim()
        if (lower.isBlank()) return null

        return when {
            // Call history check (check before search to avoid ambiguity)
            callHistoryKeywords.any { lower.contains(it) } -> parseCallHistory(text, lower)

            // File / folder search
            fileSearchKeywords.any { lower.contains(it) } -> parseFileSearch(text, lower)

            // WhatsApp message
            whatsappKeywords.any { lower.contains(it) } -> parseWhatsApp(text, lower)

            // Gmail / Email
            gmailKeywords.any { lower.contains(it) } -> parseGmail(text, lower)

            // Alarm
            alarmKeywords.any { lower.contains(it) } -> parseAlarm(text, lower)

            // Reminder
            reminderKeywords.any { lower.contains(it) } -> parseReminder(text, lower)

            else -> null
        }
    }

    // ── Alarm Parsing ────────────────────────────────────────────────────────

    private fun parseAlarm(original: String, lower: String): ParsedAction {
        val timeMs = extractTimeMs(lower)
        return ParsedAction(
            type = ActionType.ALARM,
            triggerTimeMs = timeMs,
            description = original
        )
    }

    // ── Reminder Parsing ─────────────────────────────────────────────────────

    private fun parseReminder(original: String, lower: String): ParsedAction {
        val timeMs = extractTimeMs(lower)
        return ParsedAction(
            type = ActionType.REMINDER,
            triggerTimeMs = timeMs,
            description = original
        )
    }

    // ── WhatsApp Parsing ─────────────────────────────────────────────────────

    private fun parseWhatsApp(original: String, lower: String): ParsedAction {
        val contact = extractContact(lower)
        val messageBody = extractMessageBody(original, lower)

        return ParsedAction(
            type = ActionType.WHATSAPP,
            targetContact = contact,
            targetMessage = messageBody,
            description = original
        )
    }

    // ── Gmail Parsing ────────────────────────────────────────────────────────

    private fun parseGmail(original: String, lower: String): ParsedAction {
        val isRead = lower.contains("read") || lower.contains("check") ||
                lower.contains("padho") || lower.contains("dekh") || lower.contains("aaye hain")

        if (isRead) {
            return ParsedAction(
                type = ActionType.GMAIL,
                isEmailRead = true,
                description = original
            )
        }

        // Send mode
        val contact = extractEmailRecipient(lower) ?: extractContact(lower)
        val subject = extractEmailSubject(lower)
        val messageBody = extractMessageBody(original, lower)

        return ParsedAction(
            type = ActionType.GMAIL,
            isEmailRead = false,
            targetContact = contact,
            emailSubject = subject,
            targetMessage = messageBody,
            description = original
        )
    }

    // ── File Search Parsing ──────────────────────────────────────────────────

    private fun parseFileSearch(original: String, lower: String): ParsedAction {
        var query = lower
        for (kw in fileSearchKeywords) {
            query = query.replace(kw, "")
        }
        val cleanQuery = query.replace("karo", "").replace("ko", "").replace("naam ki", "").trim()

        return ParsedAction(
            type = ActionType.FILE_SEARCH,
            searchQuery = cleanQuery.ifBlank { original },
            description = original
        )
    }

    // ── Call History Parsing ─────────────────────────────────────────────────

    private fun parseCallHistory(original: String, lower: String): ParsedAction {
        val contact = extractContact(lower)
        return ParsedAction(
            type = ActionType.CALL_HISTORY,
            targetContact = contact,
            description = original
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun extractTimeMs(lower: String): Long? {
        val cal = Calendar.getInstance()

        val isTomorrow = lower.contains("kal") || lower.contains("tomorrow")
        if (isTomorrow) cal.add(Calendar.DAY_OF_YEAR, 1)

        val explicitAm = lower.contains("am") || lower.contains("a.m") ||
                lower.contains("subah") || lower.contains("morning") || lower.contains("सुबह")

        val explicitPm = lower.contains("pm") || lower.contains("p.m") ||
                lower.contains("shaam") || lower.contains("sham") || lower.contains("raat") ||
                lower.contains("evening") || lower.contains("night") || lower.contains("दोपहर")

        // Regex supports "6:03", "6:03 am", "6.03", "6 baje", "18:00"
        val regex = Regex("""(\d{1,2})(?:[:.](\d{2}))?(?:\s*(am|pm|baje|बजे))?""")
        val match = regex.find(lower) ?: return null

        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0

        // Handle AM/PM logic deterministically
        if (explicitAm) {
            if (hour == 12) hour = 0
        } else if (explicitPm) {
            if (hour < 12) hour += 12
        } else {
            // No explicit marker:
            // If hour is between 1 and 6, and current time is already afternoon, default to morning or next available
            if (hour in 1..6) {
                // If the user says "set alarm for 6", check if it's currently day or night
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (currentHour < 6) {
                    // It's early morning (e.g. 2 AM), 6 means 6 AM today
                    // Keep hour as 6
                } else if (currentHour < 18) {
                    // It's daytime, "6" could be 6 PM today or 6 AM tomorrow
                    // In alarm contexts, "6 baje" often means 6 AM tomorrow morning
                    hour = 6
                }
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If time is in the past for today, schedule for tomorrow
        if (!isTomorrow && cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }

    private fun extractContact(lower: String): String? {
        val ignored = listOf("me", "a", "an", "the", "call", "alarm", "whatsapp", "message", "email", "bhejo", "send", "karo", "likho")

        // Hindi pattern: "<Contact> ko" or "<Contact> ke liye" or "<Contact> se"
        val hindiPattern = Regex("""([a-zA-Z\u0900-\u097F]+)\s+(?:ko|ke liye|se)\b""")
        val hindiMatch = hindiPattern.find(lower)?.groupValues?.get(1)?.trim()
        if (hindiMatch != null && !ignored.contains(hindiMatch)) {
            return hindiMatch.replaceFirstChar { it.uppercase() }
        }

        // English pattern: "to <Contact>" or "for <Contact>"
        val englishPattern = Regex("""\b(?:to|for)\s+([a-zA-Z\u0900-\u097F]+(?:\s+[a-zA-Z\u0900-\u097F]+)?)\b""")
        val englishMatch = englishPattern.find(lower)?.groupValues?.get(1)?.trim()
        if (englishMatch != null && !ignored.contains(englishMatch)) {
            return englishMatch.replaceFirstChar { it.uppercase() }
        }

        return null
    }

    private fun extractEmailRecipient(lower: String): String? {
        val emailPattern = Regex("""[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+""")
        return emailPattern.find(lower)?.value
    }

    private fun extractEmailSubject(lower: String): String? {
        val subjPattern = Regex("""(?:subject|vishay)\s+([a-zA-Z0-9\s]+?)(?:\s+(?:saying|that|body|message)|$)""")
        return subjPattern.find(lower)?.groupValues?.get(1)?.trim()
    }

    private fun extractMessageBody(original: String, lower: String): String? {
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
