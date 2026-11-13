package ir.timemate.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.timemate.app.alarm.AlarmScheduler
import ir.timemate.app.alarm.HourlyChimeSynthesizer
import ir.timemate.app.alarm.SnoozePreferences
import ir.timemate.app.data.model.AlarmEntity
import ir.timemate.app.data.model.ReminderEntity
import ir.timemate.app.ui.TimeMateViewModel
import ir.timemate.app.ui.AlarmStatisticsScreen
import ir.timemate.app.ui.theme.*
import ir.timemate.app.util.*
import ir.timemate.app.widget.BaseMinimalWidgetProvider
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: TimeMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseMinimalWidgetProvider.updateAllWidgets(this)
        if (HourlyChimeSynthesizer.isHourlyChimeEnabled(this)) {
            AlarmScheduler.scheduleNextHourlyChime(this)
        }
        setContent {
            TimeMateTheme {
                RequestNotificationPermissionIfNeeded()
                TimeMateMainApp(viewModel)
            }
        }
    }
}

@Composable
private fun RequestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun TimeMateMainApp(viewModel: TimeMateViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showXiaomiGuide by remember { mutableStateOf(false) }
    val alarms by viewModel.alarms.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences(XIAOMI_GUIDE_PREFERENCES, Context.MODE_PRIVATE)
        if (XiaomiPermissionHelper.isXiaomiDevice() &&
            !preferences.getBoolean(XIAOMI_GUIDE_SEEN_KEY, false)
        ) {
            showXiaomiGuide = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepBlack,
                        MidnightBlue,
                        Color(0xFF140F2D),
                        DeepBlack
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassBottomNavigation(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(alarms, reminders, viewModel)
                    1 -> AlarmsScreen(alarms, viewModel)
                    2 -> RemindersScreen(reminders, viewModel)
                    3 -> DateConverterScreen()
                    4 -> AlarmStatisticsScreen(viewModel)
                    5 -> WidgetsGalleryScreen()
                    6 -> SettingsScreen(viewModel, onOpenXiaomiGuide = { showXiaomiGuide = true })
                }
            }
        }
    }

    if (showXiaomiGuide) {
        XiaomiFirstRunGuideDialog(
            onDismiss = {
                context.getSharedPreferences(XIAOMI_GUIDE_PREFERENCES, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(XIAOMI_GUIDE_SEEN_KEY, true)
                    .apply()
                showXiaomiGuide = false
            }
        )
    }
}

private const val XIAOMI_GUIDE_PREFERENCES = "timemate_xiaomi_guide"
private const val XIAOMI_GUIDE_SEEN_KEY = "first_run_guide_seen"

