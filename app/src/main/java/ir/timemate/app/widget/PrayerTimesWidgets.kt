package ir.timemate.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.os.SystemClock
import ir.timemate.app.R
import ir.timemate.app.prayer.PrayerTimesActivity
import ir.timemate.app.prayer.PrayerTimesSource
import ir.timemate.app.prayer.PrayerCities
import ir.timemate.app.prayer.PrayerTimesCache
import ir.timemate.app.prayer.PrayerTimesEngine
import ir.timemate.app.util.PersianCalendarUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

abstract class BasePrayerTimesWidgetProvider(private val layoutId: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it, layoutId) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in listOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            updateAll(context)
        }
    }

    companion object {
        private fun render(context: Context, manager: AppWidgetManager, id: Int, layout: Int) {
            val views = RemoteViews(context.packageName, layout)
            val prefs = context.getSharedPreferences("prayer_times", Context.MODE_PRIVATE)
            val cityId = prefs.getString("city_id", "tehran") ?: "tehran"
            val city = PrayerCities.provincialCapitals.firstOrNull { it.id == cityId } ?: PrayerCities.provincialCapitals.first()
            val zone = runCatching { ZoneId.of(city.timeZoneId) }.getOrDefault(ZoneId.of("Asia/Tehran"))
            val now = ZonedDateTime.ofInstant(Instant.now(), zone)
            val today = now.toLocalDate()
            val cache = PrayerTimesCache(context)
            val cached = cache.get(city, today.toString())

            views.setTextViewText(R.id.prayer_city, city.title)
            views.setTextViewText(R.id.prayer_date, PersianCalendarUtils.today().formatFull())
            views.setTextViewText(R.id.prayer_source, PrayerTimesSource.DISPLAY_NAME)

            if (cached == null) {
                views.setTextViewText(R.id.prayer_status, "داده رسمی در دسترس نیست")
                views.setChronometer(R.id.prayer_next, SystemClock.elapsedRealtime(), "%s", false)\n                views.setTextViewText(R.id.prayer_next, "پس از دریافت داده معتبر نمایش داده می‌شود")
            } else {
                val tomorrow = cache.get(city, today.plusDays(1).toString())?.day
                val next = PrayerTimesEngine.nextPrayer(cached.day, tomorrow, now)
                views.setTextViewText(R.id.prayer_status, "داده ذخیره‌شده آفلاین")
                views.setTextViewText(
                    R.id.prayer_next,
                    if (next == null) "وقت بعدی در داده ذخیره‌شده موجود نیست"
                    else "${next.event.label} • ${PrayerTimesEngine.formatCountdown(next.remaining)}"
                )
            }

            val open = Intent(context, PrayerTimesActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.prayer_widget_root, pending)
            manager.updateAppWidget(id, views)
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            listOf(
                PrayerWidgetClassicProvider::class.java,
                PrayerWidgetBrightProvider::class.java,
                PrayerWidgetMinimalProvider::class.java,
                PrayerWidgetGlassProvider::class.java,
                PrayerWidgetPersianProvider::class.java
            ).forEach { provider ->
                manager.getAppWidgetIds(ComponentName(context, provider)).forEach { id ->
                    val info = manager.getAppWidgetInfo(id)
                    if (info != null) render(context, manager, id, info.initialLayout)
                }
            }
        }
    }
}

class PrayerWidgetClassicProvider : BasePrayerTimesWidgetProvider(R.layout.widget_prayer_classic)
class PrayerWidgetBrightProvider : BasePrayerTimesWidgetProvider(R.layout.widget_prayer_bright)
class PrayerWidgetMinimalProvider : BasePrayerTimesWidgetProvider(R.layout.widget_prayer_minimal)
class PrayerWidgetGlassProvider : BasePrayerTimesWidgetProvider(R.layout.widget_prayer_glass)
class PrayerWidgetPersianProvider : BasePrayerTimesWidgetProvider(R.layout.widget_prayer_persian)
