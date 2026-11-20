package app.lalla.data.repository

import app.lalla.data.local.db.dao.ConversationDao
import app.lalla.data.local.db.dao.MessageDao
import app.lalla.data.local.db.entity.ConversationEntity
import app.lalla.data.local.mapper.toDomain
import app.lalla.data.local.mapper.toEntity
import app.lalla.domain.model.Conversation
import app.lalla.domain.model.Message
import app.lalla.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversationById(id: Long): Conversation? {
        return conversationDao.getById(id)?.toDomain()
    }

    override suspend fun createConversation(title: String): Long {
        return conversationDao.insertConversation(
            ConversationEntity(title = title)
        )
    }

    override suspend fun updateConversationSummary(id: Long, summary: String) {
        conversationDao.updateSummary(id, summary)
    }

    override fun getMessages(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMessage(message: Message): Long {
        return messageDao.insertMessage(message.toEntity())
    }

    override suspend fun getRecentMessages(conversationId: Long, limit: Int): List<Message> {
        // Dao returns DESC order (newest first); reverse so caller gets chronological order
        return messageDao.getRecentMessages(conversationId, limit).map { it.toDomain() }.reversed()
    }
}
