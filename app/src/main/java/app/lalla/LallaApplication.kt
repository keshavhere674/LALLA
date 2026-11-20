package app.lalla

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import app.lalla.service.LallaWakeWordService
import app.lalla.service.TaskExecutorReceiver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LallaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startWakeWordService()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        LallaWakeWordService.CHANNEL_ID,
                        "LALLA Active",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Shows while LALLA is listening in background" },
                    NotificationChannel(
                        TaskExecutorReceiver.CHANNEL_TASKS,
                        "Task Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Alarms, reminders and task notifications from LALLA" }
                )
            )
        }
    }

    private fun startWakeWordService() {
        try {
            val intent = Intent(this, LallaWakeWordService::class.java).apply {
                action = LallaWakeWordService.ACTION_START
            }
            startForegroundService(intent)
        } catch (e: Exception) {
            // Will be started manually from MainActivity if needed
        }
    }
}
