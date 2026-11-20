package app.lalla.data.actions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import app.lalla.service.TaskExecutorReceiver
import java.util.Calendar

object AlarmScheduler {

    /**
     * Sets an exact alarm using AlarmManager.
     * On Android 12+ SCHEDULE_EXACT_ALARM permission must be granted; falls back to inexact.
     * Also opens the system clock app as a reliable fallback for the user.
     */
    fun setAlarm(context: Context, timeMs: Long, label: String, taskId: Long): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                // Inexact fallback
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the system clock app to set an alarm — most reliable across OEMs.
     * Call from an Activity context (or use FLAG_ACTIVITY_NEW_TASK).
     */
    fun openSystemAlarm(context: Context, hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) { /* clock app not available */ }
    }

    fun cancelAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskExecutorReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    fun hourMinuteFromMs(timeMs: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }
}
