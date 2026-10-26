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

package za.co.yebopets.feature.saved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import za.co.yebopets.core.*
import za.co.yebopets.feature.profile.MatchScoreText
import za.co.yebopets.ui.theme.YeboInk

@Composable
fun CompareScreen(client: ApiClient, model: SavedModel, back: () -> Unit) {
    val pets = model.animals.filter { it.id in model.selected }.take(3)

    Page("Compare your shortlist", back) {
        Text(za.co.yebopets.part3.T("Compare the details that matter most before choosing your next step."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (pets.isEmpty()) {
            Text(za.co.yebopets.part3.T("Select pets from your saved list first."))
            return@Page
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = LargeCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, YeboInk),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp)) {
                ComparisonHeader(pets)
                ComparisonRow("Compatibility", pets) { pet -> MatchScoreText(client, pet.id) }
                ComparisonRow("Distance", pets) { pet -> Text(pet.distanceKm?.let { "${it.toInt()} km" } ?: za.co.yebopets.part3.T("Not available"), style = MaterialTheme.typography.labelMedium) }
                ComparisonRow("Age", pets) { pet -> Text(pet.ageLabel(), style = MaterialTheme.typography.labelMedium) }
                ComparisonRow("Size", pets) { pet -> Text(za.co.yebopets.part3.T(pet.size), style = MaterialTheme.typography.labelMedium) }
                ComparisonRow("Activity", pets) { pet -> Text(za.co.yebopets.part3.T(pet.activity), style = MaterialTheme.typography.labelMedium) }
                ComparisonRow("Care needs", pets) { pet -> Text(pet.care, style = MaterialTheme.typography.bodySmall, maxLines = 4) }
                ComparisonRow("Secure yard", pets) { pet -> Text(za.co.yebopets.part3.T(if (pet.needsYard) "Required" else "Not required"), style = MaterialTheme.typography.labelMedium) }
                ComparisonRow("Children", pets) { pet -> Text(za.co.yebopets.part3.T(if (pet.goodWithChildren) "Suitable" else "Ask the shelter"), style = MaterialTheme.typography.labelMedium) }
                ComparisonRow("Other pets", pets) { pet -> Text(za.co.yebopets.part3.T(if (pet.goodWithPets) "Suitable" else "Ask the shelter"), style = MaterialTheme.typography.labelMedium) }
            }
        }
        Text(za.co.yebopets.part3.T("Compatibility is guidance only. Coastal Paws Rescue makes the final adoption decision."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ComparisonHeader(pets: List<Animal>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(110.dp))
        pets.forEach { pet ->
            Column(Modifier.width(140.dp).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimalImage(pet, Modifier.size(72.dp))
                Spacer(Modifier.height(7.dp))
                Text(pet.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ComparisonRow(label: String, pets: List<Animal>, value: @Composable (Animal) -> Unit) {
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(verticalAlignment = Alignment.Top) {
        Text(za.co.yebopets.part3.T(label), Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        pets.forEach { pet -> Box(Modifier.width(140.dp).padding(horizontal = 8.dp)) { value(pet) } }
    }
}
