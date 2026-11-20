package app.lalla.data.repository

import app.lalla.data.local.db.dao.MemoryDao
import app.lalla.data.local.mapper.toDomain
import app.lalla.data.local.mapper.toEntity
import app.lalla.domain.model.Memory
import app.lalla.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun getAllMemories(): Flow<List<Memory>> {
        return memoryDao.getAllMemories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMemory(memory: Memory): Long {
        return memoryDao.insertMemory(memory.toEntity())
    }

    override suspend fun searchMemories(query: String): List<Memory> {
        return memoryDao.searchMemories(query).map { it.toDomain() }
    }

    override suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemory(id)
    }

    override suspend fun getAllMemoriesSnapshot(): List<Memory> {
        return memoryDao.getAllSnapshot().map { it.toDomain() }
    }
}
