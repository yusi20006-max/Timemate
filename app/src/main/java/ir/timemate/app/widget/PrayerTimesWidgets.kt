package ir.timemate.app.widget
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.timemate.app.R
import ir.timemate.app.prayer.PrayerTimesActivity
import ir.timemate.app.prayer.PrayerTimesSource
import ir.timemate.app.prayer.PrayerCities
import ir.timemate.app.util.PersianCalendarUtils
abstract class BasePrayerTimesWidgetProvider(private val layoutId:Int):AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){ids.forEach{render(context,manager,it,layoutId)}}
 override fun onReceive(context:Context,intent:Intent){super.onReceive(context,intent);if(intent.action in listOf(Intent.ACTION_DATE_CHANGED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED)) updateAll(context)}
 companion object{
  private fun render(context:Context,manager:AppWidgetManager,id:Int,layout:Int){
   val views=RemoteViews(context.packageName,layout)
   val prefs=context.getSharedPreferences("prayer_times",Context.MODE_PRIVATE)
   val cityId=prefs.getString("city_id","tehran")?:"tehran"
   val city=PrayerCities.provincialCapitals.firstOrNull{it.id==cityId}?:PrayerCities.provincialCapitals.first()
   views.setTextViewText(R.id.prayer_city,city.title)
   views.setTextViewText(R.id.prayer_date,PersianCalendarUtils.today().formatFull())
   views.setTextViewText(R.id.prayer_status,"داده رسمی در دسترس نیست")
   views.setTextViewText(R.id.prayer_source,PrayerTimesSource.DISPLAY_NAME)
   views.setTextViewText(R.id.prayer_next,"پس از تأیید منبع رسمی نمایش داده می‌شود")
   val open=Intent(context,PrayerTimesActivity::class.java)
   val pending=PendingIntent.getActivity(context,id,open,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   views.setOnClickPendingIntent(R.id.prayer_widget_root,pending)
   manager.updateAppWidget(id,views)
  }
  fun updateAll(context:Context){
   val manager=AppWidgetManager.getInstance(context)
   listOf(PrayerWidgetClassicProvider::class.java,PrayerWidgetBrightProvider::class.java,PrayerWidgetMinimalProvider::class.java,PrayerWidgetGlassProvider::class.java,PrayerWidgetPersianProvider::class.java).forEach{provider->
    manager.getAppWidgetIds(ComponentName(context,provider)).forEach{id->
     val info=manager.getAppWidgetInfo(id)
     if(info != null) render(context,manager,id,info.initialLayout)
    }
   }
  }
 }
}
class PrayerWidgetClassicProvider:BasePrayerTimesWidgetProvider(R.layout.widget_prayer_classic)
class PrayerWidgetBrightProvider:BasePrayerTimesWidgetProvider(R.layout.widget_prayer_bright)
class PrayerWidgetMinimalProvider:BasePrayerTimesWidgetProvider(R.layout.widget_prayer_minimal)
class PrayerWidgetGlassProvider:BasePrayerTimesWidgetProvider(R.layout.widget_prayer_glass)
class PrayerWidgetPersianProvider:BasePrayerTimesWidgetProvider(R.layout.widget_prayer_persian)
