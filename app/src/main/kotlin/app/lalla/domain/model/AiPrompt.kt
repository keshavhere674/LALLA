package app.lalla.domain.model

data class AiPrompt(
    val systemInstruction: String,
    val injectedMemories: List<Memory> = emptyList(),
    val history: List<Message> = emptyList(),
    val userMessage: String
)
