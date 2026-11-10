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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.timemate.app.data.model.AlarmEntity
import ir.timemate.app.data.model.HabitEntity
import ir.timemate.app.data.model.ReminderEntity
import ir.timemate.app.ui.TimeMateViewModel
import ir.timemate.app.ui.theme.*
import ir.timemate.app.util.JalaliDate
import ir.timemate.app.util.PersianCalendarUtils
import ir.timemate.app.util.XiaomiPermissionHelper
import ir.timemate.app.util.toPersianDigits
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: TimeMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val alarms by viewModel.alarms.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val habits by viewModel.habits.collectAsState()

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
                    3 -> HabitsReportScreen(habits, reminders, viewModel)
                    4 -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun GlassBottomNavigation(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    val items = listOf(
        Triple("خانه", Icons.Default.Home, 0),
        Triple("بیدارباش", Icons.Default.Alarm, 1),
        Triple("یادآورها", Icons.Default.NotificationsActive, 2),
        Triple("گزارش‌ها", Icons.Default.Insights, 3),
        Triple("تنظیمات", Icons.Default.Settings, 4)
    )

    NavigationBar(
        containerColor = MidnightBlue.copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp))
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
                        fontSize = 11.sp,
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
// 1. HOME SCREEN (Digital/Analog Clock + Shamsi Calendar + Today's Tasks + Overdue)
// ==========================================
@Composable
fun HomeScreen(
    alarms: List<AlarmEntity>,
    reminders: List<ReminderEntity>,
    viewModel: TimeMateViewModel
) {
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
    val todayNumeric = jalaliToday.formatNumeric()
    val nowMillis = currentTime.timeInMillis

    val todayTasks = reminders.filter { it.persianDate == todayNumeric && !it.isCompleted }
    val overdueTasks = reminders.filter { !it.isCompleted && it.targetTimeMillis < nowMillis }

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

        // Live Glass Clock Card
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
                    Spacer(Modifier.height(8.dp))
                    val enabledCount = alarms.count { it.isEnabled }
                    Text(
                        text = if (enabledCount > 0)
                            "${toPersianDigits(enabledCount)} زنگ بیدارباش فعال است"
                        else
                            "هیچ زنگ فعالی تنظیم نشده است",
                        fontSize = 13.sp,
                        color = CoolSilver
                    )
                }
            }
        }

        // Persian Month Mini-Grid
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "تقویم شمسی — ${jalaliToday.monthName} ${toPersianDigits(jalaliToday.year)}",
                        fontWeight = FontWeight.Bold,
                        color = IceWhite,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        JalaliDate.DAY_SHORT_NAMES.forEach { d ->
                            Text(text = d, color = SoftPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val daysInMonth = PersianCalendarUtils.getDaysInJalaliMonth(jalaliToday.year, jalaliToday.month)
                    val rows = (1..daysInMonth).chunked(7)
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (i in 0 until 7) {
                                val dayNum = week.getOrNull(i)
                                val isToday = dayNum == jalaliToday.day
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) NeonViolet else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (dayNum != null) toPersianDigits(dayNum) else "",
                                        color = if (isToday) IceWhite else CoolSilver,
                                        fontSize = 13.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overdue Tasks Section
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

        // Today's Schedule Section
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
                        text = "برنامه باز برای امروز ثبت نشده است. از تب یادآورها برنامه جدیدی اضافه کنید.",
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

        // Hour hand
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

        // Minute hand
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

        // Second hand
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
// 2. ALARMS SCREEN (Multiple Alarms, Days of Week, Gradual Volume, Vibrate)
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
                        IconButton(onClick = { minute = (minute + 5) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = IceWhite)
                        }
                        Text(toPersianDigits("%02d".format(minute)), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                        IconButton(onClick = { minute = if (minute - 5 < 0) 55 else minute - 5 }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = IceWhite)
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
// 3. REMINDERS SCREEN (Smart Reminders with Priority & Auto-Repeat)
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
// 4. HABITS & REPORTS SCREEN
// ==========================================
@Composable
fun HabitsReportScreen(
    habits: List<HabitEntity>,
    reminders: List<ReminderEntity>,
    viewModel: TimeMateViewModel
) {
    var newHabitTitle by remember { mutableStateOf("") }
    val todayNumeric = PersianCalendarUtils.today().formatNumeric()
    val completedTasks = reminders.count { it.isCompleted }
    val totalTasks = reminders.size

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "گزارش عملکرد و پیگیری عادت‌ها",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = IceWhite
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(toPersianDigits(completedTasks), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        Text("کارهای انجام‌شده", fontSize = 12.sp, color = CoolSilver)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(toPersianDigits(totalTasks - completedTasks), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeonViolet)
                        Text("کارهای باقی‌مانده", fontSize = 12.sp, color = CoolSilver)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(toPersianDigits(habits.size), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = RoyalAzure)
                        Text("عادت‌های فعال", fontSize = 12.sp, color = CoolSilver)
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newHabitTitle,
                        onValueChange = { newHabitTitle = it },
                        label = { Text("عادت روزانه جدید (مثلاً مطالعه، ورزش)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.addHabit(newHabitTitle)
                            newHabitTitle = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                    ) {
                        Text("افزودن")
                    }
                }
            }
        }

        items(habits, key = { it.id }) { habit ->
            val doneToday = habit.lastCompletedPersianDate == todayNumeric
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(habit.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                        Text(
                            text = "زنجیره فعلی: ${toPersianDigits(habit.currentStreak)} روز | رکورد: ${toPersianDigits(habit.bestStreak)} روز",
                            fontSize = 12.sp,
                            color = SoftPurple
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { viewModel.checkInHabitToday(habit) },
                            enabled = !doneToday,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (doneToday) EmeraldSuccess else RoyalAzure
                            )
                        ) {
                            Text(if (doneToday) "امروز ثبت شد" else "ثبت امروز", fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.deleteHabit(habit) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CoolSilver)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. SETTINGS & XIAOMI MIUI RELIABILITY GUARD
// ==========================================
@Composable
fun SettingsScreen(viewModel: TimeMateViewModel) {
    val context = LocalContext.current
    val exactAlarmGranted = remember { XiaomiPermissionHelper.canScheduleExactAlarms(context) }
    val batteryIgnored = remember { XiaomiPermissionHelper.isIgnoringBatteryOptimizations(context) }

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

        // Redmi Note 12 / Xiaomi MIUI Special Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonViolet
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تضمین اجرای زنگ در شیائومی (Redmi / MIUI / HyperOS)",
                        fontWeight = FontWeight.Bold,
                        color = IceWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "برای اینکه زنگ ساعت حتی در زمان بسته بودن کامل برنامه یا خاموش و روشن شدن گوشی بدون خطا کار کند، این ۳ مجوز کلیدی را فعال کنید:",
                        fontSize = 13.sp,
                        color = CoolSilver
                    )

                    Button(
                        onClick = { XiaomiPermissionHelper.openXiaomiAutostartSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalAzure),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("۱. فعال‌سازی شروع خودکار (Autostart شیائومی)")
                    }

                    Button(
                        onClick = { XiaomiPermissionHelper.requestIgnoreBatteryOptimizations(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (batteryIgnored) EmeraldSuccess else RoyalAzure
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (batteryIgnored)
                                "۲. محدودیت باتری غیرفعال است (تأیید شده)"
                            else
                                "۲. حذف محدودیت باتری (No Restrictions)"
                        )
                    }

                    Button(
                        onClick = { XiaomiPermissionHelper.openExactAlarmSettings(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (exactAlarmGranted) EmeraldSuccess else NeonViolet
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (exactAlarmGranted)
                                "۳. مجوز آلارم دقیق فعال است (Exact Alarm)"
                            else
                                "۳. اعطای مجوز آلارم دقیق (Exact Alarm)"
                        )
                    }
                }
            }
        }

        // Offline Backup Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "پشتیبان‌گیری آفلاین (JSON)",
                        fontWeight = FontWeight.Bold,
                        color = IceWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "تمام زنگ‌ها، یادآورها و عادت‌ها کاملاً آفلاین در حافظه دستگاه ذخیره می‌شوند.",
                        fontSize = 13.sp,
                        color = CoolSilver
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
