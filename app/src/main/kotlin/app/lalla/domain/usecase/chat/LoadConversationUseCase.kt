package app.lalla.domain.usecase.chat

import app.lalla.domain.model.Message
import app.lalla.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LoadConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(conversationId: Long): Flow<List<Message>> {
        return conversationRepository.getMessages(conversationId)
    }

    suspend fun getOrCreateDefaultConversation(): Long {
        val existingList = conversationRepository.getAllConversations().first()
        val first = existingList.firstOrNull()
        return first?.id ?: conversationRepository.createConversation("LALLA Companion")
    }
}
