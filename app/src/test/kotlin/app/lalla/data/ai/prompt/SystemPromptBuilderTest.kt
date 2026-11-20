package app.lalla.data.ai.prompt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemPromptBuilderTest {

    @Test
    fun `build includes core persona and rules`() {
        val prompt = SystemPromptBuilder.build()

        assertTrue(prompt.contains("LALLA"))
        assertTrue(prompt.contains("Hindi"))
        assertTrue(prompt.contains("English"))
        assertTrue(prompt.contains("Hinglish"))
        assertTrue(prompt.contains("[MEMORY]"))
    }

    @Test
    fun `build includes empty memory placeholder when no memories provided`() {
        val prompt = SystemPromptBuilder.build(emptyList())

        assertTrue(prompt.contains("No personal memories stored yet"))
    }

    @Test
    fun `build injects provided memories accurately`() {
        val memories = listOf(
            "User's name is Rahul",
            "User prefers black coffee"
        )
        val prompt = SystemPromptBuilder.build(memories)

        assertFalse(prompt.contains("No personal memories stored yet"))
        assertTrue(prompt.contains("- User's name is Rahul"))
        assertTrue(prompt.contains("- User prefers black coffee"))
    }
}
