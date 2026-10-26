/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: State and Jetpack Compose
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/ui/compose/state> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: State holders and UI state
 * Type: [Source Code]
 * Available at: <https://developer.android.com/topic/architecture/ui-layer/stateholders> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: BackHandler
 * Type: [Source Code]
 * Available at: <https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#BackHandler(kotlin.Boolean,kotlin.Function0)> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Material 3 in Compose
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/ui/compose/designsystems/material3> [Accessed 9 September 2026]
 **************************************************************************************************************/

package za.co.yebopets

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import za.co.yebopets.core.*
import za.co.yebopets.feature.animals.*
import za.co.yebopets.feature.auth.AuthGate
import za.co.yebopets.feature.journey.*
import za.co.yebopets.feature.profile.*
import za.co.yebopets.feature.saved.*
import za.co.yebopets.feature.settings.*
import za.co.yebopets.feature.staff.StaffWorkspace
import za.co.yebopets.ui.theme.YeboPetsTheme

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("YeboPets", "Activity created")
        setContent {
            YeboPetsTheme {
                za.co.yebopets.part3.ReturningAccess {
                    AuthGate { api, requestedRole, signOut, switchWorkspace -> Workspace(api, requestedRole, signOut, switchWorkspace) }
                }
            }
        }
    }

    override fun onStop() {
        Log.i("YeboPets", "Activity stopped")
        super.onStop()
    }
}

@Composable
private fun Workspace(api: ApiClient, requestedRole: String, signOut: () -> Unit, switchWorkspace: (() -> Unit)?) {
    val settings: SettingsModel = viewModel { SettingsModel(SettingsRepository(api, requestedRole)) }
    var tab by rememberSaveable { mutableStateOf("Discover") }
    var selectedAnimalId by rememberSaveable { mutableStateOf<String?>(null) }
    var applying by remember { mutableStateOf<Animal?>(null) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var selectedApplication by remember { mutableStateOf<AdoptionApplication?>(null) }
    var selectedAdoption by remember { mutableStateOf<Adoption?>(null) }
    var followUp by remember { mutableStateOf<Adoption?>(null) }
    var followUpSent by rememberSaveable { mutableStateOf(false) }

    BackHandler(
        selectedAnimalId != null || applying != null || editingProfile || selectedApplication != null ||
            selectedAdoption != null || followUp != null || followUpSent || tab in listOf("Compare", "Settings"),
    ) {
        when {
            followUpSent -> followUpSent = false
            applying != null -> applying = null
            followUp != null -> followUp = null
            selectedAdoption != null -> selectedAdoption = null
            selectedApplication != null -> selectedApplication = null
            editingProfile -> editingProfile = false
            selectedAnimalId != null -> selectedAnimalId = null
            else -> tab = if (tab == "Compare") "Saved" else "Profile"
        }
    }

    Surface(Modifier.fillMaxSize()) {
        if (!settings.ready) {
            AccountLoading(settings, signOut)
        } else if (settings.account?.role == "ShelterStaff") {
            StaffWorkspace(api, signOut, switchWorkspace)
        } else {
            val saved: SavedModel = viewModel { SavedModel(SavedRepository(api)) }
            val initials = settings.account?.name?.split(" ")?.filter { it.isNotBlank() }?.take(2)
                ?.joinToString("") { it.first().uppercase() }?.ifBlank { "YP" } ?: "YP"
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Box(Modifier.weight(1f)) {
                    when {
                        followUpSent -> FollowUpSuccessScreen {
                            followUpSent = false
                            selectedAdoption = null
                            tab = "Applications"
                        }
                        applying != null -> ApplyScreen(api, applying!!, { applying = null }) { applying = null; tab = "Applications" }
                        followUp != null -> FollowUpScreen(api, followUp!!, { followUp = null }) {
                            followUp = null
                            followUpSent = true
                        }
                        selectedAdoption != null -> AftercareScreen(api, selectedAdoption!!, { selectedAdoption = null }) {
                            followUp = selectedAdoption
                        }
                        selectedApplication != null -> ApplicationTrackerScreen(api, selectedApplication!!) { selectedApplication = null }
                        editingProfile -> AdopterProfileScreen(api) { editingProfile = false }
                        selectedAnimalId != null -> DetailsScreen(
                            client = api,
                            id = selectedAnimalId!!,
                            saved = saved.animals.any { it.id == selectedAnimalId },
                            back = { selectedAnimalId = null },
                            onSave = { animal -> if (saved.animals.any { it.id == animal.id }) saved.remove(animal.id) else saved.save(animal.id) },
                            onCompare = { animal ->
                                if (saved.animals.none { it.id == animal.id }) saved.save(animal.id)
                                selectedAnimalId = null
                                tab = "Saved"
                            },
                            matching = { MatchCard(api, it.id) },
                            apply = { applying = it },
                        )
                        tab == "Saved" -> SavedScreen(saved, { selectedAnimalId = it }) { tab = "Compare" }
                        tab == "Compare" -> CompareScreen(api, saved) { tab = "Saved" }
                        tab == "Applications" -> ApplicationsScreen(api, { selectedApplication = it }, { selectedAdoption = it })
                        tab == "Profile" -> ProfileScreen(api, settings.account, { editingProfile = true }, { tab = "Settings" }, signOut)
                        tab == "Settings" -> SettingsScreen(settings, requestedRole, signOut, switchWorkspace) { tab = "Profile" }
                        else -> DiscoverScreen(
                            client = api,
                            defaultSpecies = settings.current.preferredSpecies,
                            defaultRadius = settings.current.searchRadiusKm,
                            savedIds = saved.animals.map { it.id }.toSet(),
                            initials = initials,
                            openProfile = { tab = "Profile" },
                            save = { id -> if (saved.animals.any { it.id == id }) saved.remove(id) else saved.save(id) },
                        ) { selectedAnimalId = it }
                    }
                }
                if (
                    tab != "Compare" && tab != "Settings" && selectedAnimalId == null && applying == null &&
                    !editingProfile && selectedApplication == null && selectedAdoption == null && followUp == null && !followUpSent
                ) {
                    NavigationBar {
                        listOf(
                            Triple("Discover", AppIconKind.HOME, "Discover"),
                            Triple("Saved", AppIconKind.HEART, "Saved"),
                            Triple("Applications", AppIconKind.APPLICATION, "Applications"),
                            Triple("Profile", AppIconKind.PROFILE, "Profile")
                        ).forEach { item ->
                            NavigationBarItem(
                                selected = tab == item.first,
                                onClick = { tab = item.first },
                                icon = { AppIcon(item.second) },
                                label = { Text(za.co.yebopets.part3.T(item.third)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