@Composable
private fun XiaomiFirstRunGuideDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var exactAlarmGranted by remember { mutableStateOf(XiaomiPermissionHelper.canScheduleExactAlarms(context)) }
    var batteryIgnored by remember { mutableStateOf(XiaomiPermissionHelper.isIgnoringBatteryOptimizations(context)) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        exactAlarmGranted = XiaomiPermissionHelper.canScheduleExactAlarms(context)
        batteryIgnored = XiaomiPermissionHelper.isIgnoringBatteryOptimizations(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightBlue,
        title = {
            Text("تنظیم اولیه برای پایداری بیدارباش", color = IceWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "در گوشی‌های Xiaomi، Redmi و POCO ممکن است مدیریت باتری اجرای زنگ را محدود کند. این راهنما فقط مسیر تنظیمات را باز می‌کند؛ فعال‌سازی گزینه‌ها باید توسط شما انجام شود.",
                    color = CoolSilver,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("۱. شروع خودکار (Autostart)", color = IceWhite, fontWeight = FontWeight.SemiBold)
                    Text("در تنظیمات بازشده، شروع خودکار TimeMate را فعال کنید. این وضعیت از طریق Android قابل تأیید مطمئن نیست و باید دستی بررسی شود.", color = CoolSilver, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { settingsLauncher.launch(XiaomiPermissionHelper.autostartSettingsIntent(context)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("باز کردن تنظیمات شروع خودکار")
                    }
                    Text("وضعیت: نیازمند بررسی دستی", color = SoftPurple, fontSize = 11.sp)
                }

                Divider(color = GlassBorder)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("۲. محدودیت باتری", color = IceWhite, fontWeight = FontWeight.SemiBold)
                    Text("در صفحه برنامه TimeMate، بخش Battery saver را روی «No restrictions / بدون محدودیت» قرار دهید. صفحه استاندارد Android ممکن است فقط فهرست برنامه‌ها را نشان دهد.", color = CoolSilver, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { settingsLauncher.launch(XiaomiPermissionHelper.batterySettingsIntent(context)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("باز کردن تنظیمات باتری")
                    }
                    Text(
                        if (batteryIgnored) "وضعیت قابل بررسی Android: معاف از بهینه‌سازی باتری" else "وضعیت قابل بررسی Android: معافیت فعال نیست؛ گزینه MIUI/HyperOS را نیز دستی بررسی کنید",
                        color = if (batteryIgnored) EmeraldSuccess else SoftPurple,
                        fontSize = 11.sp
                    )
                }

                Divider(color = GlassBorder)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("۳. مجوز زنگ دقیق", color = IceWhite, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            "برای زمان‌بندی دقیق زنگ، مجوز Alarms & reminders را بررسی کنید."
                        else
                            "در این نسخه اندروید، مجوز جداگانه زنگ دقیق لازم نیست.",
                        color = CoolSilver,
                        fontSize = 12.sp
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = { settingsLauncher.launch(XiaomiPermissionHelper.exactAlarmSettingsIntent(context)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("باز کردن تنظیمات زنگ دقیق")
                        }
                        Text(
                            if (exactAlarmGranted) "وضعیت: مجوز زنگ دقیق فعال است" else "وضعیت: مجوز زنگ دقیق فعال نیست",
                            color = if (exactAlarmGranted) EmeraldSuccess else SoftPurple,
                            fontSize = 11.sp
                        )
                    } else {
                        Text("وضعیت: توسط نسخه Android پشتیبانی/مدیریت می‌شود", color = EmeraldSuccess, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalAzure)
            ) {
                Text("متوجه شدم", color = IceWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بعداً", color = CoolSilver)
            }
        }
    )
}

@Composable
fun GlassBottomNavigation(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    val items = listOf(
        Triple("خانه", Icons.Default.Home, 0),
        Triple("بیدارباش", Icons.Default.Alarm, 1),
        Triple("یادآورها", Icons.Default.NotificationsActive, 2),
        Triple("مبدل تاریخ", Icons.Default.SwapHoriz, 3),
        Triple("آمار", Icons.Default.BarChart, 4),
        Triple("ویجت‌ها", Icons.Default.Widgets, 5),
        Triple("تنظیمات", Icons.Default.Settings, 6)
    )

    NavigationBar(
        containerColor = MidnightBlue.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        items.forEach { (label, icon, index) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) NeonViolet else CoolSilver
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) IceWhite else CoolSilver
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = RoyalAzure.copy(alpha = 0.28f)
                )
            )
        }
    }
}

// ==========================================
// 1. HOME SCREEN (Clock + Shamsi Calendar with Holidays & Occasions + Hourly Ding-Ding Quick Toggle)
// ==========================================
@Composable
fun HomeScreen(
    alarms: List<AlarmEntity>,
    reminders: List<ReminderEntity>,
    viewModel: TimeMateViewModel
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    var isAnalogClock by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000L)
        }
    }

    val jalaliToday = remember(currentTime.get(Calendar.DAY_OF_YEAR)) {
        PersianCalendarUtils.today()
    }
    var viewedMonth by remember { mutableIntStateOf(jalaliToday.month) }
    var viewedYear by remember { mutableIntStateOf(jalaliToday.year) }
    var selectedDay by remember { mutableIntStateOf(jalaliToday.day) }

    var isChimeEnabled by remember {
        mutableStateOf(HourlyChimeSynthesizer.isHourlyChimeEnabled(context))
    }

    val todayNumeric = jalaliToday.formatNumeric()
    val nowMillis = currentTime.timeInMillis
    val todayTasks = reminders.filter { it.persianDate == todayNumeric && !it.isCompleted }
    val overdueTasks = reminders.filter { !it.isCompleted && it.targetTimeMillis < nowMillis }

    val selectedDayOccasions = remember(viewedMonth, selectedDay) {
        PersianCalendarUtils.getOccasionsForDay(viewedMonth, selectedDay)
    }
    val monthOccasions = remember(viewedMonth) {
        PersianCalendarUtils.getOccasionsForMonth(viewedMonth)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TimeMate",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IceWhite
                    )
                    Text(
                        text = jalaliToday.formatFull(),
                        fontSize = 14.sp,
                        color = SoftPurple
                    )
                }

                FilterChip(
                    selected = isAnalogClock,
                    onClick = { isAnalogClock = !isAnalogClock },
                    label = {
                        Text(if (isAnalogClock) "ساعت دیجیتال" else "ساعت آنالوگ", fontSize = 12.sp)
                    }
                )
            }
        }

        // Live Clock Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = RoyalAzure.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isAnalogClock) {
                        GlassAnalogClock(calendar = currentTime)
                    } else {
                        val h = currentTime.get(Calendar.HOUR_OF_DAY)
                        val m = currentTime.get(Calendar.MINUTE)
                        val s = currentTime.get(Calendar.SECOND)
                        Text(
                            text = toPersianDigits("%02d:%02d:%02d".format(h, m, s)),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IceWhite
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val gToday = PersianCalendarUtils.jalaliToGregorian(jalaliToday.year, jalaliToday.month, jalaliToday.day)
                    val hToday = PersianCalendarUtils.gregorianToHijri(gToday.year, gToday.month, gToday.day)
                    Text(
                        text = "میلادی: ${gToday.formatNumeric()}  |  قمری: ${hToday.formatFull()}",
                        fontSize = 12.sp,
                        color = CoolSilver
                    )
                }
            }
        }

        // Quick Hourly "Ding-Ding" Chime Card (7 AM to 11 PM)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isChimeEnabled) NeonViolet.copy(alpha = 0.6f) else GlassBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "زنگ ساعتی «دینگ‌دینگ» (۷ صبح تا ۱۱ شب)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = IceWhite
                        )
                        Text(
                            text = "پخش زنگ کوتاه دو جزئی رأس هر ساعت",
                            fontSize = 12.sp,
                            color = CoolSilver
                        )
                    }
                    TextButton(onClick = { HourlyChimeSynthesizer.playDingDing() }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = SoftPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تست صدا", color = SoftPurple, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isChimeEnabled,
                        onCheckedChange = {
                            isChimeEnabled = it
                            HourlyChimeSynthesizer.setHourlyChimeEnabled(context, it)
                        }
                    )
                }
            }
        }

        // Full Interactive Shamsi Calendar with Holidays & Occasions
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (viewedMonth == 1) {
                                viewedMonth = 12
                                viewedYear -= 1
                            } else {
                                viewedMonth -= 1
                            }
                            selectedDay = 1
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل", tint = IceWhite)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${JalaliDate.MONTH_NAMES[viewedMonth - 1]} ${toPersianDigits(viewedYear)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = IceWhite,
                                fontSize = 17.sp
                            )
                            if (viewedMonth != jalaliToday.month || viewedYear != jalaliToday.year) {
                                Text(
                                    text = "بازگشت به امروز",
                                    color = NeonViolet,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable {
                                        viewedMonth = jalaliToday.month
                                        viewedYear = jalaliToday.year
                                        selectedDay = jalaliToday.day
                                    }
                                )
                            }
                        }

                        IconButton(onClick = {
                            if (viewedMonth == 12) {
                                viewedMonth = 1
                                viewedYear += 1
                            } else {
                                viewedMonth += 1
                            }
                            selectedDay = 1
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد", tint = IceWhite)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        JalaliDate.DAY_SHORT_NAMES.forEachIndexed { idx, d ->
                            Text(
                                text = d,
                                color = if (idx == 6) UrgentRed else SoftPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val daysInMonth = PersianCalendarUtils.getDaysInJalaliMonth(viewedYear, viewedMonth)
                    val startOffset = PersianCalendarUtils.getFirstDayOfWeekInMonth(viewedYear, viewedMonth)
                    val cells = List(startOffset) { null } + (1..daysInMonth).toList()
                    val rows = cells.chunked(7)

                    rows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val dayNum = week.getOrNull(col)
                                val isToday = dayNum == jalaliToday.day &&
                                        viewedMonth == jalaliToday.month &&
                                        viewedYear == jalaliToday.year
                                val isSelected = dayNum == selectedDay
                                val isHoliday = if (dayNum != null) {
                                    PersianCalendarUtils.isOfficialHoliday(viewedMonth, dayNum, col)
                                } else false

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isToday -> NeonViolet
                                                isSelected -> RoyalAzure.copy(alpha = 0.45f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = dayNum != null) {
                                            if (dayNum != null) selectedDay = dayNum
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (dayNum != null) toPersianDigits(dayNum) else "",
                                        color = when {
                                            isToday -> IceWhite
                                            isHoliday -> UrgentRed
                                            else -> IceWhite
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isToday || isHoliday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Divider(color = GlassBorder)
                    Spacer(Modifier.height(8.dp))

                    // Occasions of Selected Day
                    Text(
                        text = "مناسبت‌های ${toPersianDigits(selectedDay)} ${JalaliDate.MONTH_NAMES[viewedMonth - 1]}:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SoftPurple
                    )
                    if (selectedDayOccasions.isEmpty()) {
                        Text(
                            text = "مناسبت ثبت‌شده‌ای برای این روز وجود ندارد.",
                            fontSize = 12.sp,
                            color = CoolSilver
                        )
                    } else {
                        selectedDayOccasions.forEach { occ ->
                            Text(
                                text = "• ${occ.title}" + if (occ.isHoliday) " (تعطیل رسمی)" else "",
                                fontSize = 13.sp,
                                fontWeight = if (occ.isHoliday) FontWeight.Bold else FontWeight.Normal,
                                color = if (occ.isHoliday) UrgentRed else IceWhite
                            )
                        }
                    }

                    if (monthOccasions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "مهم‌ترین مناسبت‌ها و تعطیلات این ماه:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CoolSilver
                        )
                        monthOccasions.forEach { occ ->
                            Text(
                                text = "${toPersianDigits(occ.day)} ${JalaliDate.MONTH_NAMES[viewedMonth - 1]}: ${occ.title}" +
                                        if (occ.isHoliday) " [تعطیل]" else "",
                                fontSize = 11.sp,
                                color = if (occ.isHoliday) UrgentRed else CoolSilver
                            )
                        }
                    }
                }
            }
        }

        if (overdueTasks.isNotEmpty()) {
            item {
                Text(
                    text = "کارهای عقب‌افتاده (${toPersianDigits(overdueTasks.size)})",
                    color = UrgentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            items(overdueTasks) { item ->
                ReminderItemCard(item, onToggle = { viewModel.toggleReminderDone(item) }, onDelete = { viewModel.deleteReminder(item) })
            }
        }

        item {
            Text(
                text = "برنامه‌های امروز (${toPersianDigits(todayTasks.size)})",
                color = IceWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        if (todayTasks.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "برنامه باز برای امروز ثبت نشده است.",
                        color = CoolSilver,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(todayTasks) { item ->
                ReminderItemCard(item, onToggle = { viewModel.toggleReminderDone(item) }, onDelete = { viewModel.deleteReminder(item) })
            }
        }
    }
}

// ==========================================
// DATE CONVERTER & DAY DIFFERENCE SCREEN (Shamsi <-> Gregorian <-> Hijri)
// ==========================================
@Composable
fun DateConverterScreen() {
    val today = remember { PersianCalendarUtils.today() }
    var mode by remember { mutableIntStateOf(0) } // 0 = Shamsi to Gregorian/Hijri, 1 = Gregorian to Shamsi/Hijri

    var jYear by remember { mutableIntStateOf(today.year) }
    var jMonth by remember { mutableIntStateOf(today.month) }
    var jDay by remember { mutableIntStateOf(today.day) }

    val gNow = remember { Calendar.getInstance() }
    var gYear by remember { mutableIntStateOf(gNow.get(Calendar.YEAR)) }
    var gMonth by remember { mutableIntStateOf(gNow.get(Calendar.MONTH) + 1) }
    var gDay by remember { mutableIntStateOf(gNow.get(Calendar.DAY_OF_MONTH)) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "مبدل تاریخ و محاسبه‌گر زمان",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IceWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "تبدیل دقیق آفلاین بین تقویم خورشیدی (شمسی)، میلادی و قمری به همراه محاسبه فاصله زمانی و سن",
                fontSize = 13.sp,
                color = CoolSilver
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    label = { Text("شمسی به میلادی و قمری") }
                )
                FilterChip(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    label = { Text("میلادی به شمسی و قمری") }
                )
            }
        }

        if (mode == 0) {
            item {
                val maxDay = PersianCalendarUtils.getDaysInJalaliMonth(jYear, jMonth)
                if (jDay > maxDay) jDay = maxDay

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("انتخاب تاریخ شمسی:", fontWeight = FontWeight.Bold, color = SoftPurple)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateNumberStepper(
                                label = "روز",
                                value = jDay,
                                display = toPersianDigits(jDay),
                                onInc = { jDay = if (jDay >= maxDay) 1 else jDay + 1 },
                                onDec = { jDay = if (jDay <= 1) maxDay else jDay - 1 }
                            )
                            DateNumberStepper(
                                label = "ماه",
                                value = jMonth,
                                display = JalaliDate.MONTH_NAMES[jMonth - 1],
                                onInc = { jMonth = if (jMonth >= 12) 1 else jMonth + 1 },
                                onDec = { jMonth = if (jMonth <= 1) 12 else jMonth - 1 }
                            )
                            DateNumberStepper(
                                label = "سال",
                                value = jYear,
                                display = toPersianDigits(jYear),
                                onInc = { jYear += 1 },
                                onDec = { jYear = (jYear - 1).coerceAtLeast(1200) }
                            )
                        }
                    }
                }
            }

            item {
                val gOut = PersianCalendarUtils.jalaliToGregorian(jYear, jMonth, jDay)
                val jOut = PersianCalendarUtils.gregorianToJalali(gOut.year, gOut.month, gOut.day)
                val hOut = PersianCalendarUtils.gregorianToHijri(gOut.year, gOut.month, gOut.day)
                val diffDays = PersianCalendarUtils.daysBetweenTodayAnd(jYear, jMonth, jDay)
                val occasions = PersianCalendarUtils.getOccasionsForDay(jMonth, jDay)

                ConverterResultCard(
                    jalali = jOut,
                    gregorian = gOut,
                    hijri = hOut,
                    diffDays = diffDays,
                    occasions = occasions
                )
            }
        } else {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("انتخاب تاریخ میلادی:", fontWeight = FontWeight.Bold, color = SoftPurple)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateNumberStepper(
                                label = "Day",
                                value = gDay,
                                display = gDay.toString(),
                                onInc = { gDay = if (gDay >= 31) 1 else gDay + 1 },
                                onDec = { gDay = if (gDay <= 1) 31 else gDay - 1 }
                            )
                            DateNumberStepper(
                                label = "Month",
                                value = gMonth,
                                display = JalaliDate.GREGORIAN_MONTH_NAMES[gMonth - 1],
                                onInc = { gMonth = if (gMonth >= 12) 1 else gMonth + 1 },
                                onDec = { gMonth = if (gMonth <= 1) 12 else gMonth - 1 }
                            )
                            DateNumberStepper(
                                label = "Year",
                                value = gYear,
                                display = gYear.toString(),
                                onInc = { gYear += 1 },
                                onDec = { gYear = (gYear - 1).coerceAtLeast(1900) }
                            )
                        }
                    }
                }
            }

            item {
                val jOut = PersianCalendarUtils.gregorianToJalali(gYear, gMonth, gDay)
                val gOut = GregorianDate(gYear, gMonth, gDay)
                val hOut = PersianCalendarUtils.gregorianToHijri(gYear, gMonth, gDay)
                val diffDays = PersianCalendarUtils.daysBetweenTodayAnd(jOut.year, jOut.month, jOut.day)
                val occasions = PersianCalendarUtils.getOccasionsForDay(jOut.month, jOut.day)

                ConverterResultCard(
                    jalali = jOut,
                    gregorian = gOut,
                    hijri = hOut,
                    diffDays = diffDays,
                    occasions = occasions
                )
            }
        }
    }
}

