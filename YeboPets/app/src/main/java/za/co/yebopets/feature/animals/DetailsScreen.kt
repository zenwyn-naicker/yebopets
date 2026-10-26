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

package za.co.yebopets.feature.animals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import za.co.yebopets.core.*
import za.co.yebopets.ui.theme.YeboCoral
import za.co.yebopets.ui.theme.YeboInk
import za.co.yebopets.ui.theme.YeboPaper

class DetailsModel(private val repository: AnimalsRepository) : TaskModel() {
    var animal by mutableStateOf<Animal?>(null)
        private set

    fun load(id: String) = task("Load animal details") {
        animal = null
        animal = repository.details(id)
    }
}

@Composable
fun DetailsScreen(
    client: ApiClient,
    id: String,
    saved: Boolean,
    back: () -> Unit,
    onSave: (Animal) -> Unit,
    onCompare: (Animal) -> Unit,
    matching: @Composable (Animal) -> Unit = {},
    apply: (Animal) -> Unit = {},
) {
    val model: DetailsModel = viewModel(key = "details:$id") { DetailsModel(AnimalsRepository(client)) }

    LaunchedEffect(id) {
        snapshotFlow { model.busy }.first { !it }
        model.load(id)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (model.animal == null) {
            ScreenHeader("Pet details", back)
            Box(Modifier.padding(horizontal = 20.dp)) { Status(model) }
            if (model.error != null) {
                PrimaryAction("Retry", { model.load(id) }, Modifier.padding(20.dp).fillMaxWidth(), enabled = !model.busy)
            }
        }

        model.animal?.let { pet ->
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().height(340.dp)) {
                        AnimalImage(pet, Modifier.fillMaxSize())
                        IconButton(
                            onClick = back,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(48.dp).clip(CircleShape).background(YeboPaper.copy(alpha = .94f)),
                        ) { AppIcon(AppIconKind.BACK, size = 19.dp, contentDescription = "Back") }
                        IconButton(
                            onClick = { onSave(pet) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp).clip(CircleShape).background(YeboPaper.copy(alpha = .94f)),
                        ) {
                            AppIcon(
                                AppIconKind.HEART,
                                tint = if (saved) YeboCoral else YeboInk,
                                size = 21.dp,
                                contentDescription = if (saved) "Remove ${pet.name} from saved pets" else "Save ${pet.name}",
                            )
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Text(pet.name, style = MaterialTheme.typography.headlineLarge)
                        Text("${za.co.yebopets.part3.T(pet.species)} · ${pet.breed}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${pet.ageLabel()} · ${za.co.yebopets.part3.T(pet.sex)} · ${za.co.yebopets.part3.T(pet.size)} · ${za.co.yebopets.part3.T(pet.activity)}")
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(AppIconKind.LOCATION, size = 16.dp, tint = YeboCoral, contentDescription = "Location")
                            Text(pet.locationLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(pet.description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    Card(
                        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        shape = LargeCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeading("Care at a glance")
                            Text(pet.care)
                            KeyValueRow("Secure yard", if (pet.needsYard) "Required" else "Not required")
                            KeyValueRow("Children", if (pet.goodWithChildren) "Suitable" else "Not suitable")
                            KeyValueRow("Other pets", if (pet.goodWithPets) "Suitable" else "Not suitable")
                            KeyValueRow("Experience", "${pet.experienceRequired}/2", showDivider = false)
                        }
                    }
                }
                item { Box(Modifier.padding(horizontal = 20.dp)) { matching(pet) } }
                item { Spacer(Modifier.height(8.dp)) }
            }
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SecondaryAction(if (saved) "Saved" else "Save", { onSave(pet) }, Modifier.weight(1f))
                    SecondaryAction("Compare", { onCompare(pet) }, Modifier.weight(1.15f))
                    PrimaryAction("Apply", { apply(pet) }, Modifier.weight(1.2f))
                }
            }
        }
    }
}
