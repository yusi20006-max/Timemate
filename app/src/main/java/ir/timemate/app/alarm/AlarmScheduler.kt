package ir.timemate.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import ir.timemate.app.MainActivity
import ir.timemate.app.data.model.AlarmEntity
import ir.timemate.app.data.model.ReminderEntity
import java.util.Calendar
import java.util.UUID

object AlarmScheduler {

    private const val HOURLY_CHIME_REQ_CODE = 77700

    fun scheduleNextHourlyChime(context: Context) {
        if (!HourlyChimeSynthesizer.isHourlyChimeEnabled(context)) {
            cancelHourlyChime(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startHour = HourlyChimeSynthesizer.getStartHour(context) // 7
        val endHour = HourlyChimeSynthesizer.getEndHour(context)     // 23

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }

        val nextHour = next.get(Calendar.HOUR_OF_DAY)
        if (nextHour < startHour) {
            next.set(Calendar.HOUR_OF_DAY, startHour)
        } else if (nextHour > endHour) {
            next.add(Calendar.DAY_OF_YEAR, 1)
            next.set(Calendar.HOUR_OF_DAY, startHour)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HOURLY_CHIME
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            HOURLY_CHIME_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelHourlyChime(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HOURLY_CHIME
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            HOURLY_CHIME_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val triggerTime = computeNextAlarmTriggerMillis(alarm.hour, alarm.minute, alarm.daysOfWeek)
        val sessionId = UUID.randomUUID().toString()

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_IS_VIBRATE, alarm.isVibrate)
            putExtra(AlarmReceiver.EXTRA_IS_GRADUAL, alarm.isGradualVolume)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, SnoozePreferences.getMinutes(context))
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, false)
            putExtra(AlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(AlarmReceiver.EXTRA_SCHEDULED_AT, triggerTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun scheduleSnooze(
        context: Context,
        alarmId: Int,
        title: String,
        snoozeMinutes: Int,
        isVibrate: Boolean,
        isGradual: Boolean,
        ringtoneUri: String?,
        sessionId: String,
        snoozeCount: Int,
        triggerAtMillis: Long? = null
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = maxOf(
            triggerAtMillis ?: (System.currentTimeMillis() + snoozeMinutes * 60 * 1000L),
            System.currentTimeMillis() + 1000L
        )
        val baseTitle = title.removeSuffix(" (چرت)")
        SnoozeSessionStore.savePending(
            context,
            SnoozeSession(
                alarmId = alarmId,
                sessionId = sessionId,
                snoozeCount = snoozeCount,
                nextTriggerAtMillis = triggerTime,
                title = baseTitle,
                isVibrate = isVibrate,
                isGradual = isGradual,
                ringtoneUri = ringtoneUri
            )
        )

        val snoozeData = Uri.parse("timemate://snooze/$alarmId/$sessionId")
        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            data = snoozeData
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_TITLE, "$baseTitle (چرت)")
            putExtra(AlarmReceiver.EXTRA_IS_VIBRATE, isVibrate)
            putExtra(AlarmReceiver.EXTRA_IS_GRADUAL, isGradual)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, ringtoneUri)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, SnoozePreferences.getMinutes(context))
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_SESSION_ID, sessionId)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, snoozeCount)
            putExtra(AlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(AlarmReceiver.EXTRA_SCHEDULED_AT, triggerTime)
        }

        val requestCode = SNOOZE_REQUEST_CODE_BASE + alarmId
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent), pendingIntent)
            true
        } catch (e: SecurityException) {
            SnoozeSessionStore.clear(context, alarmId, sessionId)
            e.printStackTrace()
            false
        }
    }

    fun cancelSnooze(context: Context, alarmId: Int, sessionId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            data = Uri.parse("timemate://snooze/$alarmId/$sessionId")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE_BASE + alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
            it.cancel()
        }
        SnoozeSessionStore.clear(context, alarmId, sessionId)
    }

    private const val SNOOZE_REQUEST_CODE_BASE = 50000

    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        if (reminder.isCompleted || reminder.targetTimeMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_DESC, reminder.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetTimeMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun computeNextAlarmTriggerMillis(hour: Int, minute: Int, daysOfWeek: List<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = target.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val candidateShamsiDow = when (candidate.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> 0
                Calendar.SUNDAY -> 1
                Calendar.MONDAY -> 2
                Calendar.TUESDAY -> 3
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 5
                Calendar.FRIDAY -> 6
                else -> 0
            }
            if (candidateShamsiDow in daysOfWeek && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }
}
