package app.lalla.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lalla.domain.model.Memory
import app.lalla.domain.usecase.memory.DeleteMemoryUseCase
import app.lalla.domain.usecase.memory.GetAllMemoriesUseCase
import app.lalla.domain.usecase.memory.SaveMemoryUseCase
import app.lalla.domain.usecase.memory.SearchMemoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryUiState(
    val memories: List<Memory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val getAllMemoriesUseCase: GetAllMemoriesUseCase,
    private val deleteMemoryUseCase: DeleteMemoryUseCase,
    private val searchMemoriesUseCase: SearchMemoriesUseCase,
    private val saveMemoryUseCase: SaveMemoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadAllMemories()
    }

    private fun loadAllMemories() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllMemoriesUseCase().collect { list ->
                _uiState.update {
                    it.copy(
                        memories = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            loadAllMemories()
        } else {
            observeJob?.cancel()
            viewModelScope.launch {
                val results = searchMemoriesUseCase(query)
                _uiState.update { it.copy(memories = results) }
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            deleteMemoryUseCase(id)
            if (_uiState.value.searchQuery.isNotBlank()) {
                val results = searchMemoriesUseCase(_uiState.value.searchQuery)
                _uiState.update { it.copy(memories = results) }
            }
        }
    }

    fun addManualMemory(content: String, category: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            saveMemoryUseCase(content, category)
        }
    }
}
