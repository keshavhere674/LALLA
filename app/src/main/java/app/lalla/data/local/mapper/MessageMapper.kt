package app.lalla.data.local.mapper

import app.lalla.data.local.db.entity.MessageEntity
import app.lalla.domain.model.Message
import app.lalla.domain.model.MessageRole

fun MessageEntity.toDomain(): Message {
    val roleEnum = try {
        MessageRole.valueOf(role)
    } catch (_: Exception) {
        MessageRole.USER
    }
    return Message(
        id = id,
        conversationId = conversationId,
        role = roleEnum,
        content = content,
        timestamp = timestamp
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        content = content,
        timestamp = timestamp
    )
}
