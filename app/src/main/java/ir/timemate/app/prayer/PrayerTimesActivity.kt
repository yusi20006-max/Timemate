package ir.timemate.app.prayer
import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import ir.timemate.app.widget.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.timemate.app.ui.theme.AppTheme
import ir.timemate.app.ui.theme.TimeMateTheme
class PrayerTimesActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{TimeMateTheme(AppTheme.DEEP_OCEAN){PrayerTimesScreen()}}}
}
@Composable fun PrayerTimesScreen(){
 val context=androidx.compose.ui.platform.LocalContext.current
 var cityId by remember { mutableStateOf(context.getSharedPreferences("prayer_times",0).getString("city_id","tehran")?:"tehran") }
 val city=PrayerCities.provincialCapitals.first{it.id==cityId}
 val state=remember{OfficialPrayerTimesRepository().currentState()}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Text("اوقات شرعی",style=MaterialTheme.typography.headlineMedium);Text(PrayerTimesSource.DISPLAY_NAME,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("شهر انتخاب‌شده: ${city.title}",style=MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(onClick={val i=PrayerCities.provincialCapitals.indexOfFirst{it.id==cityId};cityId=PrayerCities.provincialCapitals[(i-1).coerceAtLeast(0)].id;context.getSharedPreferences("prayer_times",0).edit().putString("city_id",cityId).apply();BasePrayerTimesWidgetProvider.updateAll(context)}){Text("شهر قبلی")}
    OutlinedButton(onClick={val i=PrayerCities.provincialCapitals.indexOfFirst{it.id==cityId};cityId=PrayerCities.provincialCapitals[(i+1)%PrayerCities.provincialCapitals.size].id;context.getSharedPreferences("prayer_times",0).edit().putString("city_id",cityId).apply()}){Text("شهر بعدی")}
   }
  }}}
  item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("داده رسمی فعلاً در دسترس نیست",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.error)
   Text((state as PrayerTimesState.Unavailable).reason)
   Text("هیچ زمان محاسباتی یا داده‌ای از سرویس ثالث جایگزین منبع دانشگاه تهران نمی‌شود.")
   Text("صفحه مرجع: ${PrayerTimesSource.OFFICIAL_PAGE}",color=MaterialTheme.colorScheme.primary)
  }}}
  item{Text("پنج طرح ویجت اوقات شرعی",style=MaterialTheme.typography.titleLarge)}
  item{Text("هر طرح را می‌توانید جداگانه به صفحه اصلی اضافه کنید. چند نمونه از هر طرح نیز قابل نصب است.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item{PrayerWidgetAddCard("کلاسیک تیره",PrayerWidgetClassicProvider::class.java)}
  item{PrayerWidgetAddCard("روشن تصویری",PrayerWidgetBrightProvider::class.java)}
  item{PrayerWidgetAddCard("مدرن مینیمال",PrayerWidgetMinimalProvider::class.java)}
  item{PrayerWidgetAddCard("شیشه‌ای رنگی",PrayerWidgetGlassProvider::class.java)}
  item{PrayerWidgetAddCard("سنتی ایرانی",PrayerWidgetPersianProvider::class.java)}
 }
}

@Composable
private fun PrayerWidgetAddCard(title:String,provider:Class<out android.appwidget.AppWidgetProvider>){
 val context=androidx.compose.ui.platform.LocalContext.current
 Card(Modifier.fillMaxWidth()){
  Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){
   Text(title,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
   Button(onClick={
    val manager=AppWidgetManager.getInstance(context)
    if(manager.isRequestPinAppWidgetSupported){
     manager.requestPinAppWidget(ComponentName(context,provider),null,null)
    }else{
     Toast.makeText(context,"از فهرست Widgets لانچر، ویجت TimeMate را اضافه کنید.",Toast.LENGTH_LONG).show()
    }
   }){Text("افزودن")}
  }
 }
}
