package app.lalla.domain.model

enum class MemorySource {
    CONVERSATION,
    USER
}

data class Memory(
    val id: Long = 0,
    val content: String,
    val category: String? = null,
    val source: MemorySource = MemorySource.CONVERSATION,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)
