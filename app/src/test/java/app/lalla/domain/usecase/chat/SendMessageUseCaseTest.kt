package app.lalla.domain.usecase.chat

import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.Memory
import app.lalla.domain.model.Message
import app.lalla.domain.model.MessageRole
import app.lalla.domain.repository.AiRepository
import app.lalla.domain.repository.ConversationRepository
import app.lalla.domain.repository.MemoryRepository
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class SendMessageUseCaseTest {

    private val conversationRepository: ConversationRepository = mockk(relaxed = true)
    private val memoryRepository: MemoryRepository = mockk(relaxed = true)
    private val aiRepository: AiRepository = mockk(relaxed = true)

    private lateinit var useCase: SendMessageUseCase

    @Before
    fun setup() {
        useCase = SendMessageUseCase(
            conversationRepository = conversationRepository,
            memoryRepository = memoryRepository,
            aiRepository = aiRepository
        )
    }

    @Test
    fun `invoke streams tokens, extracts memories, and saves assistant message`() = runBlocking {
        val convId = 1L
        val userPrompt = "Mera naam Keshav hai aur mujhe chai pasand hai"

        coEvery { conversationRepository.saveMessage(any()) } returns 101L
        coEvery { conversationRepository.getRecentMessages(convId, any()) } returns emptyList()
        coEvery { memoryRepository.getAllMemoriesSnapshot() } returns emptyList()

        val streamedChunks = listOf(
            "Namaste Keshav! ",
            "Main aapki baat yaad rakhungi.\n",
            "[MEMORY] User name is Keshav\n",
            "[MEMORY] User likes tea"
        )
        every { aiRepository.streamChat(any()) } returns flowOf(*streamedChunks.toTypedArray())

        val savedMemorySlot = mutableListOf<Memory>()
        coEvery { memoryRepository.saveMemory(capture(savedMemorySlot)) } returns 1L

        val savedMessageSlot = slot<Message>()
        coEvery { conversationRepository.saveMessage(capture(savedMessageSlot)) } returns 102L

        useCase(convId, userPrompt).test {
            // Check tokens
            assertEquals(SendMessageResult.Token("Namaste Keshav! "), awaitItem())
            assertEquals(SendMessageResult.Token("Main aapki baat yaad rakhungi.\n"), awaitItem())
            assertEquals(SendMessageResult.Token("[MEMORY] User name is Keshav\n"), awaitItem())
            assertEquals(SendMessageResult.Token("[MEMORY] User likes tea"), awaitItem())

            // Check completion
            val complete = awaitItem() as SendMessageResult.Complete
            assertTrue(complete.fullResponse.contains("Namaste Keshav!"))
            // [MEMORY] lines should be stripped from the clean response
            assertTrue(!complete.fullResponse.contains("[MEMORY]"))

            awaitComplete()
        }

        // Verify memories were saved to repository
        assertEquals(2, savedMemorySlot.size)
        assertEquals("User name is Keshav", savedMemorySlot[0].content)
        assertEquals("User likes tea", savedMemorySlot[1].content)

        // Verify clean message was saved as ASSISTANT
        coVerify {
            conversationRepository.saveMessage(match {
                it.role == MessageRole.ASSISTANT &&
                        it.content.contains("Namaste Keshav!") &&
                        !it.content.contains("[MEMORY]")
            })
        }
    }

    @Test
    fun `invoke resolves direct date query deterministically with live clock`() = runBlocking {
        val convId = 1L
        val datePrompt = "Aaj kya date hai?"
        val currentYear = ZonedDateTime.now().year.toString()

        coEvery { conversationRepository.saveMessage(any()) } returns 101L

        useCase(convId, datePrompt).test {
            val token = awaitItem() as SendMessageResult.Token
            assertTrue("Token must contain current year $currentYear", token.text.contains(currentYear))

            val complete = awaitItem() as SendMessageResult.Complete
            assertTrue("Complete must contain current year $currentYear", complete.fullResponse.contains(currentYear))

            awaitComplete()
        }

        // Verify aiRepository was NOT called for deterministic query
        coVerify(exactly = 0) { aiRepository.streamChat(any()) }

        // Verify saved assistant message contains current year
        coVerify {
            conversationRepository.saveMessage(match {
                it.role == MessageRole.ASSISTANT && it.content.contains(currentYear)
            })
        }
    }
}