@Composable
fun DateNumberStepper(
    label: String,
    value: Int,
    display: String,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = CoolSilver)
        IconButton(onClick = onInc) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = IceWhite)
        }
        Text(display, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IceWhite)
        IconButton(onClick = onDec) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = IceWhite)
        }
    }
}

@Composable
fun ConverterResultCard(
    jalali: JalaliDate,
    gregorian: GregorianDate,
    hijri: HijriDate,
    diffDays: Long,
    occasions: List<PersianOccasion>
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NeonViolet.copy(alpha = 0.6f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "نتیجه تبدیل تاریخ",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = IceWhite
            )
            Divider(color = GlassBorder)

            Text(
                text = "خورشیدی (شمسی): ${jalali.formatFull()} (${toPersianDigits(jalali.formatNumeric())})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = IceWhite
            )
            Text(
                text = "میلادی: ${gregorian.day} ${gregorian.monthName} ${gregorian.year} (${gregorian.formatNumeric()})",
                fontSize = 14.sp,
                color = SoftPurple
            )
            Text(
                text = "قمری: ${hijri.formatFull()}",
                fontSize = 14.sp,
                color = Color(0xFF60A5FA)
            )
            Text(
                text = "برج فلکی: ${JalaliDate.ZODIAC_NAMES.getOrElse(jalali.month - 1) { "" }}",
                fontSize = 13.sp,
                color = CoolSilver
            )

            val diffText = when {
                diffDays == 0L -> "این تاریخ دقیقاً امروز است."
                diffDays > 0L -> "${toPersianDigits(diffDays)} روز مانده تا این تاریخ"
                else -> {
                    val pastDays = abs(diffDays)
                    val years = pastDays / 365
                    val remDays = pastDays % 365
                    "${toPersianDigits(pastDays)} روز از این تاریخ گذشته است (معادل حدود ${toPersianDigits(years)} سال و ${toPersianDigits(remDays)} روز)"
                }
            }
            Text(
                text = "فاصله زمانی: $diffText",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldSuccess
            )

            if (occasions.isNotEmpty()) {
                Divider(color = GlassBorder)
                Text("مناسبت این روز:", fontSize = 12.sp, color = SoftPurple, fontWeight = FontWeight.Bold)
                occasions.forEach { occ ->
                    Text(
                        text = "• ${occ.title}" + if (occ.isHoliday) " (تعطیل رسمی)" else "",
                        fontSize = 13.sp,
                        color = if (occ.isHoliday) UrgentRed else IceWhite
                    )
                }
            }
        }
    }
}

