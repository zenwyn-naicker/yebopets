package za.co.yebopets

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import za.co.yebopets.feature.auth.WorkspacePicker
import za.co.yebopets.feature.journey.AdoptionApplication
import za.co.yebopets.feature.settings.UserAccount
import za.co.yebopets.feature.settings.UserSettings
import za.co.yebopets.feature.settings.AdopterPreferences
import za.co.yebopets.feature.staff.StaffApplicationList
import za.co.yebopets.part3.LanguageSelector
import za.co.yebopets.ui.theme.YeboPetsTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkspaceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun languageSelectorShowsAndChangesItsSelectedOption() {
        compose.setContent {
            var language by remember { mutableStateOf("en") }
            YeboPetsTheme { LanguageSelector(language) { language = it } }
        }
        compose.onNodeWithText("English").assertIsSelected()
        compose.onNodeWithText("isiZulu").performClick().assertIsSelected()
        compose.onNodeWithText("English").assertIsNotSelected()
    }

    @Test fun workspaceChoiceUsesOnlyPermittedRoles() {
        var selected: String? = null
        compose.setContent {
            YeboPetsTheme {
                WorkspacePicker(UserAccount("user", "Test User", "test@example.com", "Adopter"),
                    listOf("Adopter"), { selected = it }, {})
            }
        }
        compose.onNodeWithText("Shelter staff").assertDoesNotExist()
        compose.onNodeWithText("Adopter").performClick()
        assertEquals("Adopter", selected)
    }

    @Test fun dualAccountDiscoveryPreferencesFollowTheWorkspaceAndSurviveSwitching() {
        var role by mutableStateOf("ShelterStaff")
        var settings by mutableStateOf(UserSettings(searchRadiusKm = 40, preferredSpecies = "Cat"))
        compose.setContent {
            YeboPetsTheme {
                androidx.compose.foundation.layout.Column {
                    AdopterPreferences(role, settings, true) { settings = it }
                }
            }
        }
        compose.onNodeWithText("Preferred species").assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).assertCountEquals(0)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
        compose.runOnIdle { role = "Adopter" }
        compose.onNodeWithText("Preferred species").assertExists()
        compose.onNodeWithText("Cat").assertExists()
        compose.onNodeWithText("Dog").performClick()
        compose.runOnIdle { role = "ShelterStaff" }
        compose.onNodeWithText("Preferred species").assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(40, settings.searchRadiusKm)
            assertEquals("Dog", settings.preferredSpecies)
            role = "Adopter"
        }
        compose.onNodeWithText("Preferred species").assertExists()
        compose.runOnIdle { assertEquals("Dog", settings.preferredSpecies) }
    }

    @Test fun staffCanFilterAndSearchWithoutMixingReviewedApplicationsIntoTheQueue() {
        val waiting = AdoptionApplication("waiting1", "luna", "owner", "Submitted", "A caring home", true, true,
            null, null, "2026-10-22T10:00:00Z", "2026-10-22T10:00:00Z")
        val approved = waiting.copy(id = "approved", animalId = "milo", status = "Approved")
        compose.setContent {
            YeboPetsTheme { StaffApplicationList(listOf(approved, waiting), emptyList(), false, {}, {}, {}) }
        }
        compose.onNodeWithText("Needs review").performClick()
        compose.onNodeWithText("Reference: waiting1").assertExists()
        compose.onNodeWithText("Reference: approved").assertDoesNotExist()
        compose.onNodeWithText("Other").performClick().assertIsSelected()
        val filterBounds = listOf("All", "Needs review", "Other").map {
            compose.onNodeWithText(it).getBoundsInRoot()
        }
        filterBounds.drop(1).forEach {
            assertEquals(filterBounds.first().top, it.top)
            assertEquals(filterBounds.first().bottom, it.bottom)
        }
        compose.onNodeWithText("Reference: approved").assertExists()
        compose.onNodeWithText("Reference: waiting1").assertDoesNotExist()
        compose.onNodeWithText("All").performClick().assertIsSelected()
        compose.onNodeWithText("Needs review (1)").assertIsDisplayed()
        compose.onNodeWithText("Reference: waiting1").assertIsDisplayed()
        compose.onNodeWithText("Other").performClick()
        compose.onNodeWithText("Search by pet or reference").performTextInput("nobody")
        compose.onNodeWithText("No applications match your search.").assertExists()
    }
}
