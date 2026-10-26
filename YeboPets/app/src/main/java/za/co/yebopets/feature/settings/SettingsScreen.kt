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
 * Title: Material 3 in Compose
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/ui/compose/designsystems/material3> [Accessed 9 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.co.yebopets.core.*
import za.co.yebopets.ui.theme.YeboPaper

@Composable
fun SettingsScreen(model: SettingsModel, workspaceRole: String, signOut: () -> Unit, switchWorkspace: (() -> Unit)? = null, back: () -> Unit) {
    Page("Settings", back) {
        model.account?.let {
            Card(Modifier.fillMaxWidth(), shape = LargeCardShape, colors = CardDefaults.cardColors(containerColor = YeboPaper), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(it.name.ifBlank { "Google account" }, style = MaterialTheme.typography.titleLarge)
                    Text(it.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusPill(if (workspaceRole == "ShelterStaff") "Shelter staff" else "Adopter")
                }
            }
        }

        Status(model)

        AdopterPreferences(workspaceRole, model.draft, !model.busy) { model.draft = it }

        za.co.yebopets.part3.Part3Settings(model)

        PrimaryAction("Save settings", model::save, Modifier.fillMaxWidth(), enabled = !model.busy)

        switchWorkspace?.let { SecondaryAction("Switch workspace", it, Modifier.fillMaxWidth()) }
        SecondaryAction("Sign out", signOut, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun AdopterPreferences(workspaceRole: String, settings: UserSettings, enabled: Boolean, update: (UserSettings) -> Unit) {
    if (workspaceRole != "Adopter") return

    Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_2, settings.searchRadiusKm))
    Slider(
        value = settings.searchRadiusKm.toFloat(),
        onValueChange = { update(settings.copy(searchRadiusKm = it.toInt())) },
        valueRange = 5f..50f,
        enabled = enabled,
    )
    ChoiceRow("Preferred species", Choices.species, settings.preferredSpecies, enabled) {
        update(settings.copy(preferredSpecies = it))
    }
    Text(za.co.yebopets.part3.T("Saved species and radius become the defaults when opening Discover."))
}

@Composable
fun AccountLoading(model: SettingsModel, signOut: () -> Unit) {
    Page("Connecting your account") {
        Status(model)
        Text(za.co.yebopets.part3.T("Loading your account and saved preferences."))

        PrimaryAction("Retry", model::load, Modifier.fillMaxWidth(), enabled = !model.busy)

        TextButton(onClick = signOut) {
            Text(za.co.yebopets.part3.T("Sign out"))
        }
    }
}
