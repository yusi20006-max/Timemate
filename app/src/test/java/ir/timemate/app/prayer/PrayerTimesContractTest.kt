package ir.timemate.app.prayer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class PrayerTimesContractTest{
 private val city=PrayerCity("tehran","تهران")
 @Test fun acceptsValidTimeShape(){assertTrue(PrayerTimesDay(city,"2026-11-14","05:12","06:38","11:52","16:58","17:16","23:40").hasValidTimes())}
 @Test fun rejectsInvalidTime(){assertFalse(PrayerTimesDay(city,"2026-11-14","25:12","06:38","11:52","16:58","17:16","23:40").hasValidTimes())}
 @Test fun repositoryFailsClosedWithoutVerifiedFeed(){assertTrue(OfficialPrayerTimesRepository().currentState() is PrayerTimesState.Unavailable)}
}
