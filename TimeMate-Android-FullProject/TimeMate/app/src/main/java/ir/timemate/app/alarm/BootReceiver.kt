package ir.timemate.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.timemate.app.TimeMateApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = (context.applicationContext as TimeMateApp).database
                val enabledAlarms = db.dao().getEnabledAlarmsSnapshot()
                enabledAlarms.forEach { alarm ->
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }

                val activeReminders = db.dao().getActiveRemindersSnapshot()
                activeReminders.forEach { reminder ->
                    AlarmScheduler.scheduleReminder(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
