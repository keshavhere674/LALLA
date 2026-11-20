package app.lalla.data.local.mapper

import app.lalla.data.local.db.entity.ConversationEntity
import app.lalla.data.local.db.entity.MemoryEntity
import app.lalla.data.local.db.entity.MessageEntity
import app.lalla.domain.model.Conversation
import app.lalla.domain.model.Memory
import app.lalla.domain.model.MemorySource
import app.lalla.domain.model.Message
import app.lalla.domain.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    @Test
    fun `MessageEntity and Message map bidirectionally`() {
        val entity = MessageEntity(
            id = 5L,
            conversationId = 1L,
            role = "ASSISTANT",
            content = "Namaste",
            timestamp = 12345L
        )

        val domain = entity.toDomain()
        assertEquals(5L, domain.id)
        assertEquals(MessageRole.ASSISTANT, domain.role)
        assertEquals("Namaste", domain.content)

        val backToEntity = domain.toEntity()
        assertEquals(entity, backToEntity)
    }

    @Test
    fun `ConversationEntity and Conversation map bidirectionally`() {
        val entity = ConversationEntity(
            id = 10L,
            title = "Personal Chat",
            createdAt = 5000L,
            summary = "Summary test",
            isPinned = true
        )

        val domain = entity.toDomain()
        assertEquals(10L, domain.id)
        assertEquals("Personal Chat", domain.title)
        assertEquals(true, domain.isPinned)

        val backToEntity = domain.toEntity()
        assertEquals(entity, backToEntity)
    }

    @Test
    fun `MemoryEntity and Memory map bidirectionally`() {
        val entity = MemoryEntity(
            id = 3L,
            content = "Loves coffee",
            category = "preference",
            source = "CONVERSATION",
            createdAt = 1000L,
            lastAccessedAt = 2000L
        )

        val domain = entity.toDomain()
        assertEquals(3L, domain.id)
        assertEquals("Loves coffee", domain.content)
        assertEquals(MemorySource.CONVERSATION, domain.source)

        val backToEntity = domain.toEntity()
        assertEquals(entity, backToEntity)
    }
}
