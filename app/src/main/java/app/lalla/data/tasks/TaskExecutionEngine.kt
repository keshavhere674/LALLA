package app.lalla.data.tasks

import android.content.Context
import app.lalla.data.actions.ActionParser
import app.lalla.data.actions.ActionType
import app.lalla.data.actions.ParsedAction
import app.lalla.data.local.db.entity.TaskEntity
import app.lalla.data.tasks.executors.*
import app.lalla.domain.model.LallaTask
import app.lalla.domain.model.TaskType
import app.lalla.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExecutionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository
) {

    /**
     * Executes a parsed action deterministically, logs to database, and returns the result.
     */
    suspend fun executeAction(
        action: ParsedAction,
        conversationId: Long = 0L
    ): TaskExecutionResult {
        // 1. Create task entry in database
        val taskType = when (action.type) {
            ActionType.ALARM -> TaskType.ALARM
            ActionType.WHATSAPP -> TaskType.WHATSAPP
            ActionType.GMAIL -> TaskType.GMAIL
            ActionType.FILE_SEARCH -> TaskType.FILE_SEARCH
            ActionType.CALL_HISTORY -> TaskType.CALL_HISTORY
            ActionType.REMINDER -> TaskType.REMINDER
            else -> TaskType.GENERIC
        }

        val taskId = taskRepository.saveTask(
            LallaTask(
                description = action.description,
                type = taskType,
                targetContact = action.targetContact,
                targetMessage = action.targetMessage,
                triggerTimeMs = action.triggerTimeMs,
                conversationId = conversationId
            )
        )

        // 2. Execute corresponding deterministic executor
        val result = when (action.type) {
            ActionType.ALARM, ActionType.REMINDER -> {
                AlarmTaskExecutor.execute(context, action.triggerTimeMs, action.description, taskId)
            }
            ActionType.WHATSAPP -> {
                WhatsAppTaskExecutor.execute(context, action.targetContact, action.targetMessage)
            }
            ActionType.GMAIL -> {
                if (action.isEmailRead) {
                    GmailTaskExecutor.executeRead(context, action.targetMessage)
                } else {
                    GmailTaskExecutor.executeSend(context, action.targetContact, action.emailSubject, action.targetMessage)
                }
            }
            ActionType.FILE_SEARCH -> {
                FileSearchTaskExecutor.execute(context, action.searchQuery)
            }
            ActionType.CALL_HISTORY -> {
                CallHistoryTaskExecutor.execute(context, action.targetContact)
            }
            ActionType.NONE, ActionType.GENERIC -> {
                TaskExecutionResult(
                    isSuccess = true,
                    method = "Generic",
                    spokenFeedback = "Task save kar liya gaya hai."
                )
            }
        }

        // 3. Update database record with execution status
        val status = when {
            result.isSuccess && result.fallbackAttempted -> "FALLBACK_SUCCESS"
            result.isSuccess -> "SUCCESS"
            else -> "FAILED"
        }

        taskRepository.updateExecutionResult(
            id = taskId,
            isCompleted = result.isSuccess,
            method = result.method,
            status = status,
            reason = result.failureReason,
            timeMs = System.currentTimeMillis()
        )

        return result
    }
}
