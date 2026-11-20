package app.lalla.domain.usecase.memory

import app.lalla.domain.model.Memory
import app.lalla.domain.repository.MemoryRepository
import javax.inject.Inject

class SearchMemoriesUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(query: String): List<Memory> {
        return if (query.isBlank()) {
            memoryRepository.getAllMemoriesSnapshot()
        } else {
            memoryRepository.searchMemories(query.trim())
        }
    }
}
