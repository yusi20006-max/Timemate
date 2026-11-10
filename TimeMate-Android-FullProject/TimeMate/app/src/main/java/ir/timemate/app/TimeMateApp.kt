package ir.timemate.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ir.timemate.app.data.local.TimeMateDatabase

class TimeMateApp : Application() {
    val database: TimeMateDatabase by lazy { TimeMateDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "بیدارباش و زنگ اضطراری",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "کانال اختصاصی پخش صدای بیدارباش روی صفحه قفل"
                enableVibration(true)
                setSound(null, null) // Sound handled directly by AlarmService MediaPlayer
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "یادآور کارها و روتین‌ها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان یادآورهای زمان‌بندی‌شده روزانه"
                enableVibration(true)
            }

            manager.createNotificationChannel(alarmChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        const val CHANNEL_ALARM_ID = "timemate_alarm_channel"
        const val CHANNEL_REMINDER_ID = "timemate_reminder_channel"
    }
}
