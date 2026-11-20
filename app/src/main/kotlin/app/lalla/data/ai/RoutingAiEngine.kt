package app.lalla.data.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lalla.data.ai.mock.MockAiEngine
import app.lalla.data.ai.ollama.OllamaEngine
import app.lalla.data.ai.openai.OpenAiCompatibleEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingAiEngine @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AiEngine {

    companion object {
        val KEY_PROVIDER = stringPreferencesKey("key_provider") // "freellmapi", "ollama", "mock"
        val KEY_FREELLM_URL = stringPreferencesKey("key_freellm_url")
        val KEY_FREELLM_MODEL = stringPreferencesKey("key_freellm_model")
        val KEY_FREELLM_API_KEY = stringPreferencesKey("key_freellm_api_key")
        val KEY_OLLAMA_URL = stringPreferencesKey("key_ollama_url")
        val KEY_OLLAMA_MODEL = stringPreferencesKey("key_ollama_model")
        val KEY_TEMPERATURE = floatPreferencesKey("key_temperature")

        const val PROVIDER_FREELLM = "freellmapi"
        const val PROVIDER_OLLAMA = "ollama"
        const val PROVIDER_MOCK = "mock"
    }

    private suspend fun resolveEngine(): AiEngine {
        val prefs = dataStore.data.first()
        val provider = prefs[KEY_PROVIDER] ?: PROVIDER_FREELLM
        val temperature = (prefs[KEY_TEMPERATURE] ?: 0.7f).toDouble()

        return when (provider) {
            PROVIDER_FREELLM -> {
                val url = prefs[KEY_FREELLM_URL] ?: "http://localhost:3000/v1"
                val model = prefs[KEY_FREELLM_MODEL] ?: "gemini-1.5-flash"
                val apiKey = prefs[KEY_FREELLM_API_KEY] ?: ""
                OpenAiCompatibleEngine(
                    baseUrl = url,
                    modelName = model,
                    apiKey = apiKey,
                    temperature = temperature
                )
            }
            PROVIDER_OLLAMA -> {
                val url = prefs[KEY_OLLAMA_URL] ?: "http://localhost:11434"
                val model = prefs[KEY_OLLAMA_MODEL] ?: "qwen2.5:3b"
                OllamaEngine(
                    baseUrl = url,
                    modelName = model,
                    temperature = temperature
                )
            }
            PROVIDER_MOCK -> {
                MockAiEngine()
            }
            else -> {
                OpenAiCompatibleEngine(temperature = temperature)
            }
        }
    }

    override fun streamChat(prompt: AiPrompt): Flow<String> = flow {
        val activeEngine = resolveEngine()
        activeEngine.streamChat(prompt).collect { token ->
            emit(token)
        }
    }

    override suspend fun chat(prompt: AiPrompt): AiResponse {
        val activeEngine = resolveEngine()
        return activeEngine.chat(prompt)
    }
}
