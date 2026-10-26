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

package za.co.yebopets.feature.journey

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.co.yebopets.core.*
import za.co.yebopets.ui.theme.*

@Composable
fun ApplyScreen(client: ApiClient, animal: Animal, back: () -> Unit, done: () -> Unit) {
    val model: JourneyModel = viewModel { JourneyModel(JourneyRepository(client)) }
    var motivation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var permission by remember { mutableStateOf(false) }
    var care by remember { mutableStateOf(false) }
    var submitted by remember(animal.id) { mutableStateOf(false) }
    val activity=androidx.activity.compose.LocalActivity.current
    var draftReady by remember(animal.id) { mutableStateOf(false) }
    LaunchedEffect(animal.id) {
        client.ownerId?.let { uid -> za.co.yebopets.part3.Drafts.load(uid,animal.id)?.let {
            motivation=it.motivation; notes=it.additionalNotes.orEmpty()
            permission=it.housingPermissionConfirmed; care=it.longTermCareConfirmed
        } }
        draftReady=true
    }
    val lastDraft by rememberUpdatedState(ApplicationRequest(motivation,permission,care,notes))
    DisposableEffect(animal.id) {
        onDispose {
            if(draftReady && !submitted) client.ownerId?.let { uid ->
                (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch(kotlinx.coroutines.Dispatchers.IO) {
                    za.co.yebopets.part3.Drafts.save(uid,animal.id,lastDraft)
                }
            }
        }
    }
    LaunchedEffect(motivation,notes,permission,care,draftReady) {
        if(draftReady) client.ownerId?.let { uid ->
            za.co.yebopets.part3.Drafts.save(uid,animal.id,ApplicationRequest(motivation,permission,care,notes))
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_0, animal.name), back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth(), shape = LargeCardShape, colors = CardDefaults.cardColors(containerColor = YeboPaper), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnimalImage(animal, Modifier.size(78.dp).clip(RoundedCornerShape(16.dp)))
                    Column { Text(animal.name, style = MaterialTheme.typography.titleLarge); Text(animal.breed, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.weight(1f))
                    StatusPill("Available")
                }
            }
            Status(model)
            Text(za.co.yebopets.part3.S("p3_draft_help"))
            Text(za.co.yebopets.part3.T("Tell the shelter about the home you can offer."))
            YeboTextField(motivation, { if (it.length <= 1000) motivation = it }, androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_1, animal.name), singleLine = false, minLines = 5)
            YeboTextField(notes, { if (it.length <= 500) notes = it }, "Anything else the shelter should know?", singleLine = false, minLines = 3)
            BooleanRow("I have permission to keep a pet at my home", permission) { permission = it }
            BooleanRow("I accept the long-term care and cost commitment", care) { care = it }
            Text(za.co.yebopets.part3.T("Complete your profile and confirm both commitments before applying."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            PrimaryAction(
                "Submit application",
                { model.submit(animal.id, motivation, permission, care, notes.ifBlank { null }, { submitted=true; done() }) },
                Modifier.fillMaxWidth().padding(16.dp),
                enabled = !model.busy,
            )
        }
    }
}

