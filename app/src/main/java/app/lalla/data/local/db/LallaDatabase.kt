package app.lalla.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.lalla.data.local.db.dao.ConversationDao
import app.lalla.data.local.db.dao.MemoryDao
import app.lalla.data.local.db.dao.MessageDao
import app.lalla.data.local.db.dao.TaskDao
import app.lalla.data.local.db.entity.ConversationEntity
import app.lalla.data.local.db.entity.MemoryEntity
import app.lalla.data.local.db.entity.MessageEntity
import app.lalla.data.local.db.entity.TaskEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        MemoryEntity::class,
        TaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LallaDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        type TEXT NOT NULL,
                        targetContact TEXT,
                        targetMessage TEXT,
                        triggerTimeMs INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        conversationId INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN executionMethod TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN resultStatus TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN failureReason TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN executedAtMs INTEGER")
            }
        }
    }
}