// ==========================================
// WIDGETS GALLERY SCREEN (All 4 Flat 2D Minimal Persian Widgets)
// ==========================================
@Composable
fun WidgetsGalleryScreen() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000L)
        }
    }

    val today = remember(currentTime.get(Calendar.DAY_OF_YEAR)) { PersianCalendarUtils.today() }
    val clockStr = toPersianDigits(
        "%02d:%02d".format(
            currentTime.get(Calendar.HOUR_OF_DAY),
            currentTime.get(Calendar.MINUTE)
        )
    )
    val dayMonthStr = "${toPersianDigits(today.day)} ${today.monthName}"
    val yearStr = toPersianDigits(today.year)
    val weekdayStr = today.dayOfWeekName

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "ویجت‌های مینیمال و تخت (۲ بعدی)",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IceWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "هر ۴ مدل ویجت ساعت و تقویم شمسی فعال هستند. با زدن دکمه «افزودن به صفحه اصلی»، ویجت مستقیماً روی هوم‌اسکرین گوشی قرار می‌گیرد.",
                fontSize = 13.sp,
                color = CoolSilver
            )
        }

        item {
            WidgetPreviewContainer(
                title = "ویجت ۱ — افقی استاندارد",
                onAddClick = { BaseMinimalWidgetProvider.requestPinWidget(context, 1) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF141C32), Color(0xFF24193D))))
                        .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonViolet.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(yearStr, fontSize = 11.sp, color = IceWhite)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(dayMonthStr, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                        Text(weekdayStr, fontSize = 14.sp, color = SoftPurple)
                    }

                    Text(
                        text = clockStr,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IceWhite,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        item {
            WidgetPreviewContainer(
                title = "ویجت ۲ — بزرگ و خوانا",
                onAddClick = { BaseMinimalWidgetProvider.requestPinWidget(context, 2) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF141C32), Color(0xFF24193D))))
                        .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(dayMonthStr, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = IceWhite)
                        Text(weekdayStr, fontSize = 16.sp, color = CoolSilver)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(yearStr, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                        Text(clockStr, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                    }
                }
            }
        }

        item {
            WidgetPreviewContainer(
                title = "ویجت ۳ — مربعی جمع‌وجور",
                onAddClick = { BaseMinimalWidgetProvider.requestPinWidget(context, 3) }
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .size(165.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF141C32), Color(0xFF24193D))))
                            .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(weekdayStr, fontSize = 16.sp, color = CoolSilver)
                        Spacer(Modifier.height(6.dp))
                        Text("$dayMonthStr $yearStr", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                        Spacer(Modifier.height(6.dp))
                        Text(clockStr, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SoftPurple)
                    }
                }
            }
        }

        item {
            WidgetPreviewContainer(
                title = "ویجت ۴ — نواری کشیده و فوق‌مینیمال",
                onAddClick = { BaseMinimalWidgetProvider.requestPinWidget(context, 4) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF141C32), Color(0xFF24193D))))
                        .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clockStr,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IceWhite
                    )
                    Text(
                        text = "$weekdayStr، $dayMonthStr $yearStr",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoolSilver
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewContainer(
    title: String,
    onAddClick: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 15.sp)
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalAzure),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("افزودن به صفحه اصلی", fontSize = 12.sp)
                }
            }
            previewContent()
        }
    }
}

