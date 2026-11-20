package app.lalla.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.lalla.data.local.db.dao.ConversationDao
import app.lalla.data.local.db.dao.MemoryDao
import app.lalla.data.local.db.dao.MessageDao
import app.lalla.data.local.db.entity.ConversationEntity
import app.lalla.data.local.db.entity.MemoryEntity
import app.lalla.data.local.db.entity.MessageEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LallaDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
}
