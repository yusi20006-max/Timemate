package ir.timemate.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val title: String = "بیدارباش",
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = emptyList(), // 0=Shanbeh .. 6=Jomeh (empty = Once)
    val ringtoneUri: String? = null,
    val isVibrate: Boolean = true,
    val isGradualVolume: Boolean = true,
    val snoozeMinutes: Int = 5
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val persianDate: String, // e.g., 1405/08/20
    val hour: Int,
    val minute: Int,
    val targetTimeMillis: Long,
    val priority: Int = 2, // 1: فوری (High), 2: مهم (Medium), 3: عادی (Normal)
    val repeatMode: String = "NONE", // NONE, HOURLY, DAILY, WEEKLY, MONTHLY
    val autoRepeatIfUnfinished: Boolean = true,
    val isCompleted: Boolean = false
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String = "روزانه",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedPersianDate: String = "",
    val completedDatesHistory: List<String> = emptyList()
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromIntList(list: List<Int>?): String = gson.toJson(list ?: emptyList<Int>())

    @TypeConverter
    fun toIntList(data: String?): List<Int> {
        if (data.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(data, type)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String = gson.toJson(list ?: emptyList<String>())

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(data, type)
    }
}
