package app.lalla.domain.repository

import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun streamChat(prompt: AiPrompt): Flow<String>
    suspend fun chat(prompt: AiPrompt): AiResponse
}
