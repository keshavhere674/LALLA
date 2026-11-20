package app.lalla.domain.usecase.memory

import app.lalla.domain.model.Memory
import app.lalla.domain.model.MemorySource
import app.lalla.domain.repository.MemoryRepository
import javax.inject.Inject

class SaveMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(content: String, category: String? = null): Long {
        val memory = Memory(
            content = content.trim(),
            category = category?.trim(),
            source = MemorySource.USER
        )
        return memoryRepository.saveMemory(memory)
    }
}
