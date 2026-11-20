package app.lalla.domain.model

enum class TaskType {
    ALARM,
    WHATSAPP,
    GMAIL,
    FILE_SEARCH,
    CALL_HISTORY,
    REMINDER,
    GENERIC
}

data class LallaTask(
    val id: Long = 0,
    val description: String,
    val type: TaskType,
    val targetContact: String? = null,
    val targetMessage: String? = null,
    val triggerTimeMs: Long? = null,
    val isCompleted: Boolean = false,
    val executionMethod: String? = null,
    val resultStatus: String? = null,
    val failureReason: String? = null,
    val executedAtMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val conversationId: Long = 0
)
