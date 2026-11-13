package ir.timemate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import ir.timemate.app.data.model.AlarmHistoryEntity
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.onSurfaceVariant
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.tertiary
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.outlineVariant
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.onSurface
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.secondary
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.primary
import ir.timemate.app.ui.theme.MaterialTheme.colorScheme.tertiary
import ir.timemate.app.ui.theme.GlassCard
import ir.timemate.app.util.PersianCalendarUtils
import ir.timemate.app.util.toPersianDigits
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun AlarmStatisticsScreen(viewModel: TimeMateViewModel) {
    var selectedDays by remember { androidx.compose.runtime.mutableIntStateOf(7) }
    val statsFlow = remember(selectedDays) { viewModel.observeAlarmHistoryStats(selectedDays) }
    val stats by statsFlow.collectAsState(initial = AlarmHistoryStats())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("آمار و تحلیل بیدارباش", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("بر پایه تاریخچه ثبت‌شده روی دستگاه؛ بدون تحلیل یا تشخیص پزشکی خواب", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedDays == 7,
                    onClick = { selectedDays = 7 },
                    label = { Text("۷ روز اخیر") }
                )
                FilterChip(
                    selected = selectedDays == 30,
                    onClick = { selectedDays = 30 },
                    label = { Text("۳۰ روز اخیر") }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "تعداد بیدارباش",
                    value = toPersianDigits(stats.totalAlarms.toString()),
                    icon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "میانگین چرت",
                    value = toPersianDigits("%.1f".format(stats.averageSnoozes)),
                    icon = { Icon(Icons.Default.Snooze, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
                )
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("میانگین زمان تا خاموشی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = stats.averageDismissMinutes?.let {
                            "${toPersianDigits(it.roundToInt().toString())} دقیقه"
                        } ?: "هنوز داده کافی نیست",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بر اساس ${toPersianDigits(stats.completedCount.toString())} جلسه تکمیل‌شده",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(19.dp))
                        Text("روند روزانه بیدارباش", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                    if (stats.totalAlarms == 0) {
                        Text("در این بازه هنوز بیدارباشی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        val maxCount = (stats.dailyCounts.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(112.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (selectedDays == 7) 8.dp else 2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            stats.dailyCounts.forEachIndexed { index, day ->
                                Column(
                                    modifier = Modifier.weight(1f).height(112.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = if (day.count > 0) toPersianDigits(day.count.toString()) else "",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 8.sp,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (day.count == 0) 3.dp else (62f * day.count / maxCount).coerceAtLeast(6f).dp)
                                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                            .background(if (day.count == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    if (selectedDays == 7 || index % 5 == 0 || index == stats.dailyCounts.lastIndex) {
                                        Text(
                                            text = toPersianDigits(Calendar.getInstance().apply { timeInMillis = day.dayStartMillis }.get(Calendar.DAY_OF_MONTH).toString()),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 8.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("سوابق بیدارباش", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        if (stats.records.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(34.dp))
                        Text("هنوز سابقه‌ای ثبت نشده است", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text("پس از اولین بیدارباش، گزارش آن در این بخش نمایش داده می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(stats.records, key = { it.sessionId }) { record ->
                AlarmHistoryCard(record)
            }
        }
        item {
            Text(
                "این آمار فقط رفتار ثبت‌شده بیدارباش را توصیف می‌کند و معیاری برای کیفیت یا مراحل خواب نیست.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit
) {
    GlassCard(modifier = modifier, borderColor = MaterialTheme.colorScheme.outlineVariant) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AlarmHistoryCard(record: AlarmHistoryEntity) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatPersianDate(record.startedAtMillis), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    if (record.endedAtMillis == null) "فعال / ناقص" else "تکمیل‌شده",
                    color = if (record.endedAtMillis == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary,
                    fontSize = 10.sp
                )
            }
            Text(
                "زمان برنامه‌ریزی‌شده: ${formatTime(record.scheduledAtMillis)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                "شروع: ${formatTime(record.startedAtMillis)}  •  خاموشی: ${record.endedAtMillis?.let(::formatTime) ?: "ثبت نشده"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("چرت: ${toPersianDigits(record.snoozeCount.coerceAtLeast(0).toString())} بار", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
                record.durationMillis?.let {
                    Text("مدت: ${toPersianDigits((it / 60_000L).toString())} دقیقه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatPersianDate(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val jalali = PersianCalendarUtils.gregorianToJalali(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    return "${toPersianDigits(jalali.formatNumeric())} • ${formatTime(timestamp)}"
}

private fun formatTime(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return toPersianDigits("%02d:%02d".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)))
}
