package ir.timemate.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.timemate.app.util.DeviceSetupManufacturer
import ir.timemate.app.util.XiaomiPermissionHelper

@Composable
fun DeviceManufacturerPickerDialog(
    onSelect: (DeviceSetupManufacturer) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("گوشی شما از چه برندی است؟", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("برای نمایش راهنمای درستِ مجوزها و تنظیمات باتری، نوع گوشی را انتخاب کنید.")
                DeviceSetupManufacturer.values().forEach { manufacturer ->
                    OutlinedButton(onClick = { onSelect(manufacturer) }, modifier = Modifier.fillMaxWidth()) {
                        Text(manufacturer.title)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("فعلاً رد کردن") } }
    )
}

@Composable
fun ManufacturerSetupGuideDialog(
    manufacturer: DeviceSetupManufacturer,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }
    val samsung = manufacturer == DeviceSetupManufacturer.SAMSUNG
    val title = if (samsung) "تنظیم اولیه برای Samsung One UI" else "تنظیم اولیه Android"
    val intro = if (samsung) {
        "در گوشی‌های Samsung، محدودیت‌های پس‌زمینه و حالت‌های صرفه‌جویی باتری ممکن است اجرای زنگ را به تأخیر بیندازند. گزینه‌ها را خودتان بررسی و فعال کنید."
    } else {
        "در برخی گوشی‌های Android، بهینه‌سازی باتری یا مجوز زنگ دقیق می‌تواند بر زمان‌بندی بیدارباش اثر بگذارد. این راهنما مسیرهای عمومی سیستم را باز می‌کند."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(intro, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("۱. مجوز زنگ دقیق", fontWeight = FontWeight.SemiBold)
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        "در صفحه Alarms & reminders، اجازه زمان‌بندی دقیق را برای TimeMate فعال کنید."
                    else "نسخه Android شما به مجوز جداگانه زنگ دقیق نیاز ندارد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(
                        onClick = {
                            settingsLauncher.launch(XiaomiPermissionHelper.exactAlarmSettingsIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("باز کردن تنظیمات زنگ دقیق") }
                }
                Text("۲. محدودیت باتری و اجرای پس‌زمینه", fontWeight = FontWeight.SemiBold)
                Text(
                    if (samsung)
                        "در Settings > Apps > TimeMate > Battery، حالت محدودکننده را غیرفعال کنید. سپس در Device care > Battery > Background usage limits، در صورت وجود، TimeMate را به Never sleeping apps اضافه کنید. نام مسیرها ممکن است با نسخه One UI فرق کند."
                    else
                        "در Settings > Apps > TimeMate > Battery، در صورت وجود گزینه Unrestricted را انتخاب کنید. نام و محل این گزینه به سازنده گوشی و نسخه Android بستگی دارد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        settingsLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("باز کردن تنظیمات برنامه TimeMate") }
                Text(
                    "وضعیت گزینه‌های اختصاصی سازنده از طریق Android قابل تأیید قطعی نیست؛ پس از بازگشت، آن‌ها را دستی بررسی کنید.",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("پایان راهنما") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بعداً") }
        }
    )
}
