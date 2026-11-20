package app.lalla.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lalla.domain.model.LallaTask
import app.lalla.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val pendingTasks: StateFlow<List<LallaTask>> = taskRepository
        .getPendingTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<LallaTask>> = taskRepository
        .getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markComplete(id: Long) {
        viewModelScope.launch { taskRepository.markComplete(id) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { taskRepository.deleteTask(id) }
    }
}
