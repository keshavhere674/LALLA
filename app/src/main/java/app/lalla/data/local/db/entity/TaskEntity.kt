package app.lalla.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val type: String,                   // ALARM, WHATSAPP, GMAIL, FILE_SEARCH, CALL_HISTORY, REMINDER, GENERIC
    val targetContact: String? = null,  // Contact name or number
    val targetMessage: String? = null,  // Message or email body
    val triggerTimeMs: Long? = null,    // Epoch ms for alarm/reminder
    val isCompleted: Boolean = false,
    val executionMethod: String? = null,// E.g., "AlarmManager", "ClockAppFallback", "WhatsAppDirect", "MediaStore"
    val resultStatus: String? = null,   // "SUCCESS", "FALLBACK_SUCCESS", "FAILED", "PERMISSION_REQUIRED"
    val failureReason: String? = null,  // Details if failed
    val executedAtMs: Long? = null,     // Execution timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val conversationId: Long = 0
)
