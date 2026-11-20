package app.lalla.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lalla.data.speech.AndroidTextToSpeech
import app.lalla.domain.model.Message
import app.lalla.domain.usecase.chat.LoadConversationUseCase
import app.lalla.domain.usecase.chat.SendMessageResult
import app.lalla.domain.usecase.chat.SendMessageUseCase
import app.lalla.ui.speech.SpeechRecognizer
import app.lalla.ui.speech.SttResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING }
enum class ConnectionStatus { UNKNOWN, CONNECTED, DISCONNECTED }

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val streamingResponse: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentConversationId: Long = 0L,
    val voiceState: VoiceState = VoiceState.IDLE,
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val voiceInputText: String = "",
    val isTtsEnabled: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loadConversationUseCase: LoadConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val speechRecognizer: SpeechRecognizer,
    private val tts: AndroidTextToSpeech
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var voiceJob: Job? = null
    private var observeMessagesJob: Job? = null

    init {
        viewModelScope.launch {
            val convId = loadConversationUseCase.getOrCreateDefaultConversation()
            _uiState.update { it.copy(currentConversationId = convId) }
            observeConversation(convId)
        }
        // Observe TTS state
        viewModelScope.launch {
            tts.state.collect { ttsState ->
                val voiceState = when (ttsState) {
                    is app.lalla.data.speech.TtsState.Speaking -> VoiceState.SPEAKING
                    else -> if (_uiState.value.voiceState == VoiceState.SPEAKING) VoiceState.IDLE
                            else _uiState.value.voiceState
                }
                _uiState.update { it.copy(voiceState = voiceState) }
            }
        }
    }

    private fun observeConversation(conversationId: Long) {
        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            loadConversationUseCase(conversationId).collect { msgList ->
                _uiState.update { it.copy(messages = msgList) }
            }
        }
    }

    fun sendMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || _uiState.value.isLoading) return

        // Stop any ongoing TTS before sending
        tts.stop()

        streamingJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                streamingResponse = "",
                error = null,
                voiceState = VoiceState.THINKING,
                voiceInputText = ""
            )
        }

        streamingJob = viewModelScope.launch {
            val streamBuilder = StringBuilder()
            val convId = ensureConversation()

            sendMessageUseCase(convId, clean).collect { result ->
                when (result) {
                    is SendMessageResult.Token -> {
                        streamBuilder.append(result.text)
                        _uiState.update {
                            it.copy(streamingResponse = streamBuilder.toString())
                        }
                    }
                    is SendMessageResult.Complete -> {
                        val cleanedText = result.cleanedText
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamingResponse = "",
                                voiceState = if (it.isTtsEnabled && cleanedText.isNotBlank())
                                    VoiceState.SPEAKING else VoiceState.IDLE
                            )
                        }
                        // Auto-speak the response if TTS is enabled
                        if (_uiState.value.isTtsEnabled && cleanedText.isNotBlank()) {
                            tts.speak(cleanedText)
                        }
                    }
                    is SendMessageResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamingResponse = "",
                                error = result.message,
                                voiceState = VoiceState.IDLE
                            )
                        }
                    }
                }
            }
        }
    }

    fun startVoiceInput() {
        if (_uiState.value.isLoading) return
        // Check permission first
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            _uiState.update {
                it.copy(error = "Microphone permission required. Grant it in Settings.")
            }
            return
        }

        tts.stop()
        voiceJob?.cancel()
        _uiState.update {
            it.copy(
                voiceState = VoiceState.LISTENING,
                voiceInputText = "",
                error = null
            )
        }

        voiceJob = viewModelScope.launch {
            speechRecognizer.startListening().collect { result ->
                when (result) {
                    is SttResult.Partial -> {
                        _uiState.update { it.copy(voiceInputText = result.text) }
                    }
                    is SttResult.Final -> {
                        _uiState.update {
                            it.copy(
                                voiceState = VoiceState.IDLE,
                                voiceInputText = result.text
                            )
                        }
                        if (result.text.isNotBlank()) {
                            sendMessage(result.text)
                        }
                    }
                    is SttResult.Error -> {
                        _uiState.update {
                            it.copy(
                                voiceState = VoiceState.IDLE,
                                error = result.message,
                                voiceInputText = ""
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopVoiceInput() {
        speechRecognizer.stopListening()
        voiceJob?.cancel()
        _uiState.update {
            it.copy(voiceState = VoiceState.IDLE, voiceInputText = "")
        }
    }

    fun stopSpeaking() {
        tts.stop()
        _uiState.update { it.copy(voiceState = VoiceState.IDLE) }
    }

    fun toggleTts() {
        val newEnabled = !_uiState.value.isTtsEnabled
        if (!newEnabled) tts.stop()
        _uiState.update { it.copy(isTtsEnabled = newEnabled) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun ensureConversation(): Long {
        return if (_uiState.value.currentConversationId > 0L) {
            _uiState.value.currentConversationId
        } else {
            val newId = loadConversationUseCase.getOrCreateDefaultConversation()
            _uiState.update { it.copy(currentConversationId = newId) }
            newId
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
    }
}
