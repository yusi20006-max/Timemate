package ir.timemate.app.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.timemate.app.MainActivity
import ir.timemate.app.TimeMateApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HOURLY_CHIME -> {
                if (HourlyChimeSynthesizer.isHourlyChimeEnabled(context)) {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val startHour = HourlyChimeSynthesizer.getStartHour(context)
                    val endHour = HourlyChimeSynthesizer.getEndHour(context)
                    if (currentHour in startHour..endHour) {
                        HourlyChimeSynthesizer.playDingDing()
                    }
                    AlarmScheduler.scheduleNextHourlyChime(context)
                }
            }

            ACTION_TRIGGER_ALARM -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "بیدارباش TimeMate"
                val isVibrate = intent.getBooleanExtra(EXTRA_IS_VIBRATE, true)
                val isGradual = intent.getBooleanExtra(EXTRA_IS_GRADUAL, true)
                val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
                val snoozeMinutes = SnoozePreferences.getMinutes(context)
                val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
                val suppliedSessionId = intent.getStringExtra(EXTRA_SNOOZE_SESSION_ID)
                val suppliedSnoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
                val session = if (isSnooze && !suppliedSessionId.isNullOrBlank()) {
                    SnoozeSessionStore.resume(
                        context, alarmId, suppliedSessionId, suppliedSnoozeCount,
                        title.removeSuffix(" (چرت)"), isVibrate, isGradual, ringtoneUri
                    ).also { SnoozeSessionStore.markTriggered(context, it) }
                } else {
                    SnoozeSessionStore.startNew(
                        context, alarmId, title, isVibrate, isGradual, ringtoneUri
                    )
                }

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_IS_VIBRATE, isVibrate)
                    putExtra(EXTRA_IS_GRADUAL, isGradual)
                    putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
                    putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    putExtra(EXTRA_SNOOZE_SESSION_ID, session.sessionId)
                    putExtra(EXTRA_SNOOZE_COUNT, session.snoozeCount)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                if (alarmId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = (context.applicationContext as TimeMateApp).database
                            val alarm = db.dao().getAlarmById(alarmId)
                            if (alarm != null) {
                                if (alarm.daysOfWeek.isNotEmpty()) {
                                    AlarmScheduler.scheduleAlarm(context, alarm)
                                } else {
                                    db.dao().updateAlarm(alarm.copy(isEnabled = false))
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآور هوشمند"
                val desc = intent.getStringExtra(EXTRA_DESC) ?: ""

                showReminderNotification(context, reminderId, title, desc)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = (context.applicationContext as TimeMateApp).database
                        val reminder = db.dao().getReminderById(reminderId)
                        if (reminder != null && !reminder.isCompleted && reminder.autoRepeatIfUnfinished) {
                            val nextTime = System.currentTimeMillis() + (30 * 60 * 1000L)
                            val updated = reminder.copy(targetTimeMillis = nextTime)
                            db.dao().updateReminder(updated)
                            AlarmScheduler.scheduleReminder(context, updated)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun showReminderNotification(context: Context, id: Int, title: String, desc: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TimeMateApp.CHANNEL_REMINDER_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(if (desc.isBlank()) "زمان انجام کار فرا رسیده است" else desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(id + 20000, notification)
    }

    companion object {
        const val ACTION_HOURLY_CHIME = "ir.timemate.app.ACTION_HOURLY_CHIME"
        const val ACTION_TRIGGER_ALARM = "ir.timemate.app.ACTION_TRIGGER_ALARM"
        const val ACTION_TRIGGER_REMINDER = "ir.timemate.app.ACTION_TRIGGER_REMINDER"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_IS_VIBRATE = "extra_is_vibrate"
        const val EXTRA_IS_GRADUAL = "extra_is_gradual"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_SNOOZE_SESSION_ID = "extra_snooze_session_id"
        const val EXTRA_SNOOZE_COUNT = "extra_snooze_count"
    }
}
