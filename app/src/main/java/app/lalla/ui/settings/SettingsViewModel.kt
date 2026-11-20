package app.lalla.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lalla.data.ai.RoutingAiEngine
import app.lalla.data.ai.ollama.OllamaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsConnectionStatus { UNKNOWN, CHECKING, CONNECTED, DISCONNECTED }

data class SettingsUiState(
    val provider: String = RoutingAiEngine.PROVIDER_OLLAMA,
    val freeLlmUrl: String = "http://localhost:3000/v1",
    val freeLlmModel: String = "gemini-1.5-flash",
    val freeLlmApiKey: String = "",
    val ollamaUrl: String = "http://localhost:11434",
    val ollamaModel: String = "qwen2.5:3b",
    val temperature: Float = 0.7f,
    val isSaved: Boolean = false,
    val connectionStatus: SettingsConnectionStatus = SettingsConnectionStatus.UNKNOWN
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        provider = prefs[RoutingAiEngine.KEY_PROVIDER] ?: RoutingAiEngine.PROVIDER_OLLAMA,
                        freeLlmUrl = prefs[RoutingAiEngine.KEY_FREELLM_URL] ?: "http://localhost:3000/v1",
                        freeLlmModel = prefs[RoutingAiEngine.KEY_FREELLM_MODEL] ?: "gemini-1.5-flash",
                        freeLlmApiKey = prefs[RoutingAiEngine.KEY_FREELLM_API_KEY] ?: "",
                        ollamaUrl = prefs[RoutingAiEngine.KEY_OLLAMA_URL] ?: "http://localhost:11434",
                        ollamaModel = prefs[RoutingAiEngine.KEY_OLLAMA_MODEL] ?: "qwen2.5:3b",
                        temperature = prefs[RoutingAiEngine.KEY_TEMPERATURE] ?: 0.7f
                    )
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = SettingsConnectionStatus.CHECKING) }
            val engine = OllamaEngine(
                baseUrl = _uiState.value.ollamaUrl,
                modelName = _uiState.value.ollamaModel
            )
            val reachable = engine.testConnection()
            _uiState.update {
                it.copy(
                    connectionStatus = if (reachable)
                        SettingsConnectionStatus.CONNECTED
                    else
                        SettingsConnectionStatus.DISCONNECTED
                )
            }
        }
    }

    fun selectProvider(provider: String) {
        _uiState.update { it.copy(provider = provider, isSaved = false) }
    }

    fun updateFreeLlmUrl(url: String) {
        _uiState.update { it.copy(freeLlmUrl = url, isSaved = false) }
    }

    fun updateFreeLlmModel(model: String) {
        _uiState.update { it.copy(freeLlmModel = model, isSaved = false) }
    }

    fun updateFreeLlmApiKey(key: String) {
        _uiState.update { it.copy(freeLlmApiKey = key, isSaved = false) }
    }

    fun updateOllamaUrl(url: String) {
        _uiState.update { it.copy(ollamaUrl = url, isSaved = false) }
    }

    fun updateOllamaModel(model: String) {
        _uiState.update { it.copy(ollamaModel = model, isSaved = false) }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp, isSaved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[RoutingAiEngine.KEY_PROVIDER] = _uiState.value.provider
                prefs[RoutingAiEngine.KEY_FREELLM_URL] = _uiState.value.freeLlmUrl.trim()
                prefs[RoutingAiEngine.KEY_FREELLM_MODEL] = _uiState.value.freeLlmModel.trim()
                prefs[RoutingAiEngine.KEY_FREELLM_API_KEY] = _uiState.value.freeLlmApiKey.trim()
                prefs[RoutingAiEngine.KEY_OLLAMA_URL] = _uiState.value.ollamaUrl.trim()
                prefs[RoutingAiEngine.KEY_OLLAMA_MODEL] = _uiState.value.ollamaModel.trim()
                prefs[RoutingAiEngine.KEY_TEMPERATURE] = _uiState.value.temperature
            }
            _uiState.update { it.copy(isSaved = true) }
            delay(2000)
            _uiState.update { it.copy(isSaved = false) }
        }
    }
}
