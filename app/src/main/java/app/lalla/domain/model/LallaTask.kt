package app.lalla.domain.model

enum class TaskType { ALARM, WHATSAPP, REMINDER, GENERIC }

data class LallaTask(
    val id: Long = 0,
    val description: String,
    val type: TaskType,
    val targetContact: String? = null,
    val targetMessage: String? = null,
    val triggerTimeMs: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val conversationId: Long = 0
)
