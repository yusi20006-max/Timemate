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
        if (!isCacheable(day)) return false
        val key = key(day.city.id, day.dateIso)
        return preferences.edit()
            .putString(key, gson.toJson(CachedPrayerTimes(day, fetchedAtMillis)))
            .commit()
    }

    fun saveAll(days: List<PrayerTimesDay>, fetchedAtMillis: Long = System.currentTimeMillis()): Boolean {
        if (days.size !in 365..366 || days.any { !isCacheable(it) }) return false
        val cityId = days.firstOrNull()?.city?.id ?: return false
        if (days.any { it.city.id != cityId }) return false
        if (days.map { it.dateIso }.toSet().size != days.size) return false

        val editor = preferences.edit()
        days.forEach { day ->
            editor.putString(key(day.city.id, day.dateIso), gson.toJson(CachedPrayerTimes(day, fetchedAtMillis)))
        }
        return editor.commit()
    }

    fun get(city: PrayerCity, dateIso: String): CachedPrayerTimes? {
        if (runCatching { LocalDate.parse(dateIso) }.isFailure) return null
        val raw = preferences.getString(key(city.id, dateIso), null) ?: return null
        return runCatching { gson.fromJson(raw, CachedPrayerTimes::class.java) }
            .getOrNull()
            ?.takeIf {
                it.day.city.id == city.id &&
                    it.day.dateIso == dateIso &&
                    isCacheable(it.day)
            }
    }

    fun getTomorrow(city: PrayerCity, date: LocalDate): PrayerTimesDay? =
        get(city, date.toString())?.day

    private fun isCacheable(day: PrayerTimesDay): Boolean {
        if (!day.hasValidTimes() || !day.hasOfficialSource()) return false
        if (runCatching { LocalDate.parse(day.dateIso) }.isFailure) return false
        return PrayerCities.provincialCapitals.any { it.id == day.city.id }
    }

    private fun key(cityId: String, dateIso: String) = "day_${cityId}_${dateIso}"

    companion object {
        private const val PREFERENCES = "official_prayer_times_cache"
    }
}
