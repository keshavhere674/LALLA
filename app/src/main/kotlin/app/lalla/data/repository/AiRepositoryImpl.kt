package app.lalla.data.repository

import app.lalla.data.ai.AiEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import app.lalla.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiEngine: AiEngine
) : AiRepository {

    override fun streamChat(prompt: AiPrompt): Flow<String> {
        return aiEngine.streamChat(prompt)
    }

    override suspend fun chat(prompt: AiPrompt): AiResponse {
        val rawResponse = aiEngine.chat(prompt)
        val lines = rawResponse.text.lines()
        val cleanedLines = mutableListOf<String>()
        val memoriesToSave = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[MEMORY]", ignoreCase = true)) {
                val memoryFact = trimmed.substringAfter("[MEMORY]").trim()
                if (memoryFact.isNotEmpty()) {
                    memoriesToSave.add(memoryFact)
                }
            } else {
                cleanedLines.add(line)
            }
        }

        return AiResponse(
            text = cleanedLines.joinToString("\n").trim(),
            memoriesToSave = memoriesToSave
        )
    }
}
