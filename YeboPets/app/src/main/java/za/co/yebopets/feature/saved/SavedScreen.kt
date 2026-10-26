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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import za.co.yebopets.core.*
import za.co.yebopets.ui.theme.YeboCoral

@Composable
fun SavedScreen(
    model: SavedModel,
    open: (String) -> Unit,
    compare: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(za.co.yebopets.part3.T("Saved pets"), style = MaterialTheme.typography.headlineLarge)
                    Text(za.co.yebopets.part3.T("Keep favourites close and compare up to three."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Status(model) }
            item { za.co.yebopets.part3.ConnectionBanner() }
            item { SectionHeading("Your shortlist", "Refresh", model::reload) }
            if (!model.busy && model.error == null && model.animals.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = LargeCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppIcon(AppIconKind.HEART, tint = YeboCoral, size = 34.dp, contentDescription = "Saved pets")
                            Text(za.co.yebopets.part3.T("Your saved pets will appear here"), style = MaterialTheme.typography.titleMedium)
                            Text(za.co.yebopets.part3.T("Open a pet from Discover and tap Save."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(model.animals, key = { it.id }) { pet ->
                CompactSavedCard(
                    pet = pet,
                    selected = pet.id in model.selected,
                    onOpen = { open(pet.id) },
                    onRemove = { model.remove(pet.id) },
                    onSelect = { model.select(pet.id) },
                )
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            PrimaryAction(
                "Compare selected (${model.selected.size}/3)",
                compare,
                Modifier.fillMaxWidth().padding(16.dp),
                enabled = model.selected.isNotEmpty() && !model.busy,
            )
        }
    }
}

@Composable
private fun CompactSavedCard(
    pet: Animal,
    selected: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onSelect: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = LargeCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimalImage(pet, Modifier.size(94.dp).clip(RoundedCornerShape(17.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(pet.name, style = MaterialTheme.typography.titleLarge)
                Text("${pet.ageLabel()} · ${za.co.yebopets.part3.T(pet.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(pet.locationLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(
                    selected = selected,
                    onClick = onSelect,
                    label = { Text(za.co.yebopets.part3.T(if (selected) "Selected" else "Compare")) },
                    leadingIcon = if (selected) {
                        { AppIcon(AppIconKind.CHECK, size = 16.dp, contentDescription = "Selected for comparison") }
                    } else null,
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp).clip(CircleShape)) {
                AppIcon(AppIconKind.HEART, tint = YeboCoral, size = 20.dp, contentDescription = "Remove ${pet.name} from saved pets")
            }
        }
    }
}

@Composable
fun SavedAction(model: SavedModel, animal: Animal) {
    val saved = model.animals.any { it.id == animal.id }
    Status(model)
    PrimaryAction(
        if (saved) "Remove from saved pets" else "Save pet",
        { if (saved) model.remove(animal.id) else model.save(animal.id) },
        Modifier.fillMaxWidth(),
        enabled = !model.busy,
    )
}
