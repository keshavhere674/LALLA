package app.lalla.domain.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class Message(
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
