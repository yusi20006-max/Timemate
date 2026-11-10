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
import ir.timemate.app.util.PersianCalendarUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TimeMateViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as TimeMateApp).database.dao()
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

    fun exportBackupJson(): String {
        val payload = mapOf(
            "alarms" to alarms.value,
            "reminders" to reminders.value,
            "habits" to habits.value
        )
        return GsonBuilder().setPrettyPrinting().create().toJson(payload)
    }
}
