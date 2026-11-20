package app.lalla.data.local.db.dao

import androidx.room.*
import app.lalla.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :id")
    suspend fun markComplete(id: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, executionMethod = :method, resultStatus = :status, failureReason = :reason, executedAtMs = :timeMs WHERE id = :id")
    suspend fun updateExecutionResult(
        id: Long,
        isCompleted: Boolean,
        method: String?,
        status: String?,
        reason: String?,
        timeMs: Long?
    )

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM tasks WHERE triggerTimeMs IS NOT NULL AND isCompleted = 0 AND triggerTimeMs <= :now")
    suspend fun getOverdueTasks(now: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?
}
