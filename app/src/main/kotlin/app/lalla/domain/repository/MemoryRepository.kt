package app.lalla.domain.repository

import app.lalla.domain.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getAllMemories(): Flow<List<Memory>>
    suspend fun saveMemory(memory: Memory): Long
    suspend fun searchMemories(query: String): List<Memory>
    suspend fun deleteMemory(id: Long)
    suspend fun getAllMemoriesSnapshot(): List<Memory>
}
