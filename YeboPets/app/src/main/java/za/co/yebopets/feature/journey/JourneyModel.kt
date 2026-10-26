package za.co.yebopets.feature.journey

import androidx.compose.runtime.*
import za.co.yebopets.core.TaskModel

class JourneyModel(private val repository: JourneyRepository) : TaskModel() {
    var applications by mutableStateOf<List<AdoptionApplication>>(emptyList())
        private set
    var adoptions by mutableStateOf<List<Adoption>>(emptyList())
        private set
    var details by mutableStateOf<ApplicationDetails?>(null)
        private set
    var followUps by mutableStateOf<List<FollowUp>>(emptyList())
        private set

    fun loadDetails(id: String) = task("Load application details") {
        details = repository.details(id)
    }

    fun loadFollowUps(id: String) = task("Load follow-up history") {
        followUps = emptyList()
        followUps = repository.followUps(id)
    }

    fun loadAdopter() = task("Load adoption journey") {
        applications = repository.mine()
        adoptions = repository.adoptions()
    }

    fun loadStaff() = task("Load shelter applications") {
        applications = repository.staffApplications()
    }

    fun submit(animalId: String, motivation: String, permission: Boolean, care: Boolean, notes: String?, done: () -> Unit) = task("Submit application") {
        require(motivation.trim().length in 20..1000) { "Motivation must be 20–1000 characters." }
        require(permission && care) { "Confirm both care commitments." }
        repository.submit(animalId, motivation, permission, care, notes)
        notice = "Application submitted."
        done()
    }

    fun decide(id: String, status: String, note: String?, done: () -> Unit) = task("Review application") {
        repository.decide(id, status, note)
        loadStaffData()
        done()
    }

    fun schedule(id: String, request: AppointmentRequest) = task("Schedule appointment") {
        require(request.locationName.isNotBlank() && request.address.isNotBlank()) { "Enter a location and address." }
        repository.schedule(id, request)
        details = repository.details(id)
        notice = "Appointment saved."
    }

    fun updateAppointment(applicationId: String, id: String, request: AppointmentUpdateRequest) = task("Update appointment") {
        repository.updateAppointment(id, request)
        details = repository.details(applicationId)
        notice = "Appointment saved."
    }

    fun finalise(id: String, collection: String, followUp: String, care: String, vaccinations: String?, done: () -> Unit) = task("Finalise adoption") {
        require(care.isNotBlank()) { "Enter care instructions." }
        repository.finalise(AdoptionRequest(id, collection, care.trim(), vaccinations?.trim(), followUp))
        loadStaffData()
        notice = "Adoption finalised."
        done()
    }

    fun followUp(id: String, status: String, notes: String, done: () -> Unit, photoUrl: String? = null) = task("Send follow-up") {
        require(notes.trim().length in 10..1000) { "Update must be 10–1000 characters." }
        repository.followUp(id, status, notes,photoUrl)
        notice = "Follow-up sent."
        done()
    }

    private suspend fun loadStaffData() {
        applications = repository.staffApplications()
    }
}
