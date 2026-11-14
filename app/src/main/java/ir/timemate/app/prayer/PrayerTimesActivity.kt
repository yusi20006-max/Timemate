package ir.timemate.app.prayer

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.timemate.app.ui.theme.AppTheme
import ir.timemate.app.ui.theme.TimeMateTheme
import ir.timemate.app.widget.*
import ir.timemate.app.util.PersianCalendarUtils
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerTimesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TimeMateTheme(AppTheme.DEEP_OCEAN) { PrayerTimesScreen() } }
    }
}

@Composable
fun PrayerTimesScreen() {
    val context = LocalContext.current
    val city = PrayerCities.provincialCapitals.single()
    val zone = remember(city.timeZoneId) { runCatching { ZoneId.of(city.timeZoneId) }.getOrDefault(ZoneId.of("Asia/Tehran")) }
    val repository = remember(context) { OfficialPrayerTimesRepository(context) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var refreshMessage by remember { mutableStateOf("داده آفلاین تهران آماده‌سازی می‌شود…") }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val now = remember(nowMillis, zone) { ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone) }
    val today = now.toLocalDate()

    LaunchedEffect(city.id) {
        val success = repository.ensureBundledYear(city)
        refreshVersion++
        refreshMessage = if (success) {
            "داده رسمی تهران برای سال ۱۴۰۵ از فایل داخلی برنامه بارگذاری شد."
        } else {
            "خواندن داده آفلاین تهران ناموفق بود؛ نصب برنامه را بررسی کنید."
        }
        BasePrayerTimesWidgetProvider.updateAll(context)
    }

    val cached = remember(city.id, today.toString(), refreshVersion) {
        repository.day(city, today.toString())
    }
    val tomorrow = remember(city.id, today.toString(), refreshVersion) {
        repository.day(city, today.plusDays(1).toString())
    }
    val next = remember(cached, tomorrow, nowMillis) {
        cached?.let { PrayerTimesEngine.nextPrayer(it, tomorrow, now) }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("اوقات شرعی", style = MaterialTheme.typography.headlineMedium)
            Text(PrayerTimesSource.DISPLAY_NAME, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("شهر: تهران", style = MaterialTheme.typography.titleMedium)
                    Text("تاریخ: ${PersianCalendarUtils.gregorianToJalali(today.year, today.monthValue, today.dayOfMonth).formatFull()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("نسخه فعلی داده‌ها: سال ۱۴۰۵", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(refreshMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (cached == null) {
                        Text("داده رسمی برای این شهر و تاریخ در دسترس نیست", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text("داده این تاریخ در نسخه فعلی موجود نیست؛ این نسخه فقط سال ۱۴۰۵ تهران را پوشش می‌دهد.")
                    } else {
                        Text("داده رسمی آفلاین همراه برنامه", style = MaterialTheme.typography.titleMedium)
                        if (next != null) {
                            Text("وقت بعدی: ${next.event.label}، ساعت ${next.at.toLocalTime()}", style = MaterialTheme.typography.titleLarge)
                            Text("زمان باقی‌مانده: ${PrayerTimesEngine.formatCountdown(next.remaining)}", style = MaterialTheme.typography.headlineMedium)
                        } else {
                            Text("وقت بعدی قابل تعیین نیست؛ داده روز بعد در حافظه موجود نیست.")
                        }
                        listOf(
                            "اذان صبح" to cached.fajr,
                            "طلوع آفتاب" to cached.sunrise,
                            "اذان ظهر" to cached.dhuhr,
                            "غروب آفتاب" to cached.sunset,
                            "اذان مغرب" to cached.maghrib,
                            "نیمه‌شب شرعی" to cached.midnight
                        ).forEach { (label, time) -> Text("$label: $time") }
                    }
                    Text("منبع: فایل PDF رسمی مرکز تقویم دانشگاه تهران", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text("پنج طرح ویجت اوقات شرعی", style = MaterialTheme.typography.titleLarge) }
        item { Text("هر طرح را جداگانه به صفحه اصلی اضافه کنید؛ چند نمونه هم‌زمان پشتیبانی می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { PrayerWidgetAddCard("کلاسیک تیره", PrayerWidgetClassicProvider::class.java) }
        item { PrayerWidgetAddCard("روشن تصویری", PrayerWidgetBrightProvider::class.java) }
        item { PrayerWidgetAddCard("مدرن مینیمال", PrayerWidgetMinimalProvider::class.java) }
        item { PrayerWidgetAddCard("شیشه‌ای رنگی", PrayerWidgetGlassProvider::class.java) }
        item { PrayerWidgetAddCard("سنتی ایرانی", PrayerWidgetPersianProvider::class.java) }
    }
}

@Composable
private fun PrayerWidgetAddCard(title: String, provider: Class<out android.appwidget.AppWidgetProvider>) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                val manager = AppWidgetManager.getInstance(context)
                if (manager.isRequestPinAppWidgetSupported) {
                    manager.requestPinAppWidget(ComponentName(context, provider), null, null)
                } else {
                    Toast.makeText(context, "از فهرست Widgets لانچر، ویجت TimeMate را اضافه کنید.", Toast.LENGTH_LONG).show()
                }
            }) { Text("افزودن") }
        }
    }
}
