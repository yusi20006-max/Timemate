package ir.timemate.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeContractTest {
    @Test
    fun stableStorageIdsResolveToExpectedThemes() {
        assertEquals(AppTheme.DEEP_OCEAN, AppTheme.fromStorage("deep_ocean"))
        assertEquals(AppTheme.MIDNIGHT_ROSE, AppTheme.fromStorage("midnight_rose"))
        assertEquals(AppTheme.FOREST_NIGHT, AppTheme.fromStorage("forest_night"))
    }

    @Test
    fun missingOrUnknownThemeFallsBackToDeepOcean() {
        assertEquals(AppTheme.DEEP_OCEAN, AppTheme.fromStorage(null))
        assertEquals(AppTheme.DEEP_OCEAN, AppTheme.fromStorage(""))
        assertEquals(AppTheme.DEEP_OCEAN, AppTheme.fromStorage("unknown_theme"))
    }
}
