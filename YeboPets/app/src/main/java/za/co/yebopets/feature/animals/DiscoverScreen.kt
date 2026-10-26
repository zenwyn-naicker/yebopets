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

package za.co.yebopets.feature.animals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import za.co.yebopets.core.*
import za.co.yebopets.ui.theme.YeboCoral
import za.co.yebopets.ui.theme.YeboInk

class DiscoveryModel(private val repository: AnimalsRepository) : TaskModel() {
    var animals by mutableStateOf<List<Animal>>(emptyList())
        private set

    var hasSearched by mutableStateOf(false)
        private set

    fun search(
        query: String,
        species: String,
        size: String,
        activity: String,
        sex: String,
        age: String,
        area: String,
        radius: Int,
    ) {
        animals = emptyList()
        hasSearched = true
        task("Discover pets") {
            animals = repository.browse(query, species, size, activity, sex, age, area, radius)
        }
    }
}

@Composable
fun DiscoverScreen(
    client: ApiClient,
    defaultSpecies: String = "Any",
    defaultRadius: Int = 25,
    savedIds: Set<String> = emptySet(),
    initials: String = "YP",
    openProfile: () -> Unit = {},
    save: (String) -> Unit = {},
    open: (String) -> Unit,
) {
    val model: DiscoveryModel = viewModel { DiscoveryModel(AnimalsRepository(client)) }
    var query by rememberSaveable { mutableStateOf("") }
    var species by rememberSaveable(defaultSpecies) { mutableStateOf(defaultSpecies) }
    var size by rememberSaveable { mutableStateOf("Any") }
    var activity by rememberSaveable { mutableStateOf("Any") }
    var sex by rememberSaveable { mutableStateOf("Any") }
    var age by rememberSaveable { mutableStateOf("Any") }
    var area by rememberSaveable { mutableStateOf("Any") }
    var radius by rememberSaveable(defaultRadius) { mutableFloatStateOf(defaultRadius.toFloat()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    fun search(selectedSpecies: String = species) = model.search(
        query, selectedSpecies, size, activity, sex, age, area, radius.toInt(),
    )

    LaunchedEffect(defaultSpecies, defaultRadius) {
        snapshotFlow { model.busy }.first { !it }
        search()
    }

    BackHandler(showFilters) { showFilters = false }

    if (showFilters) {
        SearchFiltersScreen(
            species = species,
            size = size,
            activity = activity,
            sex = sex,
            age = age,
            area = area,
            radius = radius,
            enabled = !model.busy,
            onSpecies = { species = it },
            onSize = { size = it },
            onActivity = { activity = it },
            onSex = { sex = it },
            onAge = { age = it },
            onArea = { area = it },
            onRadius = { radius = it },
            onBack = { showFilters = false },
            onClear = {
                species = "Any"
                size = "Any"
                activity = "Any"
                sex = "Any"
                age = "Any"
                area = "Any"
                radius = defaultRadius.toFloat()
            },
            onApply = {
                search()
                showFilters = false
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item { BrandBar(initials, openProfile) }
        item { za.co.yebopets.part3.ConnectionBanner() }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(za.co.yebopets.part3.T("Good morning"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(za.co.yebopets.part3.T("Ready to meet your match?"), style = MaterialTheme.typography.headlineLarge)
                Text(za.co.yebopets.part3.T("Browse pets from Coastal Paws Rescue who could fit your home and lifestyle."))
            }
        }
        item {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = LargeCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, YeboInk),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(Modifier.padding(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(za.co.yebopets.part3.T("Your profile shapes every match"), style = MaterialTheme.typography.titleSmall)
                        Text(za.co.yebopets.part3.T("Update your home and lifestyle details whenever they change."), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = openProfile) { Text(za.co.yebopets.part3.T("View")) }
                }
            }
        }
        item { Box(Modifier.padding(horizontal = 20.dp)) { Status(model) } }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { if (it.length <= 80) query = it },
                label = { Text(za.co.yebopets.part3.T("Search by name or breed")) },
                leadingIcon = { AppIcon(AppIconKind.SEARCH, tint = MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = "Search") },
                trailingIcon = {
                    IconButton(onClick = { showFilters = true }, enabled = !model.busy) {
                        AppIcon(AppIconKind.FILTER, tint = YeboInk, contentDescription = "Open filters")
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                singleLine = true,
                enabled = !model.busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("Any" to "For you", "Dog" to "Dogs", "Cat" to "Cats")) { (value, label) ->
                    ChoiceChip(label, species == value, {
                        species = value
                        search(value)
                    }, enabled = !model.busy)
                }
            }
        }
        item {
            Box(Modifier.padding(horizontal = 20.dp)) {
                SectionHeading("Top matches for you", "Filters") { showFilters = true }
            }
        }
        if (model.hasSearched && !model.busy && model.error == null && model.animals.isEmpty()) {
            item {
                Card(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = LargeCardShape,
                ) {
                    Text(za.co.yebopets.part3.T("No pets match these filters. Try a wider search."), Modifier.padding(20.dp))
                }
            }
        }
        items(model.animals, key = { it.id }) { pet ->
            Box(Modifier.padding(horizontal = 20.dp)) {
                PetCard(pet, pet.id in savedIds, null, { save(pet.id) }) { open(pet.id) }
            }
        }
    }
}

@Composable
private fun SearchFiltersScreen(
    species: String,
    size: String,
    activity: String,
    sex: String,
    age: String,
    area: String,
    radius: Float,
    enabled: Boolean,
    onSpecies: (String) -> Unit,
    onSize: (String) -> Unit,
    onActivity: (String) -> Unit,
    onSex: (String) -> Unit,
    onAge: (String) -> Unit,
    onArea: (String) -> Unit,
    onRadius: (Float) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Search & filters", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(za.co.yebopets.part3.T("Find a pet who fits your everyday life."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceRow("Animal", Choices.species, species, enabled, onSpecies)
            ChoiceRow("Size", Choices.sizes, size, enabled, onSize)
            ChoiceRow("Activity level", listOf("Any") + Choices.activities, activity, enabled, onActivity)
            ChoiceRow("Sex", listOf("Any", "Female", "Male"), sex, enabled, onSex)
            ChoiceRow("Age", Choices.ages, age, enabled, onAge)
            ChoiceRow("Search from", listOf("Any") + Choices.areas, area, enabled, onArea)
            if (area != "Any") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(za.co.yebopets.part3.T("Distance"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(za.co.yebopets.part3.T("Up to ${radius.toInt()} km"), style = MaterialTheme.typography.labelLarge, color = YeboCoral)
                    }
                    Slider(value = radius, onValueChange = onRadius, valueRange = 5f..50f, steps = 8, enabled = enabled)
                    Text(za.co.yebopets.part3.T("Distances are estimates between town centres."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryAction("Clear", onClear, Modifier.weight(1f), enabled)
                PrimaryAction("Show pets", onApply, Modifier.weight(1.5f), enabled, leadingContent = {
                    AppIcon(AppIconKind.SEARCH, tint = YeboInk)
                })
            }
        }
    }
}
