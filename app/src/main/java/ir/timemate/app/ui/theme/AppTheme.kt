package ir.timemate.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.timeMateThemeDataStore by preferencesDataStore(name = "timemate_theme")

enum class AppTheme(val storageId: String, val displayName: String, val description: String) {
    DEEP_OCEAN("deep_ocean", "اقیانوس عمیق", "آبی لاجوردی و بنفش نئونی"),
    MIDNIGHT_ROSE("midnight_rose", "رز نیمه‌شب", "قرمز تیره و صورتی نئونی"),
    FOREST_NIGHT("forest_night", "جنگل شب", "سبز زمردی و فیروزه‌ای");

    companion object {
        fun fromStorage(value: String?): AppTheme =
            entries.firstOrNull { it.storageId == value } ?: DEEP_OCEAN
    }
}

object ThemeDataStore {
    private val themeKey = stringPreferencesKey("selected_theme")

    fun observe(context: Context): Flow<AppTheme> =
        context.applicationContext.timeMateThemeDataStore.data
            .catch { error ->
                if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
                else throw error
            }
            .map { preferences -> AppTheme.fromStorage(preferences[themeKey]) }

    suspend fun setTheme(context: Context, theme: AppTheme) {
        context.applicationContext.timeMateThemeDataStore.edit { preferences ->
            preferences[themeKey] = theme.storageId
        }
    }
}