@Composable
fun GlassAnalogClock(calendar: Calendar) {
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)

    Canvas(modifier = Modifier.size(160.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = RoyalAzure.copy(alpha = 0.25f),
            radius = radius,
            center = center
        )
        drawCircle(
            color = NeonViolet,
            radius = radius,
            center = center,
            style = Stroke(width = 4f)
        )

        val hourAngle = Math.toRadians(((hour + minute / 60.0) * 30.0) - 90.0)
        drawLine(
            color = IceWhite,
            start = center,
            end = Offset(
                center.x + (radius * 0.5f * cos(hourAngle)).toFloat(),
                center.y + (radius * 0.5f * sin(hourAngle)).toFloat()
            ),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )

        val minAngle = Math.toRadians((minute * 6.0) - 90.0)
        drawLine(
            color = SoftPurple,
            start = center,
            end = Offset(
                center.x + (radius * 0.72f * cos(minAngle)).toFloat(),
                center.y + (radius * 0.72f * sin(minAngle)).toFloat()
            ),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        val secAngle = Math.toRadians((second * 6.0) - 90.0)
        drawLine(
            color = RoyalAzure,
            start = center,
            end = Offset(
                center.x + (radius * 0.85f * cos(secAngle)).toFloat(),
                center.y + (radius * 0.85f * sin(secAngle)).toFloat()
            ),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

// ==========================================
// 2. ALARMS SCREEN
// ==========================================
@Composable
fun AlarmsScreen(alarms: List<AlarmEntity>, viewModel: TimeMateViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            item {
                Text(
                    text = "مدیریت بیدارباش هوشمند",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = IceWhite
                )
            }

            if (alarms.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "هنوز زنگ بیدارباشی ثبت نکرده‌اید. دکمه + پایین صفحه را لمس کنید.",
                            color = CoolSilver,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            items(alarms, key = { it.id }) { alarm ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (alarm.isEnabled) NeonViolet.copy(alpha = 0.6f) else GlassBorder
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = toPersianDigits("%02d:%02d".format(alarm.hour, alarm.minute)),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (alarm.isEnabled) IceWhite else CoolSilver
                                )
                                Text(
                                    text = alarm.title,
                                    fontSize = 14.sp,
                                    color = SoftPurple
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { viewModel.toggleAlarm(alarm, it) }
                                )
                                IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = CoolSilver)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        val daysText = if (alarm.daysOfWeek.isEmpty()) {
                            "یک‌بار"
                        } else if (alarm.daysOfWeek.size == 7) {
                            "هر روز"
                        } else {
                            alarm.daysOfWeek.sorted().joinToString("، ") { JalaliDate.DAY_NAMES[it] }
                        }

                        Text(
                            text = "تکرار: $daysText | ${if (alarm.isGradualVolume) "افزایش تدریجی صدا" else "صدای ثابت"} | ${if (alarm.isVibrate) "لرزش فعال" else "بدون لرزش"}",
                            fontSize = 12.sp,
                            color = CoolSilver
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = NeonViolet,
            contentColor = IceWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.AddAlarm, contentDescription = "افزودن زنگ")
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onSave = { h, m, title, days, vibrate, gradual ->
                viewModel.addAlarm(h, m, title, days, vibrate, gradual)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, List<Int>, Boolean, Boolean) -> Unit
) {
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("بیدارباش صبحگاهی") }
    val selectedDays = remember { mutableStateListOf<Int>() }
    var isVibrate by remember { mutableStateOf(true) }
    var isGradual by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightBlue,
        title = { Text("تنظیم زنگ جدید", color = IceWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The time picker must always read hour (left) : minute (right),
                // even though the rest of the screen is RTL (Persian).
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, null, tint = IceWhite)
                            }
                            Text(toPersianDigits("%02d".format(hour)), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                            IconButton(onClick = { hour = if (hour - 1 < 0) 23 else hour - 1 }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = IceWhite)
                            }
                        }
                        Text(":", fontSize = 28.sp, color = NeonViolet, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { minute = (minute + 1) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowUp, null, tint = IceWhite)
                            }
                            Text(toPersianDigits("%02d".format(minute)), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                            IconButton(onClick = { minute = if (minute - 1 < 0) 59 else minute - 1 }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = IceWhite)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان زنگ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("روزهای تکرار:", fontSize = 13.sp, color = CoolSilver)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    JalaliDate.DAY_SHORT_NAMES.forEachIndexed { idx, label ->
                        val isSelected = idx in selectedDays
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) RoyalAzure else GlassSurface)
                                .clickable {
                                    if (isSelected) selectedDays.remove(idx) else selectedDays.add(idx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = IceWhite, fontSize = 12.sp)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isGradual, onCheckedChange = { isGradual = it })
                    Text("افزایش تدریجی صدای زنگ", color = IceWhite, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isVibrate, onCheckedChange = { isVibrate = it })
                    Text("لرزش همزمان با زنگ", color = IceWhite, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(hour, minute, title, selectedDays.toList(), isVibrate, isGradual) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalAzure)
            ) {
                Text("ذخیره زنگ", color = IceWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = CoolSilver)
            }
        }
    )
}

