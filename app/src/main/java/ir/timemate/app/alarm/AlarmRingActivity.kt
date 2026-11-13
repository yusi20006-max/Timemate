package ir.timemate.app.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.timemate.app.ui.theme.*
import ir.timemate.app.ui.theme.ThemeDataStore
import ir.timemate.app.ui.theme.AppTheme
import ir.timemate.app.TimeMateApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ir.timemate.app.util.toPersianDigits
import java.util.Calendar
import ir.timemate.app.alarm.SnoozePreferences
import ir.timemate.app.alarm.SnoozeSessionStore

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguardOff()

        val alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        val title = intent.getStringExtra(AlarmReceiver.EXTRA_TITLE) ?: "بیدارباش"
        val isVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_VIBRATE, true)
        val isGradual = intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_GRADUAL, true)
        val ringtoneUri = intent.getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI)
        val snoozeMinutes = SnoozePreferences.getMinutes(this)
        val sessionId = intent.getStringExtra(AlarmReceiver.EXTRA_SNOOZE_SESSION_ID).orEmpty()
        val initialSnoozeCount = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, 0)

        setContent {
            var showThirdSnoozeWarning by remember { mutableStateOf(false) }

            fun confirmSnooze() {
                val nextCount = initialSnoozeCount + 1
                val scheduled = AlarmScheduler.scheduleSnooze(
                    this@AlarmRingActivity,
                    alarmId,
                    title.removeSuffix(" (چرت)"),
                    snoozeMinutes,
                    isVibrate,
                    isGradual,
                    ringtoneUri,
                    sessionId,
                    nextCount
                )
                if (scheduled) {
                    if (sessionId.isNotBlank()) {
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            (application as TimeMateApp).database.alarmHistoryDao()
                                .recordSnooze(sessionId, nextCount, System.currentTimeMillis())
                        }
                    }
                    stopAlarmAndFinish(clearSession = false)
                } else {
                    Toast.makeText(
                        this@AlarmRingActivity,
                        "زمان‌بندی چرت انجام نشد؛ زنگ همچنان فعال است.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            val selectedTheme by ThemeDataStore.observe(this@AlarmRingActivity).collectAsState(initial = AppTheme.DEEP_OCEAN)
            TimeMateTheme(selectedTheme) {
                val cal = Calendar.getInstance()
                val timeStr = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f), MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.background)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .scale(scale)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toPersianDigits(timeStr),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (initialSnoozeCount == 2) {
                                    showThirdSnoozeWarning = true
                                } else {
                                    confirmSnooze()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "چرت زدن (${toPersianDigits(snoozeMinutes)} دقیقه)",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = { stopAlarmAndFinish() },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Icon(Icons.Default.AlarmOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(10.dp))
                            Text(text = "بیدار شدم (قطع کامل زنگ)", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            if (showThirdSnoozeWarning) {
                AlertDialog(
                    onDismissRequest = { showThirdSnoozeWarning = false },
                    title = { Text("هشدار تکرار چرت", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "این سومین بار است که زمان بیدارباش را عقب می‌اندازید. اگر ادامه دهید، زنگ دوباره پس از $snoozeMinutes دقیقه پخش می‌شود.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showThirdSnoozeWarning = false
                            confirmSnooze()
                        }) {
                            Text("ادامه چرت", color = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showThirdSnoozeWarning = false }) {
                            Text("برگشت به زنگ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }

    private fun stopAlarmAndFinish(clearSession: Boolean = true) {
        val alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        val sessionId = intent.getStringExtra(AlarmReceiver.EXTRA_SNOOZE_SESSION_ID).orEmpty()
        if (clearSession && alarmId != -1 && sessionId.isNotBlank()) {
            AlarmScheduler.cancelSnooze(this, alarmId, sessionId)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                (application as TimeMateApp).database.alarmHistoryDao()
                    .finishSession(sessionId, System.currentTimeMillis())
            }
        }
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)
        finish()
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
