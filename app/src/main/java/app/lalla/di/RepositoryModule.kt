package app.lalla.di

import app.lalla.data.repository.AiRepositoryImpl
import app.lalla.data.repository.ConversationRepositoryImpl
import app.lalla.data.repository.MemoryRepositoryImpl
import app.lalla.data.repository.TaskRepositoryImpl
import app.lalla.domain.repository.AiRepository
import app.lalla.domain.repository.ConversationRepository
import app.lalla.domain.repository.MemoryRepository
import app.lalla.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds @Singleton
    abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository

    @Binds @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
