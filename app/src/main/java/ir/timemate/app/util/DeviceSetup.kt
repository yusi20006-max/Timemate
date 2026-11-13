package ir.timemate.app.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deviceSetupDataStore by preferencesDataStore(name = "timemate_device_setup")

enum class DeviceSetupManufacturer(val storageId: String, val title: String) {
    XIAOMI("xiaomi", "Xiaomi / Redmi / POCO"),
    SAMSUNG("samsung", "Samsung"),
    OTHER("other", "سایر گوشی‌های Android");

    companion object {
        fun fromStorage(value: String?): DeviceSetupManufacturer? =
            values().firstOrNull { it.storageId == value }

        fun detect(manufacturer: String, brand: String): DeviceSetupManufacturer {
            val normalized = "${manufacturer.lowercase()} ${brand.lowercase()}"
            return when {
                listOf("xiaomi", "redmi", "poco").any(normalized::contains) -> XIAOMI
                listOf("samsung").any(normalized::contains) -> SAMSUNG
                else -> OTHER
            }
        }
    }
}

data class DeviceSetupPreferences(
    val manufacturer: DeviceSetupManufacturer? = null,
    val guideCompleted: Boolean = false
)

object DeviceSetupDataStore {
    private val manufacturerKey = stringPreferencesKey("device_manufacturer")
    private val guideCompletedKey = androidx.datastore.preferences.core.booleanPreferencesKey("guide_completed")

    fun observe(context: Context): Flow<DeviceSetupPreferences> =
        context.applicationContext.deviceSetupDataStore.data.map { preferences ->
            DeviceSetupPreferences(
                manufacturer = DeviceSetupManufacturer.fromStorage(preferences[manufacturerKey]),
                guideCompleted = preferences[guideCompletedKey] ?: false
            )
        }

    suspend fun setManufacturer(context: Context, manufacturer: DeviceSetupManufacturer) {
        context.applicationContext.deviceSetupDataStore.edit {
            it[manufacturerKey] = manufacturer.storageId
            it[guideCompletedKey] = false
        }
    }

    suspend fun markGuideCompleted(context: Context) {
        context.applicationContext.deviceSetupDataStore.edit { it[guideCompletedKey] = true }
    }
}
