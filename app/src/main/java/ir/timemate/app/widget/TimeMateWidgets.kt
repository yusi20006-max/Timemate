package ir.timemate.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import ir.timemate.app.MainActivity
import ir.timemate.app.R
import ir.timemate.app.util.PersianCalendarUtils
import ir.timemate.app.util.toPersianDigits

abstract class BaseMinimalWidgetProvider(private val styleType: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, styleType)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in listOf(
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java))
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            styleType: Int
        ) {
            val today = PersianCalendarUtils.today()
            val dayMonthStr = "${toPersianDigits(today.day)} ${today.monthName}"
            val yearStr = toPersianDigits(today.year)
            val weekdayStr = today.dayOfWeekName
            val fullDateStr = "$dayMonthStr $yearStr"
            val inlineDateStr = "$weekdayStr، $dayMonthStr $yearStr"

            val layoutId = when (styleType) {
                1 -> R.layout.widget_minimal_1
                2 -> R.layout.widget_minimal_2
                3 -> R.layout.widget_minimal_3
                else -> R.layout.widget_minimal_4
            }

            val views = RemoteViews(context.packageName, layoutId)

            when (styleType) {
                1, 2 -> {
                    views.setTextViewText(R.id.tv_year, yearStr)
                    views.setTextViewText(R.id.tv_day_month, dayMonthStr)
                    views.setTextViewText(R.id.tv_weekday, weekdayStr)
                }
                3 -> {
                    views.setTextViewText(R.id.tv_weekday, weekdayStr)
                    views.setTextViewText(R.id.tv_full_date, fullDateStr)
                }
                4 -> {
                    views.setTextViewText(R.id.tv_inline_date, inlineDateStr)
                }
            }

            val openIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                styleType,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val providers = listOf(
                1 to TimeMateWidget1Provider::class.java,
                2 to TimeMateWidget2Provider::class.java,
                3 to TimeMateWidget3Provider::class.java,
                4 to TimeMateWidget4Provider::class.java
            )
            providers.forEach { (style, cls) ->
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                ids.forEach { id -> updateWidget(context, manager, id, style) }
            }
        }

        fun requestPinWidget(context: Context, styleType: Int) {
            val manager = AppWidgetManager.getInstance(context)
            val providerClass = when (styleType) {
                1 -> TimeMateWidget1Provider::class.java
                2 -> TimeMateWidget2Provider::class.java
                3 -> TimeMateWidget3Provider::class.java
                else -> TimeMateWidget4Provider::class.java
            }
            val provider = ComponentName(context, providerClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
                manager.requestPinAppWidget(provider, null, null)
            } else {
                Toast.makeText(
                    context,
                    "روی صفحه اصلی گوشی انگشت خود را نگه دارید و از بخش Widgets ویجت TimeMate را اضافه کنید.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

class TimeMateWidget1Provider : BaseMinimalWidgetProvider(1)
class TimeMateWidget2Provider : BaseMinimalWidgetProvider(2)
class TimeMateWidget3Provider : BaseMinimalWidgetProvider(3)
class TimeMateWidget4Provider : BaseMinimalWidgetProvider(4)