// ==========================================
// 3. REMINDERS SCREEN
// ==========================================
@Composable
fun RemindersScreen(reminders: List<ReminderEntity>, viewModel: TimeMateViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            item {
                Text(
                    text = "یادآورهای هوشمند و برنامه‌ها",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = IceWhite
                )
            }

            if (reminders.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "یادآوری ثبت نشده است. با دکمه پایین صفحه یادآور جدید بسازید.",
                            color = CoolSilver,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            items(reminders, key = { it.id }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    onToggle = { viewModel.toggleReminderDone(reminder) },
                    onDelete = { viewModel.deleteReminder(reminder) }
                )
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = RoyalAzure,
            contentColor = IceWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.AddTask, contentDescription = "یادآور جدید")
        }
    }

    if (showDialog) {
        AddReminderDialog(
            onDismiss = { showDialog = false },
            onSave = { title, desc, priority, repeatMode, hour, minute, autoRepeat ->
                val todayStr = PersianCalendarUtils.today().formatNumeric()
                viewModel.addReminder(title, desc, todayStr, hour, minute, priority, repeatMode, autoRepeat)
                showDialog = false
            }
        )
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (reminder.priority) {
        1 -> UrgentRed
        2 -> NeonViolet
        else -> RoyalAzure
    }
    val priorityLabel = when (reminder.priority) {
        1 -> "فوری"
        2 -> "مهم"
        else -> "عادی"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = priorityColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = reminder.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Column {
                    Text(
                        text = reminder.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isCompleted) CoolSilver else IceWhite,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (reminder.description.isNotBlank()) {
                        Text(text = reminder.description, fontSize = 12.sp, color = CoolSilver)
                    }
                    Text(
                        text = "ساعت ${toPersianDigits("%02d:%02d".format(reminder.hour, reminder.minute))} | اولویت: $priorityLabel | ${toPersianDigits(reminder.persianDate)}",
                        fontSize = 11.sp,
                        color = SoftPurple
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = CoolSilver)
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, Int, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(2) }
    var repeatMode by remember { mutableStateOf("DAILY") }
    var hour by remember { mutableIntStateOf(10) }
    var minute by remember { mutableIntStateOf(30) }
    var autoRepeat by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightBlue,
        title = { Text("ثبت یادآور هوشمند", color = IceWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان کار") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("زمان یادآوری:", color = IceWhite, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { hour = (hour + 1) % 24 }) { Text("+ساعت") }
                        Text(toPersianDigits("%02d:%02d".format(hour, minute)), color = NeonViolet, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { minute = (minute + 15) % 60 }) { Text("+دقیقه") }
                    }
                }

                Text("اولویت:", color = CoolSilver, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "فوری", 2 to "مهم", 3 to "عادی").forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoRepeat, onCheckedChange = { autoRepeat = it })
                    Text("تکرار خودکار در صورت انجام نشدن", color = IceWhite, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, desc, priority, repeatMode, hour, minute, autoRepeat)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalAzure)
            ) {
                Text("ثبت یادآور", color = IceWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = CoolSilver) }
        }
    )
}

