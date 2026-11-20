package app.lalla.data.tasks.executors

import android.content.Context
import android.content.Intent
import android.net.Uri

object GmailTaskExecutor {

    fun executeSend(
        context: Context,
        recipient: String?,
        subject: String?,
        body: String?
    ): TaskExecutionResult {
        val targetRecipient = recipient?.trim() ?: ""
        val targetSubject = subject?.trim() ?: "Message from LALLA"
        val targetBody = body?.trim() ?: ""

        val pm = context.packageManager
        val emailUri = if (targetRecipient.contains("@")) {
            Uri.parse("mailto:$targetRecipient")
        } else {
            Uri.parse("mailto:")
        }

        // Primary: Target Gmail app specifically
        val gmailIntent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
            setPackage("com.google.android.gm")
            putExtra(Intent.EXTRA_SUBJECT, targetSubject)
            putExtra(Intent.EXTRA_TEXT, targetBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (gmailIntent.resolveActivity(pm) != null) {
            return try {
                context.startActivity(gmailIntent)
                TaskExecutionResult(
                    isSuccess = true,
                    method = "Gmail Compose Intent",
                    spokenFeedback = "Gmail compose screen khol diya gaya hai. Kripya send button tap karein."
                )
            } catch (e: Exception) {
                fallbackGenericEmail(context, emailUri, targetSubject, targetBody, e.message ?: "Launch error")
            }
        }

        // Fallback: Generic mail client
        return fallbackGenericEmail(context, emailUri, targetSubject, targetBody, "Gmail package not available")
    }

    private fun fallbackGenericEmail(
        context: Context,
        uri: Uri,
        subject: String,
        body: String,
        primaryReason: String
    ): TaskExecutionResult {
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                TaskExecutionResult(
                    isSuccess = true,
                    method = "Generic Email Compose (Fallback)",
                    spokenFeedback = "Email compose screen khol diya gaya hai.",
                    fallbackAttempted = true,
                    failureReason = "Primary failed ($primaryReason)"
                )
            } else {
                TaskExecutionResult(
                    isSuccess = false,
                    method = "Email",
                    spokenFeedback = "Phone par koi email app uplabdh nahi hai.",
                    failureReason = "No email activity found",
                    fallbackAttempted = true
                )
            }
        } catch (e: Exception) {
            TaskExecutionResult(
                isSuccess = false,
                method = "Email",
                spokenFeedback = "Email app open karne mein truti aayi: ${e.localizedMessage}",
                failureReason = e.message,
                fallbackAttempted = true
            )
        }
    }

    fun executeRead(context: Context, query: String?): TaskExecutionResult {
        val pm = context.packageManager
        val isGmailInstalled = runCatching { pm.getPackageInfo("com.google.android.gm", 0) }.isSuccess

        if (isGmailInstalled) {
            val intent = pm.getLaunchIntentForPackage("com.google.android.gm")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                return TaskExecutionResult(
                    isSuccess = true,
                    method = "Gmail App Open",
                    spokenFeedback = "Gmail inbox khol diya gaya hai. Android suraksha niyam anusar bina Google account login ke background mein emails direct read nahi kiye ja sakte."
                )
            }
        }

        return TaskExecutionResult(
            isSuccess = false,
            method = "Gmail Read",
            spokenFeedback = "Gmail app is phone par nahi mila.",
            failureReason = "Gmail not installed"
        )
    }
}
