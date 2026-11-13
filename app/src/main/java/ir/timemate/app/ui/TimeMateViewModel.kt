package ir.timemate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import ir.timemate.app.TimeMateApp
import ir.timemate.app.alarm.AlarmScheduler
import ir.timemate.app.data.model.AlarmEntity
import ir.timemate.app.data.model.HabitEntity
import ir.timemate.app.data.model.ReminderEntity
import ir.timemate.app.data.model.AlarmHistoryEntity
import ir.timemate.app.util.PersianCalendarUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class TimeMateViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as TimeMateApp).database.dao()
    private val historyDao = (application as TimeMateApp).database.alarmHistoryDao()
    private val appContext = application.applicationContext

    val alarms = dao.getAllAlarms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = dao.getAllReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habits = dao.getAllHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlarm(
        hour: Int,
        minute: Int,
        title: String,
        daysOfWeek: List<Int>,
        isVibrate: Boolean,
        isGradual: Boolean
    ) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                hour = hour,
                minute = minute,
                title = title.ifBlank { "بیدارباش" },
                daysOfWeek = daysOfWeek,
                isVibrate = isVibrate,
                isGradualVolume = isGradual,
                isEnabled = true
            )
            val id = dao.insertAlarm(alarm).toInt()
            AlarmScheduler.scheduleAlarm(appContext, alarm.copy(id = id))
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = enabled)
            dao.updateAlarm(updated)
            AlarmScheduler.scheduleAlarm(appContext, updated)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(appContext, alarm.id)
            dao.deleteAlarm(alarm)
        }
    }

    fun addReminder(
        title: String,
        description: String,
        persianDate: String,
        hour: Int,
        minute: Int,
        priority: Int,
        repeatMode: String,
        autoRepeat: Boolean
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val reminder = ReminderEntity(
                title = title,
                description = description,
                persianDate = persianDate,
                hour = hour,
                minute = minute,
                targetTimeMillis = cal.timeInMillis,
                priority = priority,
                repeatMode = repeatMode,
                autoRepeatIfUnfinished = autoRepeat
            )
            val id = dao.insertReminder(reminder).toInt()
            AlarmScheduler.scheduleReminder(appContext, reminder.copy(id = id))
        }
    }

    fun toggleReminderDone(reminder: ReminderEntity) {
        viewModelScope.launch {
            dao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            dao.deleteReminder(reminder)
        }
    }

    fun addHabit(title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                dao.insertHabit(HabitEntity(title = title.trim()))
            }
        }
    }

    fun checkInHabitToday(habit: HabitEntity) {
        viewModelScope.launch {
            val todayStr = PersianCalendarUtils.today().formatNumeric()
            if (habit.lastCompletedPersianDate == todayStr) return@launch
            val newStreak = habit.currentStreak + 1
            val best = maxOf(habit.bestStreak, newStreak)
            val updatedHistory = (habit.completedDatesHistory + todayStr).takeLast(30)
            dao.updateHabit(
                habit.copy(
                    currentStreak = newStreak,
                    bestStreak = best,
                    lastCompletedPersianDate = todayStr,
                    completedDatesHistory = updatedHistory
                )
            )
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            dao.deleteHabit(habit)
        }
    }


    fun observeAlarmHistoryStats(days: Int): Flow<AlarmHistoryStats> {
        val rangeDays = if (days <= 7) 7 else 30
        val now = System.currentTimeMillis()
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(rangeDays - 1))
        }
        val startMillis = startCalendar.timeInMillis
        val untilMillis = now + 1L

        return historyDao.observeHistoryBetween(startMillis, untilMillis).map { records ->
            val completed = records.filter {
                it.endedAtMillis != null && it.durationMillis != null && it.durationMillis >= 0L
            }
            val averageSnoozes = if (records.isEmpty()) 0.0
                else records.map { it.snoozeCount.coerceAtLeast(0) }.average()
            val averageDismissMinutes = completed
                .mapNotNull { it.durationMillis }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.div(60_000.0)

            val dailyCounts = (0 until rangeDays).map { offset ->
                val day = Calendar.getInstance().apply {
                    timeInMillis = startMillis
                    add(Calendar.DAY_OF_YEAR, offset)
                }
                val dayStart = day.timeInMillis
                day.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = day.timeInMillis
                DailyAlarmCount(
                    dayStartMillis = dayStart,
                    count = records.count { it.startedAtMillis >= dayStart && it.startedAtMillis < dayEnd }
                )
            }

            AlarmHistoryStats(
                totalAlarms = records.size,
                averageSnoozes = averageSnoozes,
                averageDismissMinutes = averageDismissMinutes,
                completedCount = completed.size,
                dailyCounts = dailyCounts,
                records = records
            )
        }
    }

    fun exportBackupJson(): String {
        val payload = mapOf(
            "alarms" to alarms.value,
            "reminders" to reminders.value,
            "habits" to habits.value
        )
        return GsonBuilder().setPrettyPrinting().create().toJson(payload)
    }
}

data class DailyAlarmCount(
    val dayStartMillis: Long,
    val count: Int
)

data class AlarmHistoryStats(
    val totalAlarms: Int = 0,
    val averageSnoozes: Double = 0.0,
    val averageDismissMinutes: Double? = null,
    val completedCount: Int = 0,
    val dailyCounts: List<DailyAlarmCount> = emptyList(),
    val records: List<AlarmHistoryEntity> = emptyList()
)
