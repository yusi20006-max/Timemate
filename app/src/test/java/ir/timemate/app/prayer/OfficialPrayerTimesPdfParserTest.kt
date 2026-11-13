package ir.timemate.app.prayer

import ir.timemate.app.util.JalaliDate
import ir.timemate.app.util.PersianCalendarUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialPrayerTimesPdfParserTest {
    private val city = PrayerCities.provincialCapitals.first { it.id == "tehran" }
    private val source = "https://calendar.ut.ac.ir/documents/official/Tehran1405.pdf"

    private fun completeYearText(): String = buildString {
        val monthDays = listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        monthDays.forEachIndexed { index, count ->
            appendLine("اوقات شرعی در تهران فروردین 1405".replace("فروردین", JalaliDate.MONTH_NAMES[index]))
            repeat(count) { day ->
                appendLine("23:30 18:30 18:00 12:00 06:00 05:00 شنبه ${day + 1}")
            }
        }
    }

    @Test fun parsesFullOfficialYearAndMapsPdfColumns() {
        val days = OfficialPrayerTimesPdfParser.parseText(completeYearText(), city, source)
        assertEquals(365, days.size)
        val first = days.first()
        val expected = PersianCalendarUtils.jalaliToGregorian(1405, 1, 1)
        assertEquals("%04d-%02d-%02d".format(expected.year, expected.month, expected.day), first.dateIso)
        assertEquals("05:00", first.fajr)
        assertEquals("06:00", first.sunrise)
        assertEquals("12:00", first.dhuhr)
        assertEquals("18:00", first.sunset)
        assertEquals("18:30", first.maghrib)
        assertEquals("23:30", first.midnight)
    }

    @Test fun rejectsNonUniversityPdfUrl() {
        assertTrue(OfficialPrayerTimesPdfParser.parseText(completeYearText(), city, "https://example.com/timetable.pdf").isEmpty())
    }

    @Test fun rejectsIncompleteAnnualCoverage() {
        val incomplete = completeYearText().lineSequence().take(100).joinToString("\n")
        assertTrue(OfficialPrayerTimesPdfParser.parseText(incomplete, city, source).isEmpty())
    }
}
