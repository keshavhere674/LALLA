package app.lalla.data.local.mapper

import app.lalla.data.local.db.entity.ConversationEntity
import app.lalla.domain.model.Conversation

fun ConversationEntity.toDomain(): Conversation {
    return Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        summary = summary,
        isPinned = isPinned
    )
}

fun Conversation.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        summary = summary,
        isPinned = isPinned
    )
}
