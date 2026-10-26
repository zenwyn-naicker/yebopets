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

package za.co.yebopets.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import za.co.yebopets.core.*
import za.co.yebopets.feature.settings.UserAccount
import za.co.yebopets.ui.theme.YeboCoral
import za.co.yebopets.ui.theme.YeboInk
import za.co.yebopets.ui.theme.YeboTeal

@Composable
fun ProfileScreen(
    client: ApiClient,
    account: UserAccount?,
    openProfile: () -> Unit,
    openSettings: () -> Unit,
    signOut: () -> Unit,
) {
    val model: ProfileModel = viewModel { ProfileModel(ProfileRepository(client)) }
    val completion = model.draft.completionPercent()
    val name = account?.name?.ifBlank { "Google account" } ?: "Google account"
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "YP" }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(za.co.yebopets.part3.T("Profile"), style = MaterialTheme.typography.headlineLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(YeboInk), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(account?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill("Verified", color = YeboTeal)
        }
        Status(model)
        Card(
            shape = LargeCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Text(za.co.yebopets.part3.T("Adopter profile"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("$completion%", style = MaterialTheme.typography.labelLarge, color = YeboCoral)
                }
                LinearProgressIndicator(
                    progress = { completion / 100f },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = YeboCoral,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    if (completion == 100) za.co.yebopets.part3.T("Your matching profile is complete.")
                    else za.co.yebopets.part3.T("Add your household and care details to improve match accuracy."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryAction(if (completion == 100) "Edit profile" else "Complete profile", openProfile, Modifier.fillMaxWidth())
            }
        }
        SectionHeading("Your account")
        ProfileMenuRow(AppIconKind.PROFILE, "Adopter profile", "Home, lifestyle and preferences", openProfile)
        ProfileMenuRow(AppIconKind.SETTINGS, "Settings", "Discovery preferences and account", openSettings)
        SecondaryAction("Sign out", signOut, Modifier.fillMaxWidth())
        Text(za.co.yebopets.part3.T("YeboPets · Built for thoughtful South African adoptions"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileMenuRow(icon: AppIconKind, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                AppIcon(icon, tint = YeboCoral, contentDescription = title)
            }
            Column(Modifier.weight(1f)) {
                Text(za.co.yebopets.part3.T(title), style = MaterialTheme.typography.titleSmall)
                Text(za.co.yebopets.part3.T(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("→", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AdopterProfileScreen(client: ApiClient, back: () -> Unit) {
    val model: ProfileModel = viewModel { ProfileModel(ProfileRepository(client)) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Adopter profile", back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = LargeCardShape) {
                Text(za.co.yebopets.part3.T("These details help explain why each animal may suit your home. You can update them at any time."), Modifier.padding(16.dp))
            }
            Status(model)
            SectionHeading("Contact & location")
            YeboTextField(model.draft.phone, { if (it.length <= 16) model.draft = model.draft.copy(phone = it) }, "Contact phone", enabled = !model.busy)
            ChoiceRow("Area", Choices.areas, model.draft.area, !model.busy) { model.draft = model.draft.copy(area = it) }
            ChoiceRow("Home type", listOf("House", "Apartment", "Townhouse"), model.draft.housing, !model.busy) { model.draft = model.draft.copy(housing = it) }
            BooleanRow("Secure outdoor yard", model.draft.hasSecureYard, !model.busy) { model.draft = model.draft.copy(hasSecureYard = it) }
            BooleanRow("Children live in my home", model.draft.hasChildren, !model.busy) { model.draft = model.draft.copy(hasChildren = it) }
            ChoiceRow("Existing pets", (0..5).map { it.toString() }, model.draft.existingPets.toString(), !model.busy) { model.draft = model.draft.copy(existingPets = it.toInt()) }

            SectionHeading("Your lifestyle")
            ChoiceRow("Experience level", listOf("0", "1", "2"), model.draft.experience.toString(), !model.busy) { model.draft = model.draft.copy(experience = it.toInt()) }
            Text(za.co.yebopets.part3.T("0 = first pet, 1 = some experience, 2 = experienced"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceRow("Activity level", Choices.activities, model.draft.activity, !model.busy) { model.draft = model.draft.copy(activity = it) }

            SectionHeading("Preferences")
            ChoiceRow("Preferred species", Choices.species, model.draft.preferredSpecies, !model.busy) { model.draft = model.draft.copy(preferredSpecies = it) }
            ChoiceRow("Preferred size", Choices.sizes, model.draft.preferredSize, !model.busy) { model.draft = model.draft.copy(preferredSize = it) }
            ChoiceRow("Preferred age", Choices.ages, model.draft.preferredAge, !model.busy) { model.draft = model.draft.copy(preferredAge = it) }
            Row(Modifier.fillMaxWidth()) {
                Text(za.co.yebopets.part3.T("Maximum travel distance"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text("${model.draft.maxDistanceKm} km", style = MaterialTheme.typography.labelLarge, color = YeboCoral)
            }
            Slider(
                value = model.draft.maxDistanceKm.toFloat(),
                onValueChange = { model.draft = model.draft.copy(maxDistanceKm = it.toInt()) },
                valueRange = 5f..50f,
                steps = 8,
                enabled = !model.busy,
            )
            BooleanRow("I am ready for the time, cost and responsibility of long-term care", model.draft.careReady, !model.busy) {
                model.draft = model.draft.copy(careReady = it)
            }
            Text(za.co.yebopets.part3.T("Your profile is used for matching and shelter review."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            PrimaryAction("Save profile", model::save, Modifier.padding(16.dp).fillMaxWidth(), enabled = !model.busy)
        }
    }
}

private fun AdopterProfile.completionPercent(): Int {
    val checks = listOf(
        phone.isNotBlank(),
        area.isNotBlank(),
        housing.isNotBlank(),
        activity.isNotBlank(),
        preferredSpecies.isNotBlank(),
        preferredSize.isNotBlank(),
        preferredAge.isNotBlank(),
        careReady,
    )
    return checks.count { it } * 100 / checks.size
}
