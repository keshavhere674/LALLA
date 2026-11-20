package app.lalla.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val ollamaUrl: String = "http://localhost:11434",
    val modelName: String = "qwen2.5:3b",
    val temperature: Float = 0.7f,
    val isSaved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_OLLAMA_URL = stringPreferencesKey("key_ollama_url")
        val KEY_MODEL_NAME = stringPreferencesKey("key_model_name")
        val KEY_TEMPERATURE = floatPreferencesKey("key_temperature")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        ollamaUrl = prefs[KEY_OLLAMA_URL] ?: "http://localhost:11434",
                        modelName = prefs[KEY_MODEL_NAME] ?: "qwen2.5:3b",
                        temperature = prefs[KEY_TEMPERATURE] ?: 0.7f
                    )
                }
            }
        }
    }

    fun updateOllamaUrl(url: String) {
        _uiState.update { it.copy(ollamaUrl = url, isSaved = false) }
    }

    fun updateModelName(model: String) {
        _uiState.update { it.copy(modelName = model, isSaved = false) }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp, isSaved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_OLLAMA_URL] = _uiState.value.ollamaUrl.trim()
                prefs[KEY_MODEL_NAME] = _uiState.value.modelName.trim()
                prefs[KEY_TEMPERATURE] = _uiState.value.temperature
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
