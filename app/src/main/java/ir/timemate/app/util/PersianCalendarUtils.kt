package ir.timemate.app.util

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.floor

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val dayOfWeek: Int // 0 = Shanbeh ... 6 = Jomeh
) {
    val monthName: String
        get() = MONTH_NAMES.getOrElse(month - 1) { "" }

    val dayOfWeekName: String
        get() = DAY_NAMES.getOrElse(dayOfWeek) { "" }

    fun formatFull(): String = "$dayOfWeekName، ${toPersianDigits(day)} $monthName ${toPersianDigits(year)}"
    fun formatNumeric(): String = "${year}/%02d/%02d".format(month, day)

    companion object {
        val MONTH_NAMES = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        val GREGORIAN_MONTH_NAMES = listOf(
            "ژانویه (Jan)", "فوریه (Feb)", "مارس (Mar)", "آوریل (Apr)",
            "مه (May)", "ژوئن (Jun)", "ژوئیه (Jul)", "اوت (Aug)",
            "سپتامبر (Sep)", "اکتبر (Oct)", "نوامبر (Nov)", "دسامبر (Dec)"
        )
        val HIJRI_MONTH_NAMES = listOf(
            "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی",
            "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
        )
        val ZODIAC_NAMES = listOf(
            "حمل (فروردین)", "ثور (اردیبهشت)", "جوزا (خرداد)", "سرطان (تیر)",
            "اسد (مرداد)", "سنبله (شهریور)", "میزان (مهر)", "عقرب (آبان)",
            "قوس (آذر)", "جدی (دی)", "دلو (بهمن)", "حوت (اسفند)"
        )
        val DAY_NAMES = listOf(
            "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
        )
        val DAY_SHORT_NAMES = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    }
}

data class GregorianDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String
        get() = JalaliDate.GREGORIAN_MONTH_NAMES.getOrElse(month - 1) { "" }
    fun formatNumeric(): String = "%04d/%02d/%02d".format(year, month, day)
}

data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String
        get() = JalaliDate.HIJRI_MONTH_NAMES.getOrElse(month - 1) { "" }
    fun formatFull(): String = "${toPersianDigits(day)} $monthName ${toPersianDigits(year)} هـ.ق"
}

data class PersianOccasion(
    val month: Int,
    val day: Int,
    val title: String,
    val isHoliday: Boolean
)

object PersianCalendarUtils {

    private val occasions = listOf(
        // فروردین
        PersianOccasion(1, 1, "جشن نوروز و آغاز سال نو", true),
        PersianOccasion(1, 2, "عید نوروز", true),
        PersianOccasion(1, 3, "عید نوروز", true),
        PersianOccasion(1, 4, "عید نوروز", true),
        PersianOccasion(1, 6, "زادروز زرتشت / روز امید و شادباش‌نویسی", false),
        PersianOccasion(1, 12, "روز جمهوری اسلامی", true),
        PersianOccasion(1, 13, "جشن سیزده‌به‌در (روز طبیعت)", true),
        PersianOccasion(1, 25, "روز بزرگداشت عطار نیشابوری", false),
        // اردیبهشت
        PersianOccasion(2, 1, "روز بزرگداشت سعدی", false),
        PersianOccasion(2, 3, "روز بزرگداشت شیخ بهایی / روز معماری", false),
        PersianOccasion(2, 10, "روز ملی خلیج فارس", false),
        PersianOccasion(2, 12, "روز معلم", false),
        PersianOccasion(2, 25, "روز پاسداشت زبان فارسی و بزرگداشت فردوسی", false),
        PersianOccasion(2, 28, "روز بزرگداشت حکیم عمر خیام", false),
        // خرداد
        PersianOccasion(3, 14, "رحلت آیت‌الله خمینی", true),
        PersianOccasion(3, 15, "قیام ۱۵ خرداد", true),
        PersianOccasion(3, 20, "روز جهانی صنایع دستی", false),
        // تیر
        PersianOccasion(4, 1, "جشن آب‌پاشونک / آغاز تابستان", false),
        PersianOccasion(4, 13, "جشن تیرگان (بزرگداشت آرش کمانگیر)", false),
        PersianOccasion(4, 14, "روز قلم", false),
        // مرداد
        PersianOccasion(5, 7, "جشن امردادگان", false),
        PersianOccasion(5, 14, "سالروز صدور فرمان مشروطیت", false),
        PersianOccasion(5, 17, "روز خبرنگار", false),
        PersianOccasion(5, 28, "سالروز کودتای ۲۸ مرداد", false),
        // شهریور
        PersianOccasion(6, 1, "روز بزرگداشت ابوعلی سینا و روز پزشک", false),
        PersianOccasion(6, 4, "جشن شهریورگان / روز کارمند", false),
        PersianOccasion(6, 5, "روز بزرگداشت محمد بن زکریای رازی و روز داروساز", false),
        PersianOccasion(6, 13, "روز بزرگداشت ابوریحان بیرونی", false),
        PersianOccasion(6, 21, "روز سینما", false),
        PersianOccasion(6, 27, "روز شعر و ادب فارسی و بزرگداشت شهریار", false),
        // مهر
        PersianOccasion(7, 1, "آغاز سال تحصیلی و فصل پاییز", false),
        PersianOccasion(7, 8, "روز بزرگداشت مولوی", false),
        PersianOccasion(7, 16, "جشن مهرگان / روز جهانی کودک", false),
        PersianOccasion(7, 20, "روز بزرگداشت حافظ شیرازی", false),
        // آبان
        PersianOccasion(8, 7, "روز بزرگداشت کوروش بزرگ", false),
        PersianOccasion(8, 10, "جشن آبانگان", false),
        PersianOccasion(8, 13, "روز دانش‌آموز", false),
        PersianOccasion(8, 24, "روز کتاب و کتاب‌خوانی", false),
        // آذر
        PersianOccasion(9, 1, "جشن آذرگان", false),
        PersianOccasion(9, 9, "جشن آذرگان باستانی", false),
        PersianOccasion(9, 16, "روز دانشجو", false),
        PersianOccasion(9, 25, "روز پژوهش", false),
        PersianOccasion(9, 30, "جشن شب یلدا (شب چله)", false),
        // دی
        PersianOccasion(10, 1, "آغاز زمستان و میلاد خورشید", false),
        PersianOccasion(10, 5, "روز ملی ایمنی در برابر زلزله", false),
        PersianOccasion(10, 15, "جشن دیگان", false),
        // بهمن
        PersianOccasion(11, 2, "جشن بهمنگان", false),
        PersianOccasion(11, 10, "جشن سده", false),
        PersianOccasion(11, 22, "پیروزی انقلاب اسلامی", true),
        // اسفند
        PersianOccasion(12, 5, "جشن سپندارمذگان (روز عشق ایرانی) و بزرگداشت خواجه نصیرالدین طوسی (روز مهندس)", false),
        PersianOccasion(12, 15, "روز درختکاری", false),
        PersianOccasion(12, 29, "روز ملی شدن صنعت نفت ایران", true),
        PersianOccasion(12, 30, "آخرین روز سال کبیسه", true)
    )

