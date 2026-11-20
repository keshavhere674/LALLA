package app.lalla.domain.usecase.chat

import app.lalla.domain.model.Message
import app.lalla.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(conversationId: Long): Flow<List<Message>> {
        return conversationRepository.getMessages(conversationId)
    }

    suspend fun getOrCreateDefaultConversation(): Long {
        val existing = conversationRepository.getConversationById(1L)
        return if (existing != null) {
            existing.id
        } else {
            conversationRepository.createConversation("LALLA Companion")
        }
    }
}
