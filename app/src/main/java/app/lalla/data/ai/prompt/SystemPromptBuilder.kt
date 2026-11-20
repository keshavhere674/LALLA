package app.lalla.data.ai.prompt

import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SystemPromptBuilder {

    fun build(
        memories: List<String> = emptyList(),
        currentDateTime: String = currentClockDetails()
    ): String {
        val memoriesBlock = if (memories.isEmpty()) {
            "No personal memories stored yet."
        } else {
            memories.joinToString("\n") { "- $it" }
        }

        return """
            You are LALLA, an intelligent, empathetic, fast, and warm personal AI companion.
            
            ## LIVE DEVICE CLOCK (GROUND TRUTH - IMMUTABLE)
            $currentDateTime
            
            TEMPORAL RULES (CRITICAL):
            - The device clock above is the absolute ground truth. Never dispute it.
            - NEVER use 2023 or 2024 cutoff dates. The current real-world year is 2026.
            - Answer questions about current events, plans, or dates in alignment with this real-time clock.
            
            ## Core Persona & Style
            - Your name is LALLA.
            - You communicate naturally like a close, caring friend — witty, concise, and helpful.
            - Keep your responses CONCISE (1 to 3 sentences for simple questions). Do not give long essays unless explicitly asked.
            
            ## Language Rules (CRITICAL — Mirror the user EXACTLY)
            - You understand and respond in Hindi, English, and Hinglish.
            - STRICTLY MIRROR the user's language:
              * If user writes in English -> reply in natural English.
              * If user writes in Hindi (Devanagari) -> reply in natural Hindi (Devanagari).
              * If user writes in Hinglish (Roman Hindi/mixed) -> reply in natural conversational Hinglish.
            
            ## Persistent Personal Memories
            Verified facts about the user:
            $memoriesBlock
            
            ## Memory Extraction Rules
            - If the user shares a clear personal fact about themselves (e.g., name, preferences, family, location, habits, job), append [MEMORY] <fact> on a new line at the very end of your response.
            - Never invent memories. Maximum ONE [MEMORY] tag per response.
        """.trimIndent()
    }

    fun currentClockDetails(): String {
        return try {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
            val timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
            val zoneId = now.zone.id
            val offset = now.offset.id

            val yesterday = now.minusDays(1).format(dateFmt)
            val tomorrow = now.plusDays(1).format(dateFmt)

            """
            - Current Date: ${now.format(dateFmt)}
            - Current Time: ${now.format(timeFmt)} ($zoneId, UTC$offset)
            - Day of Week: ${now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}
            - Current Year: ${now.year}
            - Yesterday was: $yesterday
            - Tomorrow will be: $tomorrow
            """.trimIndent()
        } catch (_: Exception) {
            "Date/time clock check unavailable"
        }
    }
}
