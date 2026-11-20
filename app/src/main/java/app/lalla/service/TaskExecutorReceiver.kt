package app.lalla.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.lalla.MainActivity
import app.lalla.R

/**
 * BroadcastReceiver that fires when an AlarmManager alarm or reminder triggers.
 * Shows a heads-up notification and can open WhatsApp.
 */
class TaskExecutorReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM    = "app.lalla.ACTION_ALARM"
        const val ACTION_REMINDER = "app.lalla.ACTION_REMINDER"
        const val ACTION_WHATSAPP = "app.lalla.ACTION_WHATSAPP"
        const val EXTRA_TASK_ID   = "task_id"
        const val EXTRA_LABEL     = "label"
        const val EXTRA_CONTACT   = "contact"
        const val EXTRA_MESSAGE   = "message"

        const val CHANNEL_TASKS = "lalla_tasks"
    }

    override fun onReceive(context: Context, intent: Intent) {
        ensureChannel(context)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val label  = intent.getStringExtra(EXTRA_LABEL) ?: "LALLA Reminder"

        when (intent.action) {
            ACTION_ALARM, ACTION_REMINDER -> {
                showNotification(context, taskId.toInt(), "⏰ $label", "Your LALLA reminder is ready!")
            }
            ACTION_WHATSAPP -> {
                val contact = intent.getStringExtra(EXTRA_CONTACT)
                val message = intent.getStringExtra(EXTRA_MESSAGE)
                // Open WhatsApp with pre-filled message
                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                    val phone = contact?.filter { it.isDigit() }
                    val encodedMsg = android.net.Uri.encode(message ?: "")
                    data = if (!phone.isNullOrBlank())
                        android.net.Uri.parse("https://wa.me/91$phone?text=$encodedMsg")
                    else
                        android.net.Uri.parse("https://wa.me/?text=$encodedMsg")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(waIntent) } catch (_: Exception) {}
                showNotification(context, taskId.toInt(), "💬 WhatsApp message", "Opening WhatsApp for: $contact")
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tapIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notif)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_TASKS) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }
}