    fun getOccasionsForDay(month: Int, day: Int): List<PersianOccasion> {
        return occasions.filter { it.month == month && it.day == day }
    }

    fun getOccasionsForMonth(month: Int): List<PersianOccasion> {
        return occasions.filter { it.month == month }.sortedBy { it.day }
    }

    fun isOfficialHoliday(month: Int, day: Int, dayOfWeek: Int = -1): Boolean {
        if (dayOfWeek == 6) return true // Jomeh
        return occasions.any { it.month == month && it.day == day && it.isHoliday }
    }

    fun today(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.DAY_OF_WEEK)
        )
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int, javaDayOfWeek: Int = -1): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd
        for (i in 0 until gm) {
            days += gDaysInMonth[i]
        }
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }

        val resolvedDow = if (javaDayOfWeek != -1) {
            mapJavaDayOfWeek(javaDayOfWeek)
        } else {
            val cal = Calendar.getInstance().apply {
                set(gy, (gm - 1).coerceIn(0, 11), gd.coerceIn(1, 31))
            }
            mapJavaDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
        }
        return JalaliDate(jy, jm, jd, resolvedDow)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): GregorianDate {
        val jy1 = jy + 1595
        var days = -355668 + (365 * jy1) + ((jy1 / 33) * 8) + (((jy1 % 33) + 3) / 4) + jd +
                if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(
            0, 31,
            if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return GregorianDate(gy, gm, gd)
    }

    // Kuwaiti / Astronomical Tabular Islamic Hijri Conversion
    fun gregorianToHijri(gy: Int, gm: Int, gd: Int): HijriDate {
        var y = gy
        var m = gm
        val d = gd
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524

        val iyear = 10631.0 / 30.0
        val epochAstro = 1948084.0
        val shift1 = 8.01 / 60.0

        var z = jd - epochAstro
        val cyc = floor(z / 10631.0)
        z -= 10631 * cyc
        val j = floor((z - shift1) / iyear)
        val iy = (30 * cyc + j).toInt()
        z -= floor(j * iyear + shift1)
        var im = floor((z + 28.5001) / 29.5).toInt()
        if (im == 13) im = 12
        val id = (z - floor(29.5001 * im - 29)).toInt().coerceIn(1, 30)
        return HijriDate(iy, im.coerceIn(1, 12), id)
    }

    fun getFirstDayOfWeekInMonth(jy: Int, jm: Int): Int {
        val g = jalaliToGregorian(jy, jm, 1)
        val cal = Calendar.getInstance().apply {
            set(g.year, g.month - 1, g.day)
        }
        return mapJavaDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun daysBetweenTodayAnd(jy: Int, jm: Int, jd: Int): Long {
        val gTarget = jalaliToGregorian(jy, jm, jd)
        val targetCal = Calendar.getInstance().apply {
            set(gTarget.year, gTarget.month - 1, gTarget.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
        return diffMillis / (24 * 60 * 60 * 1000L)
    }

    private fun mapJavaDayOfWeek(javaDayOfWeek: Int): Int {
        return when (javaDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun isLeapJalaliYear(jy: Int): Boolean {
        val rem = (jy + 1595) % 33
        return rem in listOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isLeapJalaliYear(year)) 30 else 29
            else -> 30
        }
    }
}

fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())
fun toPersianDigits(number: Long): String = toPersianDigits(number.toString())

fun toPersianDigits(input: String): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val builder = StringBuilder()
    for (ch in input) {
        if (ch in '0'..'9') {
            builder.append(persianDigits[ch - '0'])
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}
