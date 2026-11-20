package app.lalla.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import app.lalla.data.settings.AppSettings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Auto-starts LALLA's wake-word listener after device reboot ONLY if master switch was ON.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun appSettings(): AppSettings
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val ep = EntryPointAccessors.fromApplication(context.applicationContext, BootEntryPoint::class.java)
            val settings = ep.appSettings()

            // Strict rule: Respect Master Switch
            if (!settings.isBackgroundListeningEnabledSync()) return

            // Strict rule: Verify permissions
            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) return

            try {
                val svc = Intent(context, LallaWakeWordService::class.java).apply {
                    action = LallaWakeWordService.ACTION_START
                }
                context.startForegroundService(svc)
            } catch (_: Exception) {}
        }
    }
}
