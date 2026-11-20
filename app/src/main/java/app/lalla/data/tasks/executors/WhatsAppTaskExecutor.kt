package app.lalla.data.tasks.executors

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import java.net.URLEncoder

object WhatsAppTaskExecutor {

    fun execute(context: Context, contactNameOrNumber: String?, message: String?): TaskExecutionResult {
        if (contactNameOrNumber.isNullOrBlank()) {
            return TaskExecutionResult(
                isSuccess = false,
                method = "Validation",
                spokenFeedback = "Kripya batayein kisko WhatsApp message bhejna hai.",
                failureReason = "Missing contact name/number"
            )
        }

        if (message.isNullOrBlank()) {
            return TaskExecutionResult(
                isSuccess = false,
                method = "Validation",
                spokenFeedback = "Kripya batayein kya message bhejna hai.",
                failureReason = "Missing message content"
            )
        }

        val pm = context.packageManager
        val isStandardWa = runCatching { pm.getPackageInfo("com.whatsapp", 0) }.isSuccess
        val isBusinessWa = runCatching { pm.getPackageInfo("com.whatsapp.w4b", 0) }.isSuccess

        if (!isStandardWa && !isBusinessWa) {
            return TaskExecutionResult(
                isSuccess = false,
                method = "PackageCheck",
                spokenFeedback = "Is phone par WhatsApp install nahi hai.",
                failureReason = "WhatsApp neither found in standard nor business package"
            )
        }

        val targetPackage = if (isStandardWa) "com.whatsapp" else "com.whatsapp.w4b"
        val cleanInput = contactNameOrNumber.trim()

        // 1. Check if contact is already a phone number
        val rawDigits = cleanInput.filter { it.isDigit() }
        val resolvedPhone: String? = if (rawDigits.length >= 10) {
            formatPhoneNumber(rawDigits)
        } else {
            // Search contact in ContactsProvider
            lookupContactPhone(context, cleanInput)
        }

        val encodedMsg = runCatching { URLEncoder.encode(message, "UTF-8") }.getOrDefault(message)

        // Primary Execution: Direct wa.me chat URL
        if (!resolvedPhone.isNullOrBlank()) {
            try {
                val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$resolvedPhone&text=$encodedMsg")
                val intent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    setPackage(targetPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                    return TaskExecutionResult(
                        isSuccess = true,
                        method = "WhatsApp Direct Chat (wa.me)",
                        spokenFeedback = "$cleanInput ke liye WhatsApp chat khol diya gaya hai. Kripya send tap karein, kyunki Android bina pushti ke direct background sending ki anumati nahi deta."
                    )
                }
            } catch (e: Exception) {
                // Proceed to fallback
            }
        }

        // Fallback Execution: Generic ACTION_SEND to WhatsApp
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(targetPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (sendIntent.resolveActivity(pm) != null) {
                context.startActivity(sendIntent)
                TaskExecutionResult(
                    isSuccess = true,
                    method = "WhatsApp ACTION_SEND (Fallback)",
                    spokenFeedback = "WhatsApp khol diya gaya hai. Kripya $cleanInput ko select karke message send karein.",
                    fallbackAttempted = true,
                    failureReason = if (resolvedPhone == null) "Contact phone number not found in phonebook" else null
                )
            } else {
                TaskExecutionResult(
                    isSuccess = false,
                    method = "WhatsApp ACTION_SEND (Fallback)",
                    spokenFeedback = "WhatsApp open karne mein asafalta hui.",
                    failureReason = "No activity found to handle send intent",
                    fallbackAttempted = true
                )
            }
        } catch (e: Exception) {
            TaskExecutionResult(
                isSuccess = false,
                method = "WhatsApp",
                spokenFeedback = "WhatsApp kholne mein samasya aayi: ${e.localizedMessage}",
                failureReason = e.message,
                fallbackAttempted = true
            )
        }
    }

    private fun formatPhoneNumber(digits: String): String {
        return when {
            digits.startsWith("91") && digits.length >= 12 -> digits
            digits.length == 10 -> "91$digits" // Default India country code
            else -> digits
        }
    }

    private fun lookupContactPhone(context: Context, nameQuery: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (cursor.moveToFirst() && numberIdx >= 0) {
                    val raw = cursor.getString(numberIdx) ?: ""
                    formatPhoneNumber(raw.filter { it.isDigit() })
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
