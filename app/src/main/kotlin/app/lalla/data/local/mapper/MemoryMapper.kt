package app.lalla.data.local.mapper

import app.lalla.data.local.db.entity.MemoryEntity
import app.lalla.domain.model.Memory
import app.lalla.domain.model.MemorySource

fun MemoryEntity.toDomain(): Memory {
    val src = try {
        MemorySource.valueOf(source)
    } catch (_: Exception) {
        MemorySource.CONVERSATION
    }
    return Memory(
        id = id,
        content = content,
        category = category,
        source = src,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt
    )
}

fun Memory.toEntity(): MemoryEntity {
    return MemoryEntity(
        id = id,
        content = content,
        category = category,
        source = source.name,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt
    )
}
