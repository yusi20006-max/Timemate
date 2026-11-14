package ir.timemate.app.prayer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * Reads the signed-off annual Tehran dataset shipped inside the APK.
 * No network access, PDF download, scraping, or runtime PDF parsing is performed.
 */
class OfficialPrayerTimesRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun ensureBundledYear(city: PrayerCity): Boolean =
        withContext(Dispatchers.IO) { loadBundledYearBlocking(city) }

    fun loadBundledYearBlocking(city: PrayerCity): Boolean {
        if (city.id != "tehran") return false
        if (tehranDays != null) return true

        synchronized(loadLock) {
            if (tehranDays != null) return true
            return runCatching {
                val json = InputStreamReader(
                    appContext.assets.open(ASSET_PATH),
                    Charsets.UTF_8
                ).use { it.readText() }
                val parsed = BundledPrayerTimesParser.parse(json, city)
                require(parsed.size == 365) { "Bundled Tehran 1405 dataset failed validation." }
                tehranDays = parsed.associateBy { it.dateIso }
                true
            }.onFailure { error ->
                Log.e(TAG, "Unable to load bundled Tehran prayer times.", error)
            }.getOrDefault(false)
        }
    }

    fun day(city: PrayerCity, dateIso: String): PrayerTimesDay? {
        if (city.id != "tehran") return null
        return tehranDays?.get(dateIso)
    }

    companion object {
        private const val TAG = "BundledPrayerTimes"
        private const val ASSET_PATH = "prayer-times/tehran-1405.json"
        private val loadLock = Any()

        @Volatile
        private var tehranDays: Map<String, PrayerTimesDay>? = null
    }
}
