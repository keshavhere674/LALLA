package app.lalla.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Auto-starts LALLA's wake-word listener after device reboot.
 * Requires RECEIVE_BOOT_COMPLETED permission.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val svc = Intent(context, LallaWakeWordService::class.java).apply {
                action = LallaWakeWordService.ACTION_START
            }
            context.startForegroundService(svc)
        }
    }
}
