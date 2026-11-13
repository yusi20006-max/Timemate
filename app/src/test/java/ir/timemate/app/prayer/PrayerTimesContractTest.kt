package ir.timemate.app.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerTimesContractTest {
    private val city = PrayerCity("tehran", "تهران")

    private fun day(date: String = "2026-11-14") = PrayerTimesDay(
        city, date, "05:00", "06:00", "12:00", "17:00", "17:30", "23:00"
    )

    @Test fun acceptsValidTimeShape() {
        assertTrue(day().hasValidTimes())
    }

    @Test fun rejectsInvalidTime() {
        assertFalse(day().copy(fajr = "25:12").hasValidTimes())
    }

    @Test fun rejectsChronologicallyInvalidTimes() {
        assertFalse(day().copy(sunrise = "04:30").hasValidTimes())
    }

    @Test fun selectsNextPrayerInSameDay() {
        val now = ZonedDateTime.of(2026, 11, 14, 10, 0, 0, 0, ZoneId.of("Asia/Tehran"))
        val next = PrayerTimesEngine.nextPrayer(day(), null, now)
        assertNotNull(next)
        assertEquals(PrayerEvent.DHUHR, next?.event)
        assertEquals(2L, next?.remaining?.toHours())
    }

    @Test fun rollsOverToNextDayFajrAfterLastPrayer() {
        val today = ZonedDateTime.of(2026, 11, 14, 23, 10, 0, 0, ZoneId.of("Asia/Tehran"))
        val next = PrayerTimesEngine.nextPrayer(day(), day("2026-11-15"), today)
        assertNotNull(next)
        assertEquals(PrayerEvent.FAJR, next?.event)
        assertEquals("2026-11-15", next?.at?.toLocalDate().toString())
    }

    @Test fun refusesCountdownWhenNextDayIsMissing() {
        val now = ZonedDateTime.of(2026, 11, 14, 23, 10, 0, 0, ZoneId.of("Asia/Tehran"))
        assertEquals(null, PrayerTimesEngine.nextPrayer(day(), null, now))
    }

    @Test fun formatsCountdownWithPersianDigits() {
        assertEquals("۰۱:۰۲:۰۳", PrayerTimesEngine.formatCountdown(java.time.Duration.ofSeconds(3723)))
    }
}
