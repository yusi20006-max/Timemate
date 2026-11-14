package ir.timemate.app.prayer

import com.google.gson.Gson
import java.time.LocalDate
import java.util.Locale

private data class BundledPrayerTimesAsset(
    val schemaVersion: Int = 0,
    val cityId: String = "",
    val persianYear: Int = 0,
    val timeZone: String = "",
    val startDate: String = "",
    val eventOrder: List<String> = emptyList(),
    val source: BundledPrayerTimesSource = BundledPrayerTimesSource(),
    val days: List<String> = emptyList()
)

private data class BundledPrayerTimesSource(
    val publisher: String = "",
    val document: String = "",
    val filename: String = "",
    val sha256: String = ""
)

/**
 * Decodes the compact, versioned JSON generated from the official University of Tehran PDF.
 * Each day stores six minute-of-day integers in the declared eventOrder.
 */
object BundledPrayerTimesParser {
    private val expectedEvents = listOf("fajr", "sunrise", "dhuhr", "sunset", "maghrib", "midnight")

    fun parse(json: String, city: PrayerCity): List<PrayerTimesDay> {
        if (city.id != "tehran") return emptyList()
        val asset = runCatching {
            Gson().fromJson(json, BundledPrayerTimesAsset::class.java)
        }.getOrNull() ?: return emptyList()

        if (asset.schemaVersion != 1 ||
            asset.cityId != "tehran" ||
            asset.persianYear != 1405 ||
            asset.timeZone != "Asia/Tehran" ||
            asset.startDate != "2026-03-21" ||
            asset.eventOrder != expectedEvents ||
            asset.days.size != 365 ||
            asset.source.filename != "Tehran1405.pdf" ||
            !asset.source.sha256.matches(Regex("[0-9a-f]{64}"))
        ) return emptyList()

        val start = runCatching { LocalDate.parse(asset.startDate) }.getOrNull() ?: return emptyList()
        val result = asset.days.mapIndexed { index, encoded ->
            val minutes = encoded.split(',')
            if (minutes.size != 6) return emptyList()
            val values = minutes.map { it.toIntOrNull() ?: return emptyList() }
            if (values.any { it !in 0 until 1440 } || values.zipWithNext().any { (a, b) -> b <= a }) {
                return emptyList()
            }
            fun format(value: Int): String =
                "%02d:%02d".format(Locale.ROOT, value / 60, value % 60)

            PrayerTimesDay(
                city = city,
                dateIso = start.plusDays(index.toLong()).toString(),
                fajr = format(values[0]),
                sunrise = format(values[1]),
                dhuhr = format(values[2]),
                sunset = format(values[3]),
                maghrib = format(values[4]),
                midnight = format(values[5]),
                sourceUrl = PrayerTimesSource.OFFICIAL_PAGE
            )
        }
        if (result.size != 365 || result.any { !it.hasValidTimes() || !it.hasOfficialSource() }) return emptyList()
        if (result.zipWithNext().any { (a, b) ->
                LocalDate.parse(b.dateIso) != LocalDate.parse(a.dateIso).plusDays(1)
            }
        ) return emptyList()
        return result
    }
}