@Composable
fun ApplicationsScreen(
    client: ApiClient,
    openApplication: (AdoptionApplication) -> Unit,
    openAftercare: (Adoption) -> Unit,
) {
    val model: JourneyModel = viewModel { JourneyModel(JourneyRepository(client)) }
    LaunchedEffect(Unit) { model.loadAdopter() }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(za.co.yebopets.part3.T("Applications"), style = MaterialTheme.typography.headlineLarge)
                Text(za.co.yebopets.part3.T("Follow every step without leaving the app."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Status(model) }
        item { SectionHeading("In progress") }
        if (!model.busy && model.applications.isEmpty()) {
            item { EmptyJourneyCard("No applications yet", "Open a pet from Discover when you are ready to apply.") }
        }
        items(model.applications, key = { it.id }) { application ->
            ApplicationCard(application) { openApplication(application) }
        }
        item { SectionHeading("Your adoption journey") }
        if (!model.busy && model.adoptions.isEmpty()) {
            item { Text(za.co.yebopets.part3.T("Aftercare becomes available when an approved adoption is finalised."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(model.adoptions, key = { it.id }) { adoption ->
            AftercareSummaryCard(adoption) { openAftercare(adoption) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ApplicationCard(application: AdoptionApplication, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = LargeCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(application.animalId.replaceFirstChar { it.uppercase() }, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                StatusPill(statusLabel(application.status))
            }
            Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_3, friendlyDateTime(application.submittedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(application.motivation, maxLines = 2)
            application.staffNote?.takeIf { it.isNotBlank() }?.let {
                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_5, it), style = MaterialTheme.typography.bodySmall)
            }
            Text(za.co.yebopets.part3.T("View progress  →"), style = MaterialTheme.typography.labelLarge, color = YeboCoral)
        }
    }
}

@Composable
private fun AftercareSummaryCard(adoption: Adoption, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = LargeCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                AppIcon(AppIconKind.CHECK, tint = YeboTeal, contentDescription = "Adopted")
            }
            Column(Modifier.weight(1f)) {
                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_9, adoption.animalId.replaceFirstChar { it.uppercase() }), style = MaterialTheme.typography.titleMedium)
                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_6, friendlyDate(adoption.followUpDueAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("→", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun EmptyJourneyCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth(), shape = LargeCardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(za.co.yebopets.part3.T(title), style = MaterialTheme.typography.titleMedium)
            Text(za.co.yebopets.part3.T(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ApplicationTrackerScreen(client: ApiClient, application: AdoptionApplication, back: () -> Unit) {
    val model: JourneyModel = viewModel(key = "application-${application.id}") { JourneyModel(JourneyRepository(client)) }
    LaunchedEffect(application.id) { model.loadDetails(application.id) }
    val details = model.details?.takeIf { it.application.id == application.id }
    val current = details?.application ?: application
    val status = current.status
    val reviewComplete = status in listOf("Approved", "Declined")
    val reviewActive = status in listOf("Submitted", "UnderReview", "MoreInfo")

    Page("${application.animalId.replaceFirstChar { it.uppercase() }}'s application", back) {
        Status(model)
        SecondaryAction("Refresh", { model.loadDetails(application.id) }, enabled = !model.busy)
        Card(
            shape = LargeCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            border = BorderStroke(1.dp, YeboInk),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(statusLabel(status), color = YeboSun)
                Text(
                    za.co.yebopets.part3.T(when (status) {
                        "Approved" -> if (details?.adoption != null) "Adoption finalised" else "Your application has been approved"
                        "Declined" -> "The shelter has completed its review"
                        "MoreInfo" -> "The shelter needs more information"
                        else -> "Your application is with the shelter"
                    }),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_4, friendlyDateTime(current.updatedAt)), style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionHeading("Progress")
        TimelineStep("Application submitted", friendlyDateTime(application.submittedAt), complete = true, active = false)
        TimelineStep("Shelter review", if (reviewActive) "Your answers are being reviewed" else "Review completed", complete = reviewComplete, active = reviewActive)
        val visitComplete = details?.appointments?.any { it.status == "Completed" } == true
        TimelineStep("Meet & greet", if (visitComplete) "Visit completed" else if (status == "Approved") "The shelter will confirm the next step" else "Available after approval", complete = visitComplete, active = status == "Approved" && !visitComplete)
        TimelineStep("Final decision", if (details?.adoption != null) "Adoption finalised" else if (status == "Declined") "Review completed" else "The shelter confirms the outcome", complete = status == "Declined" || details?.adoption != null, active = false)
        details?.let { AppointmentHistory(it.appointments) }
        SectionHeading("Your application")
        Text(application.motivation)
        current.staffNote?.takeIf { it.isNotBlank() }?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = LargeCardShape) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(za.co.yebopets.part3.T("Shelter note"), style = MaterialTheme.typography.titleSmall)
                    Text(it)
                }
            }
        }
    }
}

@Composable
private fun TimelineStep(title: String, subtitle: String, complete: Boolean, active: Boolean) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(
                when {
                    complete -> YeboTeal
                    active -> YeboSun
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (complete) AppIcon(AppIconKind.CHECK, size = 15.dp, tint = YeboInk, contentDescription = "Complete")
            else Text(if (active) "•" else "", style = MaterialTheme.typography.titleMedium)
        }
        Column {
            Text(za.co.yebopets.part3.T(title), style = MaterialTheme.typography.titleSmall)
            Text(za.co.yebopets.part3.T(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AftercareScreen(client: ApiClient, adoption: Adoption, back: () -> Unit, startFollowUp: () -> Unit) {
    val model: JourneyModel = viewModel(key = "aftercare-${adoption.id}") { JourneyModel(JourneyRepository(client)) }
    LaunchedEffect(adoption.id) { model.loadFollowUps(adoption.id) }
    Page("${adoption.animalId.replaceFirstChar { it.uppercase() }}'s aftercare", back) {
        Status(model)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = LargeCardShape) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(za.co.yebopets.part3.T("A happy start at home"), style = MaterialTheme.typography.headlineSmall)
                Text(za.co.yebopets.part3.T("Keep the care plan nearby and share an update when the shelter checks in."))
            }
        }
        AftercareDetail("Collection date", friendlyDate(adoption.collectionDate), "Your pet's homecoming date")
        AftercareDetail("Next follow-up", friendlyDate(adoption.followUpDueAt), "Share a wellbeing update with the shelter")
        AftercareDetail("Care plan", adoption.careInstructions, "Continue the recommended routine")
        adoption.vaccinationNotes?.takeIf { it.isNotBlank() }?.let {
            AftercareDetail("Vaccination notes", it, "Information recorded by the shelter")
        }
        PrimaryAction("Share a follow-up", startFollowUp, Modifier.fillMaxWidth())
        SecondaryAction("Refresh", { model.loadFollowUps(adoption.id) }, enabled = !model.busy)
        FollowUpHistory(client, model.followUps.filter { it.adoptionId == adoption.id })
    }
}

@Composable
private fun AftercareDetail(title: String, value: String, detail: String) {
    Card(
        shape = LargeCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                AppIcon(AppIconKind.CALENDAR, tint = YeboCoral, contentDescription = title)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(za.co.yebopets.part3.T(title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium)
                Text(za.co.yebopets.part3.T(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FollowUpScreen(client: ApiClient, adoption: Adoption, back: () -> Unit, done: () -> Unit) {
    val model: JourneyModel = viewModel { JourneyModel(JourneyRepository(client)) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var wellbeing by remember { mutableStateOf("Good") }
    var notes by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Share an update", back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_8, adoption.animalId.replaceFirstChar { it.uppercase() }), style = MaterialTheme.typography.headlineMedium)
            Status(model)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Good", "Concern", "NeedsAssistance").forEach { value ->
                    ChoiceChip(wellbeingLabel(value), wellbeing == value, { wellbeing = value }, Modifier.weight(1f))
                }
            }
            za.co.yebopets.part3.PhotoUpload(client,"followup",adoption.id) { photoUrl=it }
            YeboTextField(notes, { if (it.length <= 1000) notes = it }, "Your update", singleLine = false, minLines = 6)
            Text(za.co.yebopets.part3.T("Tell the shelter about routines, appetite, behaviour, or anything you would like help with."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            PrimaryAction(
                "Send update",
                { model.followUp(adoption.id, wellbeing, notes, done, photoUrl) },
                Modifier.fillMaxWidth().padding(16.dp),
                enabled = !model.busy,
            )
        }
    }
}

@Composable
fun FollowUpSuccessScreen(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(YeboTeal), contentAlignment = Alignment.Center) {
            AppIcon(AppIconKind.CHECK, tint = YeboInk, size = 38.dp, contentDescription = "Update sent")
        }
        Spacer(Modifier.height(20.dp))
        Text(za.co.yebopets.part3.T("Update sent"), style = MaterialTheme.typography.headlineLarge)
        Text(za.co.yebopets.part3.T("Thanks for helping Coastal Paws Rescue support your pet's new beginning."),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryAction("Back to applications", onDone, Modifier.fillMaxWidth())
    }
}
