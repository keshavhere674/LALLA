package app.lalla.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lalla.domain.model.Message
import app.lalla.domain.usecase.chat.LoadConversationUseCase
import app.lalla.domain.usecase.chat.SendMessageResult
import app.lalla.domain.usecase.chat.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val streamingResponse: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentConversationId: Long = 1L
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val loadConversationUseCase: LoadConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var observeMessagesJob: Job? = null

    init {
        viewModelScope.launch {
            val convId = loadConversationUseCase.getOrCreateDefaultConversation()
            _uiState.update { it.copy(currentConversationId = convId) }
            observeConversation(convId)
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

        streamingJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                streamingResponse = "",
                error = null
            )
        }

        streamingJob = viewModelScope.launch {
            val streamBuilder = StringBuilder()
            val convId = _uiState.value.currentConversationId

            sendMessageUseCase(convId, clean).collect { result ->
                when (result) {
                    is SendMessageResult.Token -> {
                        streamBuilder.append(result.text)
                        _uiState.update {
                            it.copy(streamingResponse = streamBuilder.toString())
                        }
                    }
                    is SendMessageResult.Complete -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamingResponse = ""
                            )
                        }
                    }
                    is SendMessageResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamingResponse = "",
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
