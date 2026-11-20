package app.lalla

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import app.lalla.data.settings.AppSettings
import app.lalla.service.LallaWakeWordService
import app.lalla.service.TaskExecutorReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LallaApplication : Application() {

    @Inject
    lateinit var appSettings: AppSettings

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startWakeWordServiceIfEnabled()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        LallaWakeWordService.CHANNEL_ID,
                        "LALLA Background Listening",
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

    private fun startWakeWordServiceIfEnabled() {
        if (appSettings.isBackgroundListeningEnabledSync()) {
            try {
                val intent = Intent(this, LallaWakeWordService::class.java).apply {
                    action = LallaWakeWordService.ACTION_START
                }
                startForegroundService(intent)
            } catch (_: Exception) {}
        }
    }
}
