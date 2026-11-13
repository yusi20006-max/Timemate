package ir.timemate.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_history",
    indices = [Index(value = ["sessionId"], unique = true), Index(value = ["startedAtMillis"])]
)
data class AlarmHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Int,
    val sessionId: String,
    val scheduledAtMillis: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val snoozeCount: Int = 0,
    val durationMillis: Long? = null,
    val status: String = STATUS_ACTIVE,
    val lastSnoozedAtMillis: Long? = null
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_DISMISSED = "DISMISSED"
    }
}
