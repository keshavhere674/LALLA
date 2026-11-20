package app.lalla.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val type: String,                   // ALARM, WHATSAPP, REMINDER, GENERIC
    val targetContact: String? = null,  // WhatsApp recipient name/number
    val targetMessage: String? = null,  // WhatsApp message body
    val triggerTimeMs: Long? = null,    // epoch ms for alarm/reminder
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val conversationId: Long = 0
)
