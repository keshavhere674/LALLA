package app.lalla.domain.repository

import app.lalla.domain.model.Conversation
import app.lalla.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: Long): Conversation?
    suspend fun createConversation(title: String): Long
    suspend fun updateConversationSummary(id: Long, summary: String)
    fun getMessages(conversationId: Long): Flow<List<Message>>
    suspend fun saveMessage(message: Message): Long
    suspend fun getRecentMessages(conversationId: Long, limit: Int = 20): List<Message>
}
