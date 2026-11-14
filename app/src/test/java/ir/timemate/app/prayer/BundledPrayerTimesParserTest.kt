package ir.timemate.app.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledPrayerTimesParserTest {
    private val city = PrayerCities.provincialCapitals.single()

    private fun fixture(firstDay: String = "283,367,732,1097,1115,1409", count: Int = 365): String {
        val rows = (0 until count).map { index ->
            if (index == 0) firstDay else "300,360,720,1080,1110,1380"
        }
        return """
            {
              "schemaVersion": 1,
              "cityId": "tehran",
              "persianYear": 1405,
              "timeZone": "Asia/Tehran",
              "startDate": "2026-03-21",
              "eventOrder": ["fajr", "sunrise", "dhuhr", "sunset", "maghrib", "midnight"],
              "source": {
                "publisher": "Geophysics Institute, University of Tehran",
                "document": "Tehran prayer times 1405",
                "filename": "Tehran1405.pdf",
                "sha256": "2035e940740b28f5f2722274c64b1fff6bcf9892de77fcd3f662a027effa3540"
              },
              "days": [${rows.joinToString(",") { "\"$it\"" }}]
            }
        """.trimIndent()
    }

    @Test fun parsesCompleteOfficialTehranYearAndMapsFirstPdfRow() {
        val days = BundledPrayerTimesParser.parse(fixture(), city)
        assertEquals(365, days.size)
        assertEquals("2026-03-21", days.first().dateIso)
        assertEquals("2027-03-20", days.last().dateIso)
        assertEquals("04:43", days.first().fajr)
        assertEquals("06:07", days.first().sunrise)
        assertEquals("12:12", days.first().dhuhr)
        assertEquals("18:17", days.first().sunset)
        assertEquals("18:35", days.first().maghrib)
        assertEquals("23:29", days.first().midnight)
        assertTrue(days.all { it.hasValidTimes() && it.hasOfficialSource() })
    }

    @Test fun rejectsIncompleteOrInvalidAnnualData() {
        assertTrue(BundledPrayerTimesParser.parse(fixture(count = 364), city).isEmpty())
        assertTrue(BundledPrayerTimesParser.parse(fixture("900,367,732,1097,1115,1409"), city).isEmpty())
    }

    @Test fun rejectsNonTehranCity() {
        assertTrue(BundledPrayerTimesParser.parse(fixture(), PrayerCity("mashhad", "مشهد")).isEmpty())
    }
}
