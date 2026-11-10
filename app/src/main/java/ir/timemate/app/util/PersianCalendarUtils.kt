package ir.timemate.app.util

import java.util.Calendar

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
        val DAY_NAMES = listOf(
            "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
        )
        val DAY_SHORT_NAMES = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    }
}

object PersianCalendarUtils {

    fun today(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.DAY_OF_WEEK)
        )
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int, javaDayOfWeek: Int = Calendar.SATURDAY): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gy2 = if (gm > 2) gy + 1 else gy
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
        // Map Calendar.SATURDAY (7) -> 0, SUNDAY (1) -> 1, ..., FRIDAY (6) -> 6
        val shamsiDow = when (javaDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
        return JalaliDate(jy, jm, jd, shamsiDow)
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
