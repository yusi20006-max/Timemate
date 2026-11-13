package ir.timemate.app.prayer
object PrayerTimesSource {
 const val DISPLAY_NAME = "دانشگاه تهران، مؤسسه ژئوفیزیک / مرکز تقویم"
 const val OFFICIAL_PAGE = "https://calendar.ut.ac.ir/Fa/Prayers/IrCenters.asp"
 const val APPROVED_LANDING_PAGE = "https://calendar.ut.ac.ir/"
 const val SOURCE_VERIFIED_FOR_CURRENT_YEAR = false
}
data class PrayerCity(val id:String,val title:String,val timeZoneId:String="Asia/Tehran")
object PrayerCities {
 val provincialCapitals = listOf(
 PrayerCity("tehran","تهران"),PrayerCity("arak","اراک"),PrayerCity("ardabil","اردبیل"),
 PrayerCity("urmia","ارومیه"),PrayerCity("isfahan","اصفهان"),PrayerCity("ahvaz","اهواز"),
 PrayerCity("ilam","ایلام"),PrayerCity("bojnurd","بجنورد"),PrayerCity("bandar-abbas","بندرعباس"),
 PrayerCity("bushehr","بوشهر"),PrayerCity("birjand","بیرجند"),PrayerCity("tabriz","تبریز"),
 PrayerCity("shahr-e-kord","شهرکرد"),PrayerCity("rasht","رشت"),PrayerCity("zahedan","زاهدان"),
 PrayerCity("zanjan","زنجان"),PrayerCity("semnan","سمنان"),PrayerCity("sanandaj","سنندج"),
 PrayerCity("shiraz","شیراز"),PrayerCity("ghazvin","قزوین"),PrayerCity("qom","قم"),
 PrayerCity("kerman","کرمان"),PrayerCity("kermanshah","کرمانشاه"),PrayerCity("yasuj","یاسوج"),
 PrayerCity("gorgan","گرگان"),PrayerCity("mashhad","مشهد"),PrayerCity("hamedan","همدان"),
 PrayerCity("yazd","یزد"),PrayerCity("khorramabad","خرم‌آباد"),PrayerCity("sari","ساری"),PrayerCity("karaj","کرج")
 )
}
sealed interface PrayerTimesState {
 data object Loading:PrayerTimesState
 data class Available(val day:PrayerTimesDay,val fetchedAtMillis:Long):PrayerTimesState
 data class Unavailable(val reason:String):PrayerTimesState
}
data class PrayerTimesDay(
 val city:PrayerCity,val dateIso:String,val fajr:String,val sunrise:String,val dhuhr:String,
 val sunset:String,val maghrib:String,val midnight:String,
 val sourceUrl:String=PrayerTimesSource.OFFICIAL_PAGE
) {
 fun hasValidTimes():Boolean = listOf(fajr,sunrise,dhuhr,sunset,maghrib,midnight)
  .all { it.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$")) }
}
/** Fails closed until current-year official data is extracted and verified. */
class OfficialPrayerTimesRepository {
 fun currentState():PrayerTimesState = PrayerTimesState.Unavailable(
  "داده رسمی سال جاری هنوز از صفحه دانشگاه تهران استخراج و اعتبارسنجی نشده است."
 )
}
