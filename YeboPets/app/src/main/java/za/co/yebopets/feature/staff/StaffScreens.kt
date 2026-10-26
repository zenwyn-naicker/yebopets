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

package za.co.yebopets.feature.staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import za.co.yebopets.core.*
import za.co.yebopets.feature.journey.*
import za.co.yebopets.ui.theme.*

class StaffModel(private val repository: StaffRepository) : TaskModel() {
    var animals by mutableStateOf<List<Animal>>(emptyList())
        private set

    init { load() }

    fun load() = task("Load shelter animals") { animals = repository.animals() }

    fun save(animal: Animal, existing: Boolean, done: () -> Unit) = task("Save animal profile") {
        require(animal.name.isNotBlank() && animal.breed.isNotBlank()) { "Enter an animal name and breed." }
        repository.save(animal, existing)
        animals = repository.animals()
        done()
    }
}

@Composable
fun StaffWorkspace(client: ApiClient, signOut: () -> Unit, switchWorkspace: (() -> Unit)? = null) {
    val settings: za.co.yebopets.feature.settings.SettingsModel = viewModel {
        za.co.yebopets.feature.settings.SettingsModel(za.co.yebopets.feature.settings.SettingsRepository(client, "ShelterStaff"))
    }
    val animals: StaffModel = viewModel { StaffModel(StaffRepository(client)) }
    val journey: JourneyModel = viewModel { JourneyModel(JourneyRepository(client)) }
    var screen by rememberSaveable { mutableStateOf("dashboard") }
    var selectedAnimal by remember { mutableStateOf<Animal?>(null) }
    var selectedApplication by remember { mutableStateOf<AdoptionApplication?>(null) }
    val roots = listOf("dashboard", "animals", "applications", "settings")
    val screenState = rememberSaveableStateHolder()
    val openReview: (AdoptionApplication) -> Unit = { selectedApplication = it; screen = "review" }

    LaunchedEffect(Unit) { journey.loadStaff() }
    BackHandler(screen != "dashboard") {
        screen = when (screen) {
            "animal" -> "animals"
            "review" -> "applications"
            else -> "dashboard"
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(Modifier.weight(1f)) {
            screenState.SaveableStateProvider(screen) {
            when (screen) {
                "animals" -> AnimalProfilesScreen(animals, { screen = "dashboard" },
                    { selectedAnimal = null; screen = "animal" }, { selectedAnimal = it; screen = "animal" })
                "applications" -> StaffApplicationsScreen(journey, animals.animals, { screen = "dashboard" }, openReview)
                "settings" -> za.co.yebopets.feature.settings.SettingsScreen(settings, "ShelterStaff", signOut, switchWorkspace) { screen = "dashboard" }
                "animal" -> ManageAnimalScreen(animals, selectedAnimal, { screen = "animals" }) { screen = "animals" }
                "review" -> ReviewApplicationScreen(journey, selectedApplication,
                    animals.animals.firstOrNull { it.id == selectedApplication?.animalId },
                    { screen = "applications" }) { screen = "applications" }
                else -> StaffDashboardScreen(animals, journey, { screen = "animals" },
                    { screen = "applications" }, signOut, openReview)
            }
            }
        }
        if (screen in roots) {
            NavigationBar {
                listOf(
                    Triple("dashboard", AppIconKind.HOME, "Overview"),
                    Triple("animals", AppIconKind.HEART, "Animals"),
                    Triple("applications", AppIconKind.APPLICATION, "Applications"),
                    Triple("settings", AppIconKind.SETTINGS, "Settings"),
                ).forEach { (destination, icon, label) ->
                    NavigationBarItem(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        icon = {
                            val count = journey.applications.count { it.needsReview() }
                            BadgedBox(badge = {
                                if (destination == "applications" && count > 0) Badge { Text(count.toString()) }
                            }) { AppIcon(icon) }
                        },
                        label = { Text(za.co.yebopets.part3.T(label)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffDashboardScreen(
    animals: StaffModel,
    journey: JourneyModel,
    openAnimals: () -> Unit,
    openApplications: () -> Unit,
    signOut: () -> Unit,
    review: (AdoptionApplication) -> Unit,
) {
    val queue = applicationQueue(journey.applications, animals.animals)
    val needsReview = queue.review.size
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Coastal Paws", style = MaterialTheme.typography.headlineLarge)
                    Text(za.co.yebopets.part3.T("Shelter workspace"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(YeboInk), contentAlignment = Alignment.Center) {
                    Text("CP", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
        item { StatusPill("Shelter account verified", color = YeboTeal) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(animals.animals.count { it.status == "Published" }.toString(), "Published", MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                MetricCard(journey.applications.size.toString(), "Applications", MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(needsReview.toString(), "Needs review", MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                MetricCard(animals.animals.count { it.status == "Adopted" }.toString(), "Adopted", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
            }
        }
        item { Status(animals) }
        item { Status(journey) }
        item { SectionHeading("Quick actions") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StaffActionCard(AppIconKind.PLUS, "Animal profiles", "Add or update pets", openAnimals, Modifier.weight(1f))
                StaffActionCard(AppIconKind.APPLICATION, "Applications", androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.workspace_review_count, needsReview), openApplications, Modifier.weight(1f))
            }
        }
        if (queue.review.isNotEmpty()) {
            item { SectionHeading("Needs review") }
            items(queue.review.take(3), key = { it.id }) { application ->
                Card(
                    Modifier.fillMaxWidth().clickable { review(application) },
                    shape = LargeCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusPill(statusLabel(application.status), color = YeboSun)
                        Column(Modifier.weight(1f)) {
                            Text(application.animalId.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                            Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_3, friendlyDateTime(application.submittedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AppIcon(AppIconKind.BACK, Modifier.rotate(180f), size = 20.dp)
                    }
                }
            }
        }
        item { SecondaryAction("Sign out of shelter workspace", signOut, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun MetricCard(value: String, label: String, color: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = color), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(za.co.yebopets.part3.T(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StaffActionCard(icon: AppIconKind, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                AppIcon(icon, tint = YeboCoral, contentDescription = title)
            }
            Text(za.co.yebopets.part3.T(title), style = MaterialTheme.typography.titleMedium)
            Text(za.co.yebopets.part3.T(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnimalProfilesScreen(model: StaffModel, back: () -> Unit, add: () -> Unit, edit: (Animal) -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Animal profiles", back)
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item { Status(model) }
            items(model.animals, key = { it.id }) { animal ->
                Card(
                    Modifier.fillMaxWidth().clickable { edit(animal) },
                    shape = LargeCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AnimalImage(animal, Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(animal.name, style = MaterialTheme.typography.titleMedium)
                            Text(animal.breed, style = MaterialTheme.typography.bodySmall)
                            StatusPill(statusLabel(animal.status))
                        }
                        Text(za.co.yebopets.part3.T("Edit"), style = MaterialTheme.typography.labelLarge, color = YeboCoral)
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            PrimaryAction("Add animal", add, Modifier.fillMaxWidth().padding(16.dp), leadingContent = { AppIcon(AppIconKind.PLUS, tint = YeboInk) })
        }
    }
}

@Composable
private fun StaffApplicationsScreen(
    journey: JourneyModel,
    animals: List<Animal>,
    back: () -> Unit,
    review: (AdoptionApplication) -> Unit,
) {
    StaffApplicationList(journey.applications, animals, journey.busy, back, journey::loadStaff, review) { Status(journey) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StaffApplicationList(
    applications: List<AdoptionApplication>,
    animals: List<Animal>,
    busy: Boolean,
    back: () -> Unit,
    refresh: () -> Unit,
    review: (AdoptionApplication) -> Unit,
    status: @Composable () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val listState = rememberLazyListState()
    val queue = remember(applications, animals, query) { applicationQueue(applications, animals, query) }
    val pets = remember(animals) { animals.associateBy { it.id } }
    val reviewItems = if (filter == "Other applications") emptyList() else queue.review
    val otherItems = if (filter == "Needs review") emptyList() else queue.other
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Applications", back)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            YeboTextField(query, {
                query = it
                listState.requestScrollToItem(0)
            }, "Search by pet or reference")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                listOf("All" to "All", "Needs review" to "Needs review", "Other applications" to "Other").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = filter == value,
                        onClick = {
                            if (filter != value) {
                                filter = value
                                listState.requestScrollToItem(0)
                            }
                        },
                        modifier = Modifier.fillMaxHeight(),
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(za.co.yebopets.part3.T(label), maxLines = 2) }
                }
            }
            SecondaryAction("Refresh", refresh, enabled = !busy)
            status()
        }
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (!busy && reviewItems.isEmpty() && otherItems.isEmpty()) {
                item { Text(za.co.yebopets.part3.T(
                    if (query.isNotBlank()) "No applications match your search."
                    else if (filter == "Needs review") "No applications need review."
                    else "No applications to show."
                )) }
            }
            if (filter != "Other applications") {
                item(key = "review-heading") {
                    SectionHeading("${za.co.yebopets.part3.T("Needs review")} (${queue.review.size})")
                }
                items(reviewItems, key = { it.id }) { application ->
                    StaffApplicationCard(application, pets[application.animalId], review)
                }
            }
            if (filter != "Needs review" && otherItems.isNotEmpty()) {
                item(key = "other-heading") {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    SectionHeading("${za.co.yebopets.part3.T("Other applications")} (${queue.other.size})")
                }
                items(otherItems, key = { it.id }) { application ->
                    StaffApplicationCard(application, pets[application.animalId], review)
                }
            }
        }
    }
}

@Composable
private fun StaffApplicationCard(application: AdoptionApplication, animal: Animal?, review: (AdoptionApplication) -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { review(application) },
        shape = LargeCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (animal != null) AnimalImage(animal, Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(animal?.name ?: application.animalId.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.workspace_reference, application.id.take(8)),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_3, friendlyDateTime(application.submittedAt)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatusPill(statusLabel(application.status))
            }
            Text(za.co.yebopets.part3.T(if (application.needsReview()) "Review" else "View"),
                style = MaterialTheme.typography.labelLarge, color = YeboCoral)
        }
    }
}

@Composable
fun ManageAnimalScreen(model: StaffModel, original: Animal?, back: () -> Unit, done: () -> Unit) {
    var uploadedPhoto by remember { mutableStateOf(original?.photoUrl) }
    var name by remember { mutableStateOf(original?.name ?: "") }
    var breed by remember { mutableStateOf(original?.breed ?: "") }
    var age by remember { mutableStateOf((original?.ageMonths ?: 12).toString()) }
    var description by remember { mutableStateOf(original?.description ?: "") }
    var species by remember { mutableStateOf(original?.species ?: "Dog") }
    var size by remember { mutableStateOf(original?.size ?: "Medium") }
    var sex by remember { mutableStateOf(original?.sex ?: "Female") }
    var activity by remember { mutableStateOf(original?.activity ?: "Moderate") }
    var area by remember { mutableStateOf(original?.area ?: "Durban North") }
    var needsYard by remember { mutableStateOf(original?.needsYard ?: false) }
    var goodWithChildren by remember { mutableStateOf(original?.goodWithChildren ?: true) }
    var goodWithPets by remember { mutableStateOf(original?.goodWithPets ?: true) }
    var experienceRequired by remember { mutableStateOf((original?.experienceRequired ?: 0).toString()) }
    var care by remember { mutableStateOf(original?.care ?: "") }
    var status by remember { mutableStateOf(original?.status ?: "Draft") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader(if (original == null) "Add animal" else "Animal profile", back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            original?.let {
                AnimalImage(it.copy(photoUrl=uploadedPhoto), Modifier.fillMaxWidth().height(240.dp).clip(LargeCardShape))
                za.co.yebopets.part3.LocalMediaClient.current?.let { api ->
                    za.co.yebopets.part3.PhotoUpload(api,"animal",it.id) { url -> uploadedPhoto=url }
                }
            } ?: Text(za.co.yebopets.part3.S("p3_photo_create"))
            Status(model)
            SectionHeading("Animal details")
            YeboTextField(name, { name = it.take(60) }, "Name")
            YeboTextField(breed, { breed = it.take(80) }, "Breed")
            YeboTextField(age, { age = it.filter(Char::isDigit).take(3) }, "Age in months")
            ChoiceRow("Species", listOf("Dog", "Cat"), species) { species = it }
            ChoiceRow("Size", listOf("Small", "Medium", "Large"), size) { size = it }
            ChoiceRow("Sex", listOf("Female", "Male"), sex) { sex = it }
            ChoiceRow("Activity", Choices.activities, activity) { activity = it }
            ChoiceRow("Area", Choices.areas, area) { area = it }
            YeboTextField(description, { description = it.take(1000) }, "Description", singleLine = false, minLines = 5)
            YeboTextField(care, { care = it.take(500) }, "Care needs", singleLine = false, minLines = 3)
            SectionHeading("Compatibility")
            BooleanRow("Secure yard required", needsYard) { needsYard = it }
            BooleanRow("Suitable with children", goodWithChildren) { goodWithChildren = it }
            BooleanRow("Suitable with other pets", goodWithPets) { goodWithPets = it }
            ChoiceRow("Experience required", listOf("0", "1", "2"), experienceRequired) { experienceRequired = it }
            SectionHeading("Publication")
            ChoiceRow("Status", listOf("Draft", "Published", "Paused"), status) { status = it }
            Text(za.co.yebopets.part3.T("Published profiles are visible to adopters and included in matching."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            PrimaryAction(
                "Save animal profile",
                {
                    val id = original?.id ?: name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                    val animal = Animal(
                        id = id,
                        name = name.trim(),
                        species = species,
                        breed = breed.trim(),
                        ageMonths = age.toIntOrNull() ?: 0,
                        size = size,
                        sex = sex,
                        activity = activity,
                        area = area,
                        needsYard = needsYard,
                        goodWithChildren = goodWithChildren,
                        goodWithPets = goodWithPets,
                        experienceRequired = experienceRequired.toIntOrNull() ?: 0,
                        care = care.ifBlank { "Confirm daily care needs with shelter staff." },
                        description = description.trim(),
                        status = status,
                        photoUrl = uploadedPhoto,
                    )
                    model.save(animal, original != null, done)
                },
                Modifier.fillMaxWidth().padding(16.dp),
                enabled = !model.busy,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewApplicationScreen(
    model: JourneyModel,
    application: AdoptionApplication?,
    animal: Animal?,
    back: () -> Unit,
    done: () -> Unit,
) {
    if (application == null) {
        Page("Review application", back) { Text(za.co.yebopets.part3.T("Application unavailable.")) }
        return
    }

    LaunchedEffect(application.id) { model.loadDetails(application.id) }
    val details = model.details?.takeIf { it.application.id == application.id }
    val current = details?.application ?: application

    var decision by remember(application.id) {
        mutableStateOf(application.status.takeIf { it in listOf("UnderReview", "MoreInfo", "Approved", "Declined") } ?: "UnderReview")
    }
    var note by remember { mutableStateOf(application.staffNote.orEmpty()) }
    var appointment by remember { mutableStateOf(defaultDateTime(7, 10)) }
    var appointmentType by rememberSaveable(application.id) { mutableStateOf("Meet-and-greet") }
    var location by rememberSaveable(application.id) { mutableStateOf("") }
    var address by rememberSaveable(application.id) { mutableStateOf("") }
    var visitNotes by rememberSaveable(application.id) { mutableStateOf("") }
    var careInstructions by rememberSaveable(application.id) { mutableStateOf("") }
    var vaccinationNotes by rememberSaveable(application.id) { mutableStateOf("") }
    var collection by remember { mutableStateOf(defaultDateTime(10, 9)) }
    var followUp by remember { mutableStateOf(defaultDateTime(24, 9)) }

    Page("Review application", back) {
        Status(model)
        SecondaryAction("Refresh", { model.loadDetails(application.id) }, enabled = !model.busy)
        Card(
            shape = LargeCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (animal != null) AnimalImage(animal, Modifier.size(92.dp).clip(RoundedCornerShape(17.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(animal?.name ?: application.animalId.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge)
                    Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_3, friendlyDateTime(application.submittedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusPill(statusLabel(current.status))
                }
            }
        }
        SectionHeading("Application snapshot")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SnapshotCard(if (application.housingPermissionConfirmed) "Confirmed" else "Missing", "Housing permission", Modifier.weight(1f))
            SnapshotCard(if (application.longTermCareConfirmed) "Confirmed" else "Missing", "Long-term care", Modifier.weight(1f))
        }
        SectionHeading("Why this pet?")
        Text(application.motivation)
        application.additionalNotes?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        YeboTextField(note, { note = it.take(1000) }, "Staff note", singleLine = false, minLines = 3)
        SectionHeading("Decision")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "UnderReview" to "Under review",
                "MoreInfo" to "Request info",
                "Approved" to "Approve",
                "Declined" to "Decline",
            ).forEach { (value, label) ->
                ChoiceChip(label, decision == value, { decision = value }, enabled = !model.busy)
            }
        }
        PrimaryAction(
            "Save decision",
            { model.decide(application.id, decision, note, done) },
            Modifier.fillMaxWidth(),
            enabled = !model.busy && details != null && details.adoption == null,
        )

        SectionHeading("Appointments")
        if (details?.appointments?.isEmpty() == true) Text(za.co.yebopets.part3.T("No appointments scheduled."))
        details?.appointments?.forEach { StaffAppointment(it, model) }

        if (current.status == "Approved" && details?.adoption == null) {
            SectionHeading("New appointment")
            ChoiceRow("Visit type", listOf("Meet-and-greet", "Home check"), appointmentType, !model.busy) { appointmentType = it }
            DateTimePickerField("Appointment date", appointment, { appointment = it }, enabled = !model.busy)
            YeboTextField(location, { location = it.take(200) }, "Location", enabled = !model.busy)
            YeboTextField(address, { address = it.take(500) }, "Address", enabled = !model.busy)
            YeboTextField(visitNotes, { visitNotes = it.take(1000) }, "Visit notes", enabled = !model.busy, singleLine = false)
            SecondaryAction(
                "Save appointment",
                { model.schedule(application.id, AppointmentRequest(
                    if (appointmentType == "Home check") "HomeCheck" else "MeetAndGreet",
                    appointment.toApiTimestamp(), location.trim(), address.trim(), visitNotes.trim())) },
                Modifier.fillMaxWidth(),
                enabled = !model.busy && details != null && location.isNotBlank() && address.isNotBlank(),
            )
            SectionHeading("Finalise adoption")
            DateTimePickerField("Collection date", collection, { collection = it }, enabled = !model.busy)
            DateTimePickerField("Follow-up date", followUp, { followUp = it }, enabled = !model.busy)
            YeboTextField(careInstructions, { careInstructions = it.take(2000) }, "Care plan", enabled = !model.busy, singleLine = false, minLines = 3)
            YeboTextField(vaccinationNotes, { vaccinationNotes = it.take(1000) }, "Vaccination notes", enabled = !model.busy, singleLine = false)
            SecondaryAction(
                "Finalise approved adoption",
                { model.finalise(application.id, collection.toApiTimestamp(), followUp.toApiTimestamp(),
                    careInstructions, vaccinationNotes.ifBlank { null }, done) },
                Modifier.fillMaxWidth(),
                enabled = !model.busy && details != null && careInstructions.isNotBlank(),
            )
        }
        details?.adoption?.let { adoption ->
            SectionHeading("Aftercare")
            KeyValueRow("Collection date", friendlyDate(adoption.collectionDate))
            Text(adoption.careInstructions)
            adoption.vaccinationNotes?.let { Text(it) }
            za.co.yebopets.part3.LocalMediaClient.current?.let { client -> FollowUpHistory(client, details.followUps) }
        }
    }
}

@Composable
private fun SnapshotCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(za.co.yebopets.part3.T(value), style = MaterialTheme.typography.titleSmall)
            Text(za.co.yebopets.part3.T(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
