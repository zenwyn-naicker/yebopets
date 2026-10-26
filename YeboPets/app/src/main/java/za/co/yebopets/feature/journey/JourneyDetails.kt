package za.co.yebopets.feature.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.ZoneId
import za.co.yebopets.core.*
import za.co.yebopets.part3.PrivatePhoto
import za.co.yebopets.part3.T

@Composable
fun AppointmentSummary(appointment: Appointment) {
    Text(T(if (appointment.type == "HomeCheck") "Home check" else "Meet-and-greet"), style = MaterialTheme.typography.titleSmall)
    StatusPill(statusLabel(appointment.status))
    Text(friendlyDateTime(appointment.scheduledAt))
    Text(appointment.locationName)
    Text(appointment.address)
    appointment.notes?.takeIf { it.isNotBlank() }?.let { Text(it) }
}

@Composable
fun AppointmentHistory(appointments: List<Appointment>) {
    SectionHeading("Appointments")
    if (appointments.isEmpty()) Text(T("No appointments scheduled."))
    appointments.forEach { appointment ->
        Card(Modifier.fillMaxWidth(), shape = LargeCardShape) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppointmentSummary(appointment)
            }
        }
    }
}

@Composable
fun StaffAppointment(appointment: Appointment, model: JourneyModel) {
    var editing by rememberSaveable(appointment.id) { mutableStateOf(false) }
    var date by remember(appointment.scheduledAt) {
        mutableStateOf(OffsetDateTime.parse(appointment.scheduledAt).atZoneSameInstant(ZoneId.of("Africa/Johannesburg")))
    }
    var location by remember(appointment.locationName) { mutableStateOf(appointment.locationName) }
    var address by remember(appointment.address) { mutableStateOf(appointment.address) }
    var notes by remember(appointment.notes) { mutableStateOf(appointment.notes.orEmpty()) }
    Card(Modifier.fillMaxWidth(), shape = LargeCardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppointmentSummary(appointment)
            if (appointment.status != "Completed") {
                SecondaryAction(if (editing) "Close" else "Reschedule", { editing = !editing }, enabled = !model.busy)
            }
            if (editing) {
                DateTimePickerField("Appointment date", date, { date = it }, enabled = !model.busy)
                YeboTextField(location, { location = it.take(200) }, "Location", enabled = !model.busy)
                YeboTextField(address, { address = it.take(500) }, "Address", enabled = !model.busy)
                YeboTextField(notes, { notes = it.take(1000) }, "Visit notes", enabled = !model.busy, singleLine = false)
                PrimaryAction("Save appointment", {
                    model.updateAppointment(appointment.applicationId, appointment.id,
                        AppointmentUpdateRequest("Scheduled", date.toApiTimestamp(), location, address, notes))
                }, enabled = !model.busy && location.isNotBlank() && address.isNotBlank())
            }
            if (appointment.status == "Scheduled") {
                SecondaryAction("Mark completed", {
                    model.updateAppointment(appointment.applicationId, appointment.id, AppointmentUpdateRequest("Completed"))
                }, enabled = !model.busy)
                SecondaryAction("Cancel visit", {
                    model.updateAppointment(appointment.applicationId, appointment.id, AppointmentUpdateRequest("Cancelled"))
                }, enabled = !model.busy)
            }
        }
    }
}

@Composable
fun FollowUpHistory(client: ApiClient, updates: List<FollowUp>) {
    SectionHeading("Follow-up history")
    if (updates.isEmpty()) Text(T("No follow-up updates yet."))
    updates.forEach { update ->
        Card(Modifier.fillMaxWidth(), shape = LargeCardShape) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill(wellbeingLabel(update.wellbeingStatus))
                Text(friendlyDateTime(update.submittedAt), style = MaterialTheme.typography.bodySmall)
                Text(update.notes)
                update.photoUrl?.let { url ->
                    PrivatePhoto(client, url, Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)))
                }
            }
        }
    }
}
