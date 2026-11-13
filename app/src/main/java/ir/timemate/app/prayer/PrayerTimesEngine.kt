package ir.timemate.app.prayer

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class PrayerEvent(val label: String, val time: String) {
    FAJR("اذان صبح", "fajr"),
    SUNRISE("طلوع آفتاب", "sunrise"),
    DHUHR("اذان ظهر", "dhuhr"),
    SUNSET("غروب آفتاب", "sunset"),
    MAGHRIB("اذان مغرب", "maghrib"),
    MIDNIGHT("نیمه‌شب شرعی", "midnight")
}

data class NextPrayer(
    val event: PrayerEvent,
    val at: ZonedDateTime,
    val remaining: Duration
)

object PrayerTimesEngine {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun nextPrayer(
        today: PrayerTimesDay,
        tomorrow: PrayerTimesDay?,
        now: ZonedDateTime
    ): NextPrayer? {
        val zone = runCatching { ZoneId.of(today.city.timeZoneId) }.getOrDefault(now.zone)
        val localNow = now.withZoneSameInstant(zone)
        val todayDate = runCatching { LocalDate.parse(today.dateIso) }.getOrNull() ?: return null
        if (localNow.toLocalDate() != todayDate) return null

        val todayEvents = events(today).mapNotNull { (event, value) ->
            parseTime(value)?.let { event to ZonedDateTime.of(todayDate, it, zone) }
        }
        val candidate = todayEvents.firstOrNull { it.second.isAfter(localNow) }
        if (candidate != null) {
            return NextPrayer(candidate.first, candidate.second, Duration.between(localNow, candidate.second))
        }

        val tomorrowDate = todayDate.plusDays(1)
        if (tomorrow == null || tomorrow.city.id != today.city.id ||
            runCatching { LocalDate.parse(tomorrow.dateIso) }.getOrNull() != tomorrowDate
        ) return null

        val nextFajr = parseTime(tomorrow.fajr) ?: return null
        val at = ZonedDateTime.of(tomorrowDate, nextFajr, zone)
        return NextPrayer(PrayerEvent.FAJR, at, Duration.between(localNow, at))
    }

    fun formatCountdown(duration: Duration): String {
        if (duration.isNegative) return "۰۰:۰۰:۰۰"
        val seconds = duration.seconds
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainder = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, remainder)
            .mapDigitsToPersian()
    }

    fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value, timeFormatter) }.getOrNull()

    private fun events(day: PrayerTimesDay): List<Pair<PrayerEvent, String>> = listOf(
        PrayerEvent.FAJR to day.fajr,
        PrayerEvent.SUNRISE to day.sunrise,
        PrayerEvent.DHUHR to day.dhuhr,
        PrayerEvent.SUNSET to day.sunset,
        PrayerEvent.MAGHRIB to day.maghrib,
        PrayerEvent.MIDNIGHT to day.midnight
    )

    private fun String.mapDigitsToPersian(): String =
        map { if (it in '0'..'9') ('۰'.code + (it.code - '0'.code)).toChar() else it }
}
