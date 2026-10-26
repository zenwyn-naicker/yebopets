package za.co.yebopets

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import za.co.yebopets.core.ApiClient
import za.co.yebopets.feature.journey.*
import za.co.yebopets.ui.theme.YeboPetsTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyUiTest {
    @get:Rule val compose = createComposeRule()
    private val appointment = Appointment("visit", "application", "HomeCheck", "2026-11-01T10:00:00+02:00",
        "Adopter home", "12 Test Road", "Bring the checklist", "Scheduled")

    @Test fun appointmentShowsStoredLocationAddressAndNotes() {
        compose.setContent { YeboPetsTheme { AppointmentHistory(listOf(appointment)) } }
        compose.onNodeWithText("Home check").assertExists()
        compose.onNodeWithText("Adopter home").assertExists()
        compose.onNodeWithText("12 Test Road").assertExists()
        compose.onNodeWithText("Bring the checklist").assertExists()
        compose.onNodeWithText("Scheduled").assertExists()
    }

    @Test fun zuluAppointmentLabelsDoNotTranslateUserText() {
        val context = RuntimeEnvironment.getApplication()
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("zu")) }
        val zulu = context.createConfigurationContext(config)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides zulu, LocalConfiguration provides config) {
                YeboPetsTheme { AppointmentHistory(listOf(appointment)) }
            }
        }
        compose.onNodeWithText("Ukuhlolwa kwekhaya").assertExists()
        compose.onNodeWithText("Kuhleliwe").assertExists()
        compose.onNodeWithText("12 Test Road").assertExists()
    }

    @Test fun staffCanOpenReschedulingAndSeeStatusControls() {
        val model = JourneyModel(JourneyRepository(ApiClient("http://localhost/") { "test" }))
        compose.setContent { YeboPetsTheme { StaffAppointment(appointment, model) } }
        compose.onNodeWithText("Mark completed").assertExists()
        compose.onNodeWithText("Cancel visit").assertExists()
        compose.onNodeWithText("Reschedule").performClick()
        compose.onNodeWithText("Save appointment").assertExists()
        compose.onNodeWithText("Appointment date").assertExists()
    }
}
