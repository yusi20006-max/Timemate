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
import kotlinx.coroutines.launch
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
    var cityId by remember {
        mutableStateOf(context.getSharedPreferences("prayer_times", 0).getString("city_id", "tehran") ?: "tehran")
    }
    val city = PrayerCities.provincialCapitals.firstOrNull { it.id == cityId } ?: PrayerCities.provincialCapitals.first()
    val zone = remember(city.timeZoneId) { runCatching { ZoneId.of(city.timeZoneId) }.getOrDefault(ZoneId.of("Asia/Tehran")) }
    val cache = remember(context) { PrayerTimesCache(context) }
    val repository = remember(context) { OfficialPrayerTimesRepository(context) }
    val scope = rememberCoroutineScope()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshMessage by remember { mutableStateOf("در حال بررسی داده رسمی دانشگاه تهران…") }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val now = remember(nowMillis, zone) { ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone) }
    val today = now.toLocalDate()

    LaunchedEffect(city.id, today.toString()) {
        isRefreshing = true
        val success = repository.refreshYear(city)
        refreshVersion++
        isRefreshing = false
        refreshMessage = if (success) {
            "داده رسمی سال ۱۴۰۵ دریافت یا از حافظه معتبر بازیابی شد."
        } else {
            "دریافت تازه ممکن نشد؛ در صورت وجود، آخرین داده معتبر ذخیره‌شده نمایش داده می‌شود."
        }
        BasePrayerTimesWidgetProvider.updateAll(context)
    }

    val cached = remember(city.id, today.toString(), refreshVersion, nowMillis / 60_000) {
        cache.get(city, today.toString())
    }
    val tomorrow = remember(city.id, today.toString(), refreshVersion, nowMillis / 60_000) {
        cache.get(city, today.plusDays(1).toString())?.day
    }
    val next = remember(cached, tomorrow, nowMillis) {
        cached?.let { PrayerTimesEngine.nextPrayer(it.day, tomorrow, now) }
    }

    fun refreshNow() {
        scope.launch {
            isRefreshing = true
            refreshMessage = "در حال دریافت فایل رسمی دانشگاه تهران…"
            val success = repository.refreshYear(city, force = true)
            refreshVersion++
            isRefreshing = false
            refreshMessage = if (success) "داده رسمی با موفقیت دریافت و اعتبارسنجی شد."
            else "دریافت یا اعتبارسنجی فایل رسمی ناموفق بود؛ داده قبلی حفظ شد."
            BasePrayerTimesWidgetProvider.updateAll(context)
        }
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
                    Text("شهر انتخاب‌شده: ${city.title}", style = MaterialTheme.typography.titleMedium)
                    Text("تاریخ: ${PersianCalendarUtils.today().formatFull()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val index = PrayerCities.provincialCapitals.indexOfFirst { it.id == cityId }
                            cityId = PrayerCities.provincialCapitals[(index - 1).coerceAtLeast(0)].id
                            context.getSharedPreferences("prayer_times", 0).edit().putString("city_id", cityId).apply()
                            BasePrayerTimesWidgetProvider.updateAll(context)
                        }) { Text("شهر قبلی") }
                        OutlinedButton(onClick = {
                            val index = PrayerCities.provincialCapitals.indexOfFirst { it.id == cityId }
                            cityId = PrayerCities.provincialCapitals[(index + 1) % PrayerCities.provincialCapitals.size].id
                            context.getSharedPreferences("prayer_times", 0).edit().putString("city_id", cityId).apply()
                            BasePrayerTimesWidgetProvider.updateAll(context)
                        }) { Text("شهر بعدی") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(refreshMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { refreshNow() }, enabled = !isRefreshing) {
                        Text(if (isRefreshing) "در حال دریافت…" else "به‌روزرسانی از دانشگاه تهران")
                    }
                    if (cached == null) {
                        Text("داده رسمی برای این شهر و تاریخ در دسترس نیست", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text("در صورت نبود اینترنت یا فایل معتبر، هیچ زمان محاسباتی یا داده ثالث جایگزین نمی‌شود.")
                    } else {
                        Text("داده رسمی ذخیره‌شده", style = MaterialTheme.typography.titleMedium)
                        Text("آخرین دریافت: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(cached.fetchedAtMillis))}")
                        if (next != null) {
                            Text("وقت بعدی: ${next.event.label}، ساعت ${next.at.toLocalTime()}", style = MaterialTheme.typography.titleLarge)
                            Text("زمان باقی‌مانده: ${PrayerTimesEngine.formatCountdown(next.remaining)}", style = MaterialTheme.typography.headlineMedium)
                        } else {
                            Text("وقت بعدی قابل تعیین نیست؛ داده روز بعد در حافظه موجود نیست.")
                        }
                        listOf(
                            "اذان صبح" to cached.day.fajr,
                            "طلوع آفتاب" to cached.day.sunrise,
                            "اذان ظهر" to cached.day.dhuhr,
                            "غروب آفتاب" to cached.day.sunset,
                            "اذان مغرب" to cached.day.maghrib,
                            "نیمه‌شب شرعی" to cached.day.midnight
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
