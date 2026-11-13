package ir.timemate.app.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object XiaomiPermissionHelper {

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") ||
            brand.contains("xiaomi") ||
            brand.contains("redmi") ||
            brand.contains("poco")
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        }.getOrDefault(false)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
    }

    fun autostartSettingsIntent(context: Context): Intent {
        val candidates = listOf(
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
            Intent("miui.intent.action.POWER_HIDE_MODE_ACTIVITY")
        )
        return candidates.firstOrNull { resolves(context, it) }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: appDetailsSettingsIntent(context)
    }

    fun batterySettingsIntent(context: Context): Intent {
        // App details is the most useful stable entry point on MIUI/HyperOS:
        // the user can open Battery saver and select "No restrictions" there.
        return appDetailsSettingsIntent(context)
    }

    fun exactAlarmSettingsIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return appDetailsSettingsIntent(context)
        }
        val exactAlarmSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return if (resolves(context, exactAlarmSettings)) {
            exactAlarmSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            appDetailsSettingsIntent(context)
        }
    }

    private fun appDetailsSettingsIntent(context: Context): Intent {
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (resolves(context, appDetails)) appDetails else Intent(Settings.ACTION_SETTINGS)
    }

    private fun resolves(context: Context, intent: Intent): Boolean {
        return runCatching { intent.resolveActivity(context.packageManager) != null }
            .getOrDefault(false)
    }
}
