package app.lalla.data.ai

import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import kotlinx.coroutines.flow.Flow

interface AiEngine {
    fun streamChat(prompt: AiPrompt): Flow<String>
    suspend fun chat(prompt: AiPrompt): AiResponse
}
