package app.lalla.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.lalla.MainActivity
import app.lalla.R

/**
 * Foreground service that keeps LALLA's wake-word listener alive in the background.
 * Shows a persistent "LALLA is listening" notification.
 */
class LallaForegroundService : Service() {

    companion object {
        const val CHANNEL_FG = "lalla_foreground"
        const val NOTIF_ID = 1001
        const val ACTION_START = "app.lalla.START_LISTENING"
        const val ACTION_STOP  = "app.lalla.STOP_LISTENING"
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIF_ID, buildNotification())
        // The actual wake-word listening loop runs in LallaWakeWordService.
        // This service just keeps the process alive so the system doesn't kill it.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() = run {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LallaForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(this, CHANNEL_FG)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LALLA is listening 👂")
            .setContentText("Say \"LALLA\" to give me a command")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_FG) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_FG, "LALLA Active", NotificationManager.IMPORTANCE_LOW)
                )
            }
            if (nm.getNotificationChannel(TaskExecutorReceiver.CHANNEL_TASKS) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(TaskExecutorReceiver.CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }
}
