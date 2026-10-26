package za.co.yebopets

import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.feature.settings.UserSettings

class SettingsTest {
    @Test
    fun validRadiusBoundariesAreAccepted() {
        assertTrue(UserSettings(searchRadiusKm = 5).isValid())
        assertTrue(UserSettings(searchRadiusKm = 50).isValid())
    }

    @Test
    fun invalidRadiusAndSpeciesAreRejected() {
        assertFalse(UserSettings(searchRadiusKm = 4).isValid())
        assertFalse(UserSettings(searchRadiusKm = 51).isValid())
        assertFalse(UserSettings(preferredSpecies = "Dragon").isValid())
    }
}