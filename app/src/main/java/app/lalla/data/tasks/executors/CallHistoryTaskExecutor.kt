package app.lalla.data.tasks.executors

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallHistoryTaskExecutor {

    fun execute(context: Context, contactQuery: String?): TaskExecutionResult {
        // 1. Check READ_CALL_LOG permission
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPerm) {
            // Fallback: Open phone call log screen
            return try {
                val intent = Intent(Intent.ACTION_VIEW, CallLog.Calls.CONTENT_URI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                TaskExecutionResult(
                    isSuccess = true,
                    method = "Open Call Log (Fallback)",
                    spokenFeedback = "Call log permission abhi allow nahi hai, isliye phone dialer khol diya gaya hai.",
                    fallbackAttempted = true,
                    failureReason = "READ_CALL_LOG permission not granted"
                )
            } catch (e: Exception) {
                TaskExecutionResult(
                    isSuccess = false,
                    method = "Call History",
                    spokenFeedback = "Call log dekhne ke liye permission chahiye.",
                    failureReason = "Permission missing: ${e.message}",
                    fallbackAttempted = true
                )
            }
        }

        // Primary: Query CallLog content provider directly
        return try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION
            )

            val (selection, selectionArgs) = if (!contactQuery.isNullOrBlank()) {
                val clean = contactQuery.trim()
                "${CallLog.Calls.CACHED_NAME} LIKE ? OR ${CallLog.Calls.NUMBER} LIKE ?" to arrayOf("%$clean%", "%$clean%")
            } else {
                null to null
            }

            val sortOrder = "${CallLog.Calls.DATE} DESC"

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                    val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)

                    val number = if (numIdx >= 0) cursor.getString(numIdx) ?: "Unknown" else "Unknown"
                    val cachedName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val dateMs = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val callType = if (typeIdx >= 0) cursor.getInt(typeIdx) else 1

                    val displayName = if (!cachedName.isNullOrBlank()) cachedName else number
                    val formattedTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(dateMs))

                    val typeStr = when (callType) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        else -> "Call"
                    }

                    TaskExecutionResult(
                        isSuccess = true,
                        method = "CallLog ContentProvider Query",
                        spokenFeedback = "Aakhiri call $displayName ki thi, $formattedTime par. Yeh ek $typeStr call thi."
                    )
                } else {
                    TaskExecutionResult(
                        isSuccess = true,
                        method = "CallLog ContentProvider Query",
                        spokenFeedback = if (contactQuery != null) "$contactQuery ki koi call history nahi mili." else "Call history khali hai."
                    )
                }
            } ?: TaskExecutionResult(
                isSuccess = false,
                method = "CallLog ContentProvider Query",
                spokenFeedback = "Call log data prapt nahi ho saka.",
                failureReason = "Cursor returned null"
            )
        } catch (e: Exception) {
            TaskExecutionResult(
                isSuccess = false,
                method = "CallLog ContentProvider",
                spokenFeedback = "Call history check karne mein truti aayi: ${e.localizedMessage}",
                failureReason = e.message
            )
        }
    }
}
