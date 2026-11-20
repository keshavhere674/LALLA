package app.lalla.di

import android.content.Context
import androidx.room.Room
import app.lalla.data.local.db.LallaDatabase
import app.lalla.data.local.db.dao.ConversationDao
import app.lalla.data.local.db.dao.MemoryDao
import app.lalla.data.local.db.dao.MessageDao
import app.lalla.data.local.db.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLallaDatabase(
        @ApplicationContext context: Context
    ): LallaDatabase {
        return Room.databaseBuilder(
            context,
            LallaDatabase::class.java,
            "lalla_database.db"
        )
            .addMigrations(LallaDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideMessageDao(database: LallaDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideConversationDao(database: LallaDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMemoryDao(database: LallaDatabase): MemoryDao = database.memoryDao()

    @Provides
    fun provideTaskDao(database: LallaDatabase): TaskDao = database.taskDao()
}
