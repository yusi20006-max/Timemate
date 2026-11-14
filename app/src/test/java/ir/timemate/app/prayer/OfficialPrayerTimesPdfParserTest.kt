package ir.timemate.app.prayer

import ir.timemate.app.util.JalaliDate
import ir.timemate.app.util.PersianCalendarUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialPrayerTimesPdfParserTest {
    private val city = PrayerCities.provincialCapitals.first { it.id == "tehran" }
    private val source = "https://calendar.ut.ac.ir/documents/official/Tehran1405.pdf"

    private fun completeYearPages(): List<String> {
        val monthDays = listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        return monthDays.mapIndexed { index, count ->
            buildString {
                appendLine("اوقات شرعی در تهران ${JalaliDate.MONTH_NAMES[index]} سال 1405")
                repeat(count) { day ->
                    appendLine("23:30 18:30 18:00 12:00 06:00 05:00 شنبه ${day + 1}")
                }
            }
        }
    }

    @Test fun parsesTwelveMonthlyPagesAndMapsPdfColumns() {
        val days = OfficialPrayerTimesPdfParser.parsePages(completeYearPages(), city, source)
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

    @Test fun matchesPublishedTehran1405FarvardinFirstDayRow() {
        val pages = completeYearPages().toMutableList()
        pages[0] = pages[0].replaceFirst(
            "23:30 18:30 18:00 12:00 06:00 05:00 شنبه 1",
            "23:29 18:35 18:17 12:12 06:07 04:43 شنبه 1"
        )
        val first = OfficialPrayerTimesPdfParser.parsePages(pages, city, source).first()
        assertEquals("04:43", first.fajr)
        assertEquals("06:07", first.sunrise)
        assertEquals("12:12", first.dhuhr)
        assertEquals("18:17", first.sunset)
        assertEquals("18:35", first.maghrib)
        assertEquals("23:29", first.midnight)
    }

    @Test fun rejectsNonUniversityPdfUrl() {
        assertTrue(OfficialPrayerTimesPdfParser.parsePages(completeYearPages(), city, "https://example.com/Tehran1405.pdf").isEmpty())
    }

    @Test fun rejectsIncompleteAnnualCoverage() {
        val pages = completeYearPages().toMutableList()
        pages[11] = pages[11].substringBeforeLast("23:30")
        assertTrue(OfficialPrayerTimesPdfParser.parsePages(pages, city, source).isEmpty())
    }

    @Test fun acceptsOfficialPdfWhenYearIsInContentNotFilename() {
        val nonstandardOfficialFilename = "https://calendar.ut.ac.ir/documents/prayers/tehran.pdf"
        assertEquals(365, OfficialPrayerTimesPdfParser.parsePages(
            completeYearPages(), city, nonstandardOfficialFilename
        ).size)
    }

}
