package ir.timemate.app.prayer

import ir.timemate.app.util.PersianCalendarUtils
import java.net.URI
import java.time.LocalDate
import java.util.Locale

/**
 * The official 1405 PDFs contain one Jalali month per page.
 * The six time columns may be extracted in either visual direction by PDF text engines.
 */
object OfficialPrayerTimesPdfParser {
    private val timeRegex = Regex("""(?<!\d)([0-2]?\d):([0-5]\d)(?!\d)""")
    private val trailingDayRegex = Regex("""(\d{1,2})\s*$""")
    private val leadingDayRegex = Regex("""^\s*(\d{1,2})\s+""")

    fun parsePages(pages: List<String>, city: PrayerCity, sourceUrl: String): List<PrayerTimesDay> {
        val uri = runCatching { URI(sourceUrl) }.getOrNull() ?: return emptyList()
        if (uri.scheme != "https" || uri.host != "calendar.ut.ac.ir" ||
            !uri.path.contains(".pdf", ignoreCase = true) || pages.size != 12 ||
            pages.none { normalize(it).contains("1405") }
        ) return emptyList()

        val parsed = linkedMapOf<Pair<Int, Int>, PrayerTimesDay>()
        pages.forEachIndexed { pageIndex, pageText ->
            val month = pageIndex + 1
            pageText.lineSequence().forEach { rawLine ->
                val line = normalize(rawLine)
                val times = timeRegex.findAll(line).map { match ->
                    val hour = match.groupValues[1].toIntOrNull() ?: return@map ""
                    val minute = match.groupValues[2].toIntOrNull() ?: return@map ""
                    "%02d:%02d".format(Locale.ROOT, hour, minute)
                }.toList()
                if (times.size != 6 || times.any { it.isBlank() }) return@forEach

                val day = leadingDayRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: trailingDayRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return@forEach
                if (day !in 1..31) return@forEach

                val ascendingTimes = if (times.first() > times.last()) times.reversed() else times
                val gregorian = runCatching { PersianCalendarUtils.jalaliToGregorian(1405, month, day) }
                    .getOrNull() ?: return@forEach
                if (gregorian.month !in 1..12 || gregorian.day !in 1..31) return@forEach
                val dateIso = "%04d-%02d-%02d".format(Locale.ROOT, gregorian.year, gregorian.month, gregorian.day)

                val record = PrayerTimesDay(
                    city = city,
                    dateIso = dateIso,
                    fajr = ascendingTimes[0],
                    sunrise = ascendingTimes[1],
                    dhuhr = ascendingTimes[2],
                    sunset = ascendingTimes[3],
                    maghrib = ascendingTimes[4],
                    midnight = ascendingTimes[5],
                    sourceUrl = sourceUrl
                )
                if (record.hasValidTimes() && record.hasOfficialSource()) {
                    parsed[month to day] = record
                }
            }
        }

        if (parsed.size !in 365..366) return emptyList()
        if ((1..12).any { month -> parsed.keys.none { it.first == month && it.second == 1 } }) return emptyList()

        val ordered = parsed.entries
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .map { it.value }
        val dates = ordered.mapNotNull { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
        if (dates.size != ordered.size || dates.zipWithNext().any { (a, b) -> b != a.plusDays(1) }) return emptyList()
        return ordered
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
        .replace("\u200e", "")
        .replace("\u200f", "")
}
