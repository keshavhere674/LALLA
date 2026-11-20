package app.lalla.data.actions

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppSender {

    /**
     * Opens WhatsApp with a pre-filled message to a contact.
     * [phoneOrName]: phone number (with country code) OR a contact display name.
     * If only a name is given (no digits), opens WhatsApp chooser without a specific number.
     *
     * NOTE: True programmatic WhatsApp sending requires root or WhatsApp Business API.
     * This Intent-based approach opens WhatsApp for the user to confirm and send.
     */
    fun send(context: Context, phoneOrName: String?, message: String?): Boolean {
        return try {
            val encodedMsg = Uri.encode(message ?: "")
            val phone = phoneOrName?.filter { it.isDigit() }

            val intent = if (!phone.isNullOrBlank()) {
                // Direct chat via wa.me deep link (requires country code)
                val fullPhone = if (phone.startsWith("91") && phone.length >= 12) phone
                               else "91$phone"   // assume India +91
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$fullPhone?text=$encodedMsg"))
            } else {
                // Generic share to WhatsApp
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, message ?: "")
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) { false }
    }
}
