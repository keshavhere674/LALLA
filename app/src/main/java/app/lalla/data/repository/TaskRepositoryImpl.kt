package app.lalla.data.repository

import app.lalla.data.local.db.dao.TaskDao
import app.lalla.data.local.db.entity.TaskEntity
import app.lalla.domain.model.LallaTask
import app.lalla.domain.model.TaskType
import app.lalla.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getPendingTasks(): Flow<List<LallaTask>> =
        taskDao.getPendingTasks().map { list -> list.map { it.toDomain() } }

    override fun getAllTasks(): Flow<List<LallaTask>> =
        taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun saveTask(task: LallaTask): Long =
        taskDao.insert(task.toEntity())

    override suspend fun markComplete(id: Long) = taskDao.markComplete(id)

    override suspend fun deleteTask(id: Long) = taskDao.delete(id)

    override suspend fun getOverdueTasks(): List<LallaTask> =
        taskDao.getOverdueTasks(System.currentTimeMillis()).map { it.toDomain() }
}

private fun TaskEntity.toDomain() = LallaTask(
    id = id,
    description = description,
    type = runCatching { TaskType.valueOf(type) }.getOrDefault(TaskType.GENERIC),
    targetContact = targetContact,
    targetMessage = targetMessage,
    triggerTimeMs = triggerTimeMs,
    isCompleted = isCompleted,
    createdAt = createdAt,
    conversationId = conversationId
)

private fun LallaTask.toEntity() = TaskEntity(
    id = id,
    description = description,
    type = type.name,
    targetContact = targetContact,
    targetMessage = targetMessage,
    triggerTimeMs = triggerTimeMs,
    isCompleted = isCompleted,
    createdAt = createdAt,
    conversationId = conversationId
)
