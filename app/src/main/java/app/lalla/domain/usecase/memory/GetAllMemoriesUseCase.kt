package app.lalla.domain.usecase.memory

import app.lalla.domain.model.Memory
import app.lalla.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllMemoriesUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    operator fun invoke(): Flow<List<Memory>> {
        return memoryRepository.getAllMemories()
    }
}
