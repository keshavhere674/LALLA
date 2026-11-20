package app.lalla.domain.usecase.memory

import app.lalla.domain.repository.MemoryRepository
import javax.inject.Inject

class DeleteMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(id: Long) {
        memoryRepository.deleteMemory(id)
    }
}
