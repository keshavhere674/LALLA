package app.lalla.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.lalla.data.ai.AiEngine
import app.lalla.data.ai.ollama.OllamaEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lalla_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAiEngine(): AiEngine {
        // By default, uses local Ollama via ADB reverse (http://localhost:11434) with Qwen2.5-3B
        return OllamaEngine(
            baseUrl = "http://localhost:11434",
            modelName = "qwen2.5:3b",
            temperature = 0.7
        )
    }
}
