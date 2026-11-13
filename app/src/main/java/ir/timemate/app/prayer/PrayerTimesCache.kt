package ir.timemate.app.prayer

import android.content.Context
import com.google.gson.Gson
import java.time.LocalDate

data class CachedPrayerTimes(
    val day: PrayerTimesDay,
    val fetchedAtMillis: Long
)

class PrayerTimesCache(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(day: PrayerTimesDay, fetchedAtMillis: Long = System.currentTimeMillis()): Boolean {
        if (!day.hasValidTimes() || !day.hasOfficialSource()) return false
        if (runCatching { LocalDate.parse(day.dateIso) }.isFailure) return false
        if (PrayerCities.provincialCapitals.none { it.id == day.city.id }) return false
        val key = key(day.city.id, day.dateIso)
        return preferences.edit().putString(key, gson.toJson(CachedPrayerTimes(day, fetchedAtMillis))).commit()
    }

    fun get(city: PrayerCity, dateIso: String): CachedPrayerTimes? {
        if (runCatching { LocalDate.parse(dateIso) }.isFailure) return null
        val raw = preferences.getString(key(city.id, dateIso), null) ?: return null
        return runCatching { gson.fromJson(raw, CachedPrayerTimes::class.java) }
            .getOrNull()
            ?.takeIf { it.day.city.id == city.id && it.day.dateIso == dateIso && it.day.hasValidTimes() && it.day.hasOfficialSource() }
    }

    fun getTomorrow(city: PrayerCity, date: LocalDate): PrayerTimesDay? =
        get(city, date.toString())?.day

    private fun key(cityId: String, dateIso: String) = "day_${cityId}_${dateIso}"

    companion object {
        private const val PREFERENCES = "official_prayer_times_cache"
    }
}
