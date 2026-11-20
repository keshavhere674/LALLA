package app.lalla.domain.model

data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val isPinned: Boolean = false
)
