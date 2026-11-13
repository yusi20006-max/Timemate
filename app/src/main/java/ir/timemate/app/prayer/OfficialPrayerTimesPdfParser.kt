package ir.timemate.app.prayer

import ir.timemate.app.util.JalaliDate
import ir.timemate.app.util.PersianCalendarUtils
import java.util.Locale\nimport java.time.LocalDate

/**
 * Parses the official 1405 city PDF text. Column order in the source PDF is:
 * midnight, maghrib, sunset, dhuhr, sunrise, fajr, weekday, Jalali day.
 */
object OfficialPrayerTimesPdfParser {
    private val timeRegex = Regex("""(?<!\\d)([0-2]?\\d):([0-5]\\d)(?!\\d)""")
    private val trailingDayRegex = Regex("""(\\d{1,2})\\s*$""")

    fun parseText(text: String, city: PrayerCity, sourceUrl: String): List<PrayerTimesDay> {
        if (!sourceUrl.startsWith("https://calendar.ut.ac.ir/")) return emptyList()

        var currentMonth = 0
        val parsed = linkedMapOf<Pair<Int, Int>, PrayerTimesDay>()
        text.lineSequence().forEach { rawLine ->
            val line = normalize(rawLine)
            if (line.contains("1405")) {
                val month = JalaliDate.MONTH_NAMES.indexOfFirst { line.contains(normalize(it)) }
                if (month >= 0) currentMonth = month + 1
            }
            if (currentMonth !in 1..12) return@forEach

            val times = timeRegex.findAll(line).map { match ->
                val hour = match.groupValues[1].toIntOrNull() ?: return@map ""
                val minute = match.groupValues[2].toIntOrNull() ?: return@map ""
                "%02d:%02d".format(Locale.ROOT, hour, minute)
            }.toList()
            if (times.size != 6 || times.any { it.isBlank() }) return@forEach

            val day = trailingDayRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@forEach
            if (day !in 1..31) return@forEach

            val gregorian = runCatching { PersianCalendarUtils.jalaliToGregorian(1405, currentMonth, day) }
                .getOrNull() ?: return@forEach
            if (gregorian.month !in 1..12 || gregorian.day !in 1..31) return@forEach
            val dateIso = "%04d-%02d-%02d".format(Locale.ROOT, gregorian.year, gregorian.month, gregorian.day)

            val record = PrayerTimesDay(
                city = city,
                dateIso = dateIso,
                fajr = times[5],
                sunrise = times[4],
                dhuhr = times[3],
                sunset = times[2],
                maghrib = times[1],
                midnight = times[0],
                sourceUrl = sourceUrl
            )
            if (record.hasValidTimes() && record.hasOfficialSource()) {
                parsed[currentMonth to day] = record
            }
        }

        val months = (1..12).all { month -> parsed.keys.any { it.first == month && it.second == 1 } }
        if (!months || parsed.size !in 365..366) return emptyList()
        return parsed.entries.sortedWith(compareBy({ it.key.first }, { it.key.second })).map { it.value }
    }

    private fun normalize(value: String): String = value
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3')
        .replace('۴', '4').replace('۵', '5').replace('۶', '6').replace('۷', '7')
        .replace('۸', '8').replace('۹', '9')
        .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
        .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
        .replace('٨', '8').replace('٩', '9')
        .replace('ي', 'ی').replace('ك', 'ک')
        .replace("\u200c", "")
        .replace(" ", "")
}
