package app.lalla.data.tasks.executors

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import app.lalla.service.TaskExecutorReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TaskExecutionResult(
    val isSuccess: Boolean,
    val method: String,
    val spokenFeedback: String,
    val failureReason: String? = null,
    val fallbackAttempted: Boolean = false
)

object AlarmTaskExecutor {

    fun execute(context: Context, timeMs: Long?, label: String, taskId: Long): TaskExecutionResult {
        if (timeMs == null || timeMs <= 0L) {
            return TaskExecutionResult(
                isSuccess = false,
                method = "Validation",
                spokenFeedback = "Kripya alarm ka samay batayein, jaise subah 6 baje ya shaam 7:30 baje.",
                failureReason = "Missing or invalid time parameter"
            )
        }

        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timeMs))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            return fallbackClockApp(context, hour, minute, label, formattedTime, "AlarmManager service unavailable")
        }

        // Check if exact alarms can be scheduled (Android 12+)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            try {
                val intent = Intent(context, TaskExecutorReceiver::class.java).apply {
                    action = TaskExecutorReceiver.ACTION_ALARM
                    putExtra(TaskExecutorReceiver.EXTRA_TASK_ID, taskId)
                    putExtra(TaskExecutorReceiver.EXTRA_LABEL, label)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)

                return TaskExecutionResult(
                    isSuccess = true,
                    method = "AlarmManager.setExactAndAllowWhileIdle",
                    spokenFeedback = "Alarm $formattedTime ke liye set kar diya gaya hai."
                )
            } catch (e: Exception) {
                return fallbackClockApp(context, hour, minute, label, formattedTime, "AlarmManager exception: ${e.message}")
            }
        } else {
            // Fallback: Use system Clock app intent
            return fallbackClockApp(context, hour, minute, label, formattedTime, "SCHEDULE_EXACT_ALARM permission not granted")
        }
    }

    private fun fallbackClockApp(
        context: Context,
        hour: Int,
        minute: Int,
        label: String,
        formattedTime: String,
        primaryFailureReason: String
    ): TaskExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            TaskExecutionResult(
                isSuccess = true,
                method = "AlarmClock.ACTION_SET_ALARM (Fallback)",
                spokenFeedback = "Clock app ke madhyam se alarm $formattedTime ke liye set kar diya gaya hai.",
                fallbackAttempted = true,
                failureReason = "Primary failed ($primaryFailureReason), fallback succeeded"
            )
        } catch (e: Exception) {
            TaskExecutionResult(
                isSuccess = false,
                method = "ClockAppFallback",
                spokenFeedback = "Alarm set karne mein samasya aayi. Kripya manual clock app check karein.",
                failureReason = "Primary ($primaryFailureReason) and Fallback failed: ${e.message}",
                fallbackAttempted = true
            )
        }
    }
}
