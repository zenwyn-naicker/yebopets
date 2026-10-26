package za.co.yebopets

import android.app.Application
import android.os.Looper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import za.co.yebopets.core.ApiClient
import za.co.yebopets.feature.auth.AccountAccessModel
import za.co.yebopets.feature.settings.SettingsRepository
import za.co.yebopets.part3.Local

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccountAccessModelTest {
    @Before fun setup() { Local.init(RuntimeEnvironment.getApplication()) }

    private fun awaitLoaded(model: AccountAccessModel) {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            if (!model.busy) return
            Thread.sleep(10)
        }
        fail("Account access did not finish loading")
    }

    private fun reply(uid: String, roles: String) = MockResponse().setBody(
        """{"id":"$uid","name":"Test User","email":"test@example.com","role":"Adopter","roles":$roles}""")

    @Test fun adopterIsRoutedAutomaticallyAndCannotChooseStaff() {
        MockWebServer().use { server ->
            server.enqueue(reply("adopter", """["Adopter"]"""))
            val model = AccountAccessModel(SettingsRepository(ApiClient(server.url("/").toString(), "adopter") { "token" }, "Adopter"), "adopter")
            awaitLoaded(model)
            assertNull(model.error)
            assertEquals("Adopter", model.selectedRole)
            try { model.choose("ShelterStaff"); fail("Unapproved role was accepted") } catch (_: IllegalArgumentException) { }
            val request = server.takeRequest()
            assertEquals("/api/me/access", request.path)
            assertEquals("Bearer token", request.getHeader("Authorization"))
        }
    }

    @Test fun dualAccessOffersChoiceOnEveryNewSessionAndAllowsSwitching() {
        Local.prefs("dual").edit().putString("workspaceRole", "ShelterStaff").commit()
        MockWebServer().use { server ->
            server.enqueue(reply("dual", """["Adopter","ShelterStaff"]"""))
            val repository = SettingsRepository(ApiClient(server.url("/").toString(), "dual") { "token" }, "Adopter")
            val model = AccountAccessModel(repository, "dual")
            awaitLoaded(model)
            assertNull(model.selectedRole)
            model.choose("ShelterStaff")
            assertEquals("ShelterStaff", model.selectedRole)
            model.switchWorkspace()
            assertNull(model.selectedRole)
            server.enqueue(reply("dual", """["Adopter","ShelterStaff"]"""))
            val returning = AccountAccessModel(repository, "dual")
            awaitLoaded(returning)
            assertNull(returning.selectedRole)
        }
    }

    @Test fun staleSavedRoleCannotOverrideTheServersCurrentPermissions() {
        Local.prefs("revoked").edit().putString("workspaceRole", "ShelterStaff").commit()
        MockWebServer().use { server ->
            server.enqueue(reply("revoked", """["Adopter"]"""))
            val model = AccountAccessModel(SettingsRepository(ApiClient(server.url("/").toString(), "revoked") { "token" }, "Adopter"), "revoked")
            awaitLoaded(model)
            assertEquals("Adopter", model.selectedRole)
        }
    }
}