// ==========================================
// 5. SETTINGS & HOURLY CHIME + XIAOMI MIUI RELIABILITY GUARD
// ==========================================
@Composable
fun SettingsScreen(viewModel: TimeMateViewModel, onOpenXiaomiGuide: () -> Unit = {}) {
    val context = LocalContext.current
    var isChimeEnabled by remember {
        mutableStateOf(HourlyChimeSynthesizer.isHourlyChimeEnabled(context))
    }
    var selectedSnoozeMinutes by remember {
        mutableIntStateOf(SnoozePreferences.getMinutes(context))
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "تنظیمات و پایداری سیستم",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = IceWhite
            )
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonViolet
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "مدت زمان چرت زدن",
                        fontWeight = FontWeight.Bold,
                        color = IceWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "مدت تعویق زنگ برای همه بیدارباش‌ها",
                        fontSize = 12.sp,
                        color = CoolSilver
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15).forEach { minutes ->
                            FilterChip(
                                selected = selectedSnoozeMinutes == minutes,
                                onClick = {
                                    selectedSnoozeMinutes = minutes
                                    SnoozePreferences.setMinutes(context, minutes)
                                },
                                label = { Text("${toPersianDigits(minutes.toString())} دقیقه") }
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = RoyalAzure
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "زنگ کوتاه سر ساعت (دینگ‌دینگ)",
                                fontWeight = FontWeight.Bold,
                                color = IceWhite,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "پخش خودکار زنگ دو جزئی رأس هر ساعت از ساعت ۷:۰۰ صبح تا ۲۳:۰۰ شب",
                                fontSize = 12.sp,
                                color = CoolSilver
                            )
                        }
                        Switch(
                            checked = isChimeEnabled,
                            onCheckedChange = {
                                isChimeEnabled = it
                                HourlyChimeSynthesizer.setHourlyChimeEnabled(context, it)
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { HourlyChimeSynthesizer.playDingDing() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SoftPurple)
                        Spacer(Modifier.width(8.dp))
                        Text("پخش آزمایشی صدای دینگ‌دینگ", color = IceWhite)
                    }
                }
            }
        }

        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonViolet
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "راهنمای پایداری زنگ در MIUI و HyperOS",
                            fontWeight = FontWeight.Bold,
                            color = IceWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "تنظیمات شروع خودکار، محدودیت باتری و مجوز زنگ دقیق را هر زمان خواستید دوباره بررسی کنید.",
                            fontSize = 13.sp,
                            color = CoolSilver
                        )
                        Button(
                            onClick = onOpenXiaomiGuide,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalAzure)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("باز کردن راهنمای تنظیمات شیائومی")
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "پشتیبان‌گیری آفلاین (JSON)",
                        fontWeight = FontWeight.Bold,
                        color = IceWhite,
                        fontSize = 16.sp
                    )
                    OutlinedButton(
                        onClick = {
                            val json = viewModel.exportBackupJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("TimeMateBackup", json))
                            Toast.makeText(context, "فایل پشتیبان JSON در کلیپ‌بورد کپی شد", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = IceWhite)
                        Spacer(Modifier.width(8.dp))
                        Text("کپی نسخه پشتیبان کامل (JSON)", color = IceWhite)
                    }
                }
            }
        }
    }
}
