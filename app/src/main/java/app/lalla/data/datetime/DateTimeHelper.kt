package app.lalla.data.datetime

import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeHelper {

    enum class QueryType {
        TODAY_DATE,
        TODAY_DAY,
        CURRENT_TIME,
        TOMORROW_DATE,
        TOMORROW_DAY,
        YESTERDAY_DATE,
        YESTERDAY_DAY
    }

    enum class Language {
        DEVANAGARI,
        ENGLISH,
        HINGLISH
    }

    private val hindiDays = mapOf(
        "MONDAY" to ("सोमवार" to "Somwar"),
        "TUESDAY" to ("मंगलवार" to "Mangalwar"),
        "WEDNESDAY" to ("बुधवार" to "Budhwar"),
        "THURSDAY" to ("गुरुवार" to "Guruwar"),
        "FRIDAY" to ("शुक्रवार" to "Shukrawar"),
        "SATURDAY" to ("शनिवार" to "Shaniwar"),
        "SUNDAY" to ("रविवार" to "Raviwar")
    )

    private val hindiMonths = mapOf(
        1 to ("जनवरी" to "January"),
        2 to ("फ़रवरी" to "February"),
        3 to ("मार्च" to "March"),
        4 to ("अप्रैल" to "April"),
        5 to ("मई" to "May"),
        6 to ("जून" to "June"),
        7 to ("जुलाई" to "July"),
        8 to ("अगस्त" to "August"),
        9 to ("सितंबर" to "September"),
        10 to ("अक्टूबर" to "October"),
        11 to ("नवंबर" to "November"),
        12 to ("दिसंबर" to "December")
    )

    /**
     * Tries to resolve a direct date or time query deterministically using device clock.
     * Returns the formatted answer or null if the message is not a direct date/time question.
     */
    fun tryResolveDateOrTime(input: String): String? {
        val trimmed = input.trim()
        val queryType = detectQueryType(trimmed) ?: return null
        val language = detectLanguage(trimmed)

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        return formatResponse(queryType, language, now)
    }

    fun detectQueryType(input: String): QueryType? {
        val lower = input.lowercase(Locale.ROOT)
            .replace(Regex("[?,.!\n\r]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 1. Time queries
        if (isTimeQuery(lower)) return QueryType.CURRENT_TIME

        // 2. Tomorrow queries
        if (isTomorrowDateQuery(lower)) return QueryType.TOMORROW_DATE
        if (isTomorrowDayQuery(lower)) return QueryType.TOMORROW_DAY

        // 3. Yesterday queries
        if (isYesterdayDateQuery(lower)) return QueryType.YESTERDAY_DATE
        if (isYesterdayDayQuery(lower)) return QueryType.YESTERDAY_DAY

        // 4. Today Day queries
        if (isTodayDayQuery(lower)) return QueryType.TODAY_DAY

        // 5. Today Date queries
        if (isTodayDateQuery(lower)) return QueryType.TODAY_DATE

        return null
    }

    private fun isTimeQuery(s: String): Boolean {
        // e.g. "abhi kitne baje hain", "kya time hua hai", "time kya hai", "what time is it", "current time"
        if (s.contains("kitne baje") || s.contains("kitna baj") || s.contains("kitne baj")) return true
        if (s.contains("time kya") || s.contains("kya time") || s.contains("time batao") || s.contains("time btao")) return true
        if (s.contains("abhi ka time") || s.contains("current time") || s.contains("what time is it") || s.contains("what's the time") || s.contains("what is the time")) return true
        if (s == "time" || s == "samay" || s == "वक्त" || s == "समय") return true
        return false
    }

    private fun isTomorrowDateQuery(s: String): Boolean {
        if (!s.contains("kal") && !s.contains("tomorrow") && !s.contains("कल")) return false
        // Must be date related and future
        if (s.contains("date") || s.contains("tarikh") || s.contains("taareekh") || s.contains("तारीख")) {
            if (s.contains("hogi") || s.contains("hoga") || s.contains("hai") || s.contains("tomorrow") || s.contains("होगा") || s.contains("होगी")) {
                return true
            }
        }
        return false
    }

    private fun isTomorrowDayQuery(s: String): Boolean {
        if (!s.contains("kal") && !s.contains("tomorrow") && !s.contains("कल")) return false
        if (s.contains("din") || s.contains("day") || s.contains("vaar") || s.contains("वार") || s.contains("दिन")) {
            if (s.contains("hoga") || s.contains("hogi") || s.contains("hai") || s.contains("tomorrow") || s.contains("होगा") || s.contains("होगी")) {
                return true
            }
        }
        return false
    }

    private fun isYesterdayDateQuery(s: String): Boolean {
        if (s.contains("yesterday")) return true
        if ((s.contains("kal") || s.contains("कल")) && (s.contains("thi") || s.contains("tha") || s.contains("थी") || s.contains("था") || s.contains("beeta"))) {
            if (s.contains("date") || s.contains("tarikh") || s.contains("तारीख")) return true
        }
        return false
    }

    private fun isYesterdayDayQuery(s: String): Boolean {
        if ((s.contains("kal") || s.contains("कल") || s.contains("yesterday")) && (s.contains("thi") || s.contains("tha") || s.contains("थी") || s.contains("था"))) {
            if (s.contains("din") || s.contains("day") || s.contains("दिन")) return true
        }
        return false
    }

    private fun isTodayDayQuery(s: String): Boolean {
        // e.g. "aaj kaunsa din hai", "aaj konsa din hai", "what day is today", "what day is it"
        if (s.contains("what day is today") || s.contains("what day is it") || s.contains("what is today's day") || s.contains("which day is today")) return true
        if (s.contains("kaunsa din") || s.contains("konsa din") || s.contains("kya din") || s.contains("kaun sa din") || s.contains("kon sa din")) return true
        if (s.contains("kaunsa vaar") || s.contains("konsa vaar") || s.contains("कौन सा दिन") || s.contains("कौनसा दिन")) return true
        return false
    }

    private fun isTodayDateQuery(s: String): Boolean {
        // e.g. "aaj kya date hai", "aaj ki date", "today date", "what is today's date"
        if (s.contains("what is today's date") || s.contains("what is the date") || s.contains("today's date") || s.contains("what date is it") || s.contains("today date")) return true
        if (s.contains("aaj") || s.contains("आज") || s.contains("today")) {
            if (s.contains("date") || s.contains("tarikh") || s.contains("taareekh") || s.contains("तारीख")) return true
        }
        if (s == "date" || s == "aaj ki date" || s == "today date" || s == "tarikh") return true
        return false
    }

    fun detectLanguage(input: String): Language {
        // Check for Devanagari characters (Unicode range 0x0900 - 0x097F)
        val hasDevanagari = input.any { it.code in 0x0900..0x097F }
        if (hasDevanagari) return Language.DEVANAGARI

        // Check for English-only indicators
        val lower = input.lowercase(Locale.ROOT)
        val englishWords = listOf("what", "is", "the", "today", "tomorrow", "yesterday", "time", "date", "day", "current")
        val isPureEnglish = englishWords.any { lower.contains(it) } &&
                !lower.contains("aaj") && !lower.contains("kal") && !lower.contains("hai") &&
                !lower.contains("baje") && !lower.contains("tarikh") && !lower.contains("kya")

        return if (isPureEnglish) Language.ENGLISH else Language.HINGLISH
    }

    private fun formatResponse(queryType: QueryType, lang: Language, now: ZonedDateTime): String {
        val dayOfWeek = now.dayOfWeek.name
        val (hindiDayName, hinglishDayName) = hindiDays[dayOfWeek] ?: ("शुक्रवार" to "Friday")
        val englishDayName = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)

        val dayOfMonth = now.dayOfMonth
        val monthNum = now.monthValue
        val (hindiMonthName, hinglishMonthName) = hindiMonths[monthNum] ?: ("नवंबर" to "November")
        val englishMonthName = now.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
        val year = now.year

        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        val formattedTime = now.format(timeFormatter)
        val zoneDisplayName = now.zone.id.substringAfterLast("/")

        when (queryType) {
            QueryType.TODAY_DATE -> {
                return when (lang) {
                    Language.DEVANAGARI -> "आज $dayOfMonth $hindiMonthName $year है ($hindiDayName)।"
                    Language.ENGLISH -> "Today is $englishDayName, $dayOfMonth $englishMonthName $year."
                    Language.HINGLISH -> "Aaj $dayOfMonth $hinglishMonthName $year hai ($englishDayName)."
                }
            }
            QueryType.TODAY_DAY -> {
                return when (lang) {
                    Language.DEVANAGARI -> "आज $hindiDayName है।"
                    Language.ENGLISH -> "Today is $englishDayName."
                    Language.HINGLISH -> "Aaj $englishDayName ($hinglishDayName) hai."
                }
            }
            QueryType.CURRENT_TIME -> {
                return when (lang) {
                    Language.DEVANAGARI -> "अभी $formattedTime हो रहे हैं ($zoneDisplayName)।"
                    Language.ENGLISH -> "The current time is $formattedTime ($zoneDisplayName)."
                    Language.HINGLISH -> "Abhi $formattedTime ho rahe hain ($zoneDisplayName)."
                }
            }
            QueryType.TOMORROW_DATE -> {
                val tomorrow = now.plusDays(1)
                val tDayOfMonth = tomorrow.dayOfMonth
                val (tHindiMonth, tHinglishMonth) = hindiMonths[tomorrow.monthValue] ?: ("नवंबर" to "November")
                val tEngMonth = tomorrow.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                val (tHindiDay, _) = hindiDays[tomorrow.dayOfWeek.name] ?: ("शनिवार" to "Saturday")
                val tEngDay = tomorrow.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                val tYear = tomorrow.year

                return when (lang) {
                    Language.DEVANAGARI -> "कल $tDayOfMonth $tHindiMonth $tYear होगी ($tHindiDay)।"
                    Language.ENGLISH -> "Tomorrow will be $tEngDay, $tDayOfMonth $tEngMonth $tYear."
                    Language.HINGLISH -> "Kal $tDayOfMonth $tHinglishMonth $tYear hogi ($tEngDay)."
                }
            }
            QueryType.TOMORROW_DAY -> {
                val tomorrow = now.plusDays(1)
                val (tHindiDay, tHinglishDay) = hindiDays[tomorrow.dayOfWeek.name] ?: ("शनिवार" to "Saturday")
                val tEngDay = tomorrow.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)

                return when (lang) {
                    Language.DEVANAGARI -> "कल $tHindiDay होगा।"
                    Language.ENGLISH -> "Tomorrow will be $tEngDay."
                    Language.HINGLISH -> "Kal $tEngDay ($tHinglishDay) hoga."
                }
            }
            QueryType.YESTERDAY_DATE -> {
                val yesterday = now.minusDays(1)
                val yDayOfMonth = yesterday.dayOfMonth
                val (yHindiMonth, yHinglishMonth) = hindiMonths[yesterday.monthValue] ?: ("नवंबर" to "November")
                val yEngMonth = yesterday.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                val (yHindiDay, _) = hindiDays[yesterday.dayOfWeek.name] ?: ("गुरुवार" to "Thursday")
                val yEngDay = yesterday.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                val yYear = yesterday.year

                return when (lang) {
                    Language.DEVANAGARI -> "कल $yDayOfMonth $yHindiMonth $yYear थी ($yHindiDay)।"
                    Language.ENGLISH -> "Yesterday was $yEngDay, $yDayOfMonth $yEngMonth $yYear."
                    Language.HINGLISH -> "Kal $yDayOfMonth $yHinglishMonth $yYear thi ($yEngDay)."
                }
            }
            QueryType.YESTERDAY_DAY -> {
                val yesterday = now.minusDays(1)
                val (yHindiDay, yHinglishDay) = hindiDays[yesterday.dayOfWeek.name] ?: ("गुरुवार" to "Thursday")
                val yEngDay = yesterday.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)

                return when (lang) {
                    Language.DEVANAGARI -> "कल $yHindiDay था।"
                    Language.ENGLISH -> "Yesterday was $yEngDay."
                    Language.HINGLISH -> "Kal $yEngDay ($yHinglishDay) tha."
                }
            }
        }
    }
}
