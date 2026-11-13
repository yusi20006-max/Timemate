package ir.timemate.app.prayer
object PrayerTimesSource {
 const val DISPLAY_NAME = "دانشگاه تهران، مؤسسه ژئوفیزیک / مرکز تقویم"
 const val OFFICIAL_PAGE = "https://calendar.ut.ac.ir/%D8%A7%D9%88%D9%82%D8%A7%D8%AA-%D8%B4%D8%B1%D8%B9%D9%8A-%D9%85%D8%B1%D8%A7%DA%A9%D8%B2-%D8%A7%D8%B3%D8%AA%D8%A7%D9%86%D9%87%D8%A7/-/asset_publisher/lvuu/content/%25D8%25A7%25D9%2588%25D9%2582%25D8%25A7%25D8%25AA-%25D8%25B4%25D8%25B1%25D8%25B9%25DB%258C-%25D9%2585%25D8%25B1%25D8%25A7%25DA%25A9%25D8%25B2-%25D8%25A7%25D8%25B3%25D8%25AA%25D8%25A7%D9%86-%25D9%87%D8%A7?_com_liferay_asset_publisher_web_portlet_AssetPublisherPortlet_INSTANCE_lvuu_assetEntryId=7217366"
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
 fun hasValidTimes(): Boolean {
  val values = listOf(fajr, sunrise, dhuhr, sunset, maghrib, midnight)
  if (values.any { !it.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$")) }) return false
  val minutes = values.map { value ->
   val parts = value.split(":")
   parts[0].toInt() * 60 + parts[1].toInt()
  }
  return minutes.zipWithNext().all { (earlier, later) -> later > earlier }
 }
}
/** Fails closed until current-year official data is extracted and verified. */
class OfficialPrayerTimesRepository {
 fun currentState():PrayerTimesState = PrayerTimesState.Unavailable(
  "داده رسمی سال جاری هنوز از صفحه دانشگاه تهران استخراج و اعتبارسنجی نشده است."
 )
}
