package app.lalla.domain.usecase.chat

import android.content.Context
import app.lalla.data.actions.ActionParser
import app.lalla.data.actions.ActionType
import app.lalla.data.ai.prompt.SystemPromptBuilder
import app.lalla.data.datetime.DateTimeHelper
import app.lalla.data.tasks.TaskExecutionEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.Memory
import app.lalla.domain.model.MemorySource
import app.lalla.domain.model.Message
import app.lalla.domain.model.MessageRole
import app.lalla.domain.repository.AiRepository
import app.lalla.domain.repository.ConversationRepository
import app.lalla.domain.repository.MemoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class SendMessageResult {
    data class Token(val text: String) : SendMessageResult()
    data class Complete(val fullResponse: String, val cleanedText: String) : SendMessageResult()
    data class Error(val message: String) : SendMessageResult()
}

class SendMessageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val aiRepository: AiRepository,
    private val taskExecutionEngine: TaskExecutionEngine
) {
    operator fun invoke(conversationId: Long, userText: String): Flow<SendMessageResult> = flow {
        val trimmedInput = userText.trim()
        if (trimmedInput.isBlank()) return@flow

        // 1. Save user message to database
        val userMessage = Message(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = trimmedInput
        )
        val savedUserMsgId = conversationRepository.saveMessage(userMessage)

        // 2. Deterministic short-circuit for date/time/day questions
        val deterministicResponse = DateTimeHelper.tryResolveDateOrTime(trimmedInput)
        if (deterministicResponse != null) {
            conversationRepository.saveMessage(
                Message(conversationId = conversationId, role = MessageRole.ASSISTANT, content = deterministicResponse)
            )
            emit(SendMessageResult.Token(deterministicResponse))
            emit(SendMessageResult.Complete(fullResponse = deterministicResponse, cleanedText = deterministicResponse))
            return@flow
        }

        // 3. Central Deterministic Task Engine (Alarms, WhatsApp, Gmail, Files, Call History)
        val parsedAction = ActionParser.parse(trimmedInput)
        if (parsedAction != null && parsedAction.type != ActionType.NONE) {
            val taskResult = taskExecutionEngine.executeAction(parsedAction, conversationId)
            val feedback = taskResult.spokenFeedback

            conversationRepository.saveMessage(
                Message(conversationId = conversationId, role = MessageRole.ASSISTANT, content = feedback)
            )
            emit(SendMessageResult.Token(feedback))
            emit(SendMessageResult.Complete(fullResponse = feedback, cleanedText = feedback))
            return@flow
        }

        // 4. Regular AI conversation
        val history = conversationRepository.getRecentMessages(conversationId, limit = 6)
            .filter { it.id != savedUserMsgId }
        val memories = memoryRepository.getAllMemoriesSnapshot()

        val clockDetails = SystemPromptBuilder.currentClockDetails()
        val systemPrompt = SystemPromptBuilder.build(
            memories = memories.map { it.content },
            currentDateTime = clockDetails
        )
        val prompt = AiPrompt(
            systemInstruction = systemPrompt,
            injectedMemories = memories,
            history = history,
            userMessage = trimmedInput
        )

        // 5. Stream AI response
        val fullResponseBuilder = StringBuilder()
        aiRepository.streamChat(prompt).collect { token ->
            fullResponseBuilder.append(token)
            emit(SendMessageResult.Token(token))
        }

        val rawFullResponse = fullResponseBuilder.toString()

        // 6. Parse [MEMORY] tags if present
        val lines = rawFullResponse.lines()
        val cleanedLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[MEMORY]", ignoreCase = true)) {
                val memoryFact = trimmed.substringAfter("[MEMORY]").trim()
                if (memoryFact.isNotEmpty()) {
                    memoryRepository.saveMemory(Memory(content = memoryFact, source = MemorySource.CONVERSATION))
                }
            } else {
                cleanedLines.add(line)
            }
        }

        val cleanResponse = cleanedLines.joinToString("\n").trim()

        // 7. Save assistant message
        if (cleanResponse.isNotEmpty()) {
            conversationRepository.saveMessage(
                Message(conversationId = conversationId, role = MessageRole.ASSISTANT, content = cleanResponse)
            )
        }

        emit(SendMessageResult.Complete(fullResponse = cleanResponse, cleanedText = cleanResponse))
    }.catch { e ->
        emit(SendMessageResult.Error(e.localizedMessage ?: "AI engine error"))
    }
}
