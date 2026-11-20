package app.lalla.domain.repository

import app.lalla.domain.model.LallaTask
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getPendingTasks(): Flow<List<LallaTask>>
    fun getAllTasks(): Flow<List<LallaTask>>
    suspend fun saveTask(task: LallaTask): Long
    suspend fun markComplete(id: Long)
    suspend fun deleteTask(id: Long)
    suspend fun getOverdueTasks(): List<LallaTask>
}
