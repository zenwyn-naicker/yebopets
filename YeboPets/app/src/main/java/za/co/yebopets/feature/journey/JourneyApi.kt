package za.co.yebopets.feature.journey

import retrofit2.http.*
import za.co.yebopets.core.ApiClient

data class AdoptionApplication(
    val id: String,
    val animalId: String,
    val adopterId: String,
    val status: String,
    val motivation: String,
    val housingPermissionConfirmed: Boolean,
    val longTermCareConfirmed: Boolean,
    val additionalNotes: String?,
    val staffNote: String?,
    val submittedAt: String,
    val updatedAt: String
)

data class ApplicationRequest(
    val motivation: String,
    val housingPermissionConfirmed: Boolean,
    val longTermCareConfirmed: Boolean,
    val additionalNotes: String?
)

data class ApplicationDecisionRequest(val status: String, val staffNote: String?)

data class Appointment(
    val id: String,
    val applicationId: String,
    val type: String,
    val scheduledAt: String,
    val locationName: String,
    val address: String,
    val notes: String?,
    val status: String
)

data class AppointmentRequest(
    val type: String,
    val scheduledAt: String,
    val locationName: String,
    val address: String,
    val notes: String?
)

data class AppointmentUpdateRequest(
    val status: String,
    val scheduledAt: String? = null,
    val locationName: String? = null,
    val address: String? = null,
    val notes: String? = null,
)

data class Adoption(
    val id: String,
    val applicationId: String,
    val animalId: String,
    val adopterId: String,
    val finalisedAt: String,
    val collectionDate: String,
    val careInstructions: String,
    val vaccinationNotes: String?,
    val followUpDueAt: String
)

data class AdoptionRequest(
    val applicationId: String,
    val collectionDate: String,
    val careInstructions: String,
    val vaccinationNotes: String?,
    val followUpDueAt: String
)

data class FollowUpRequest(val wellbeingStatus: String, val notes: String, val photoUrl: String? = null)

data class FollowUp(
    val id: String, val adoptionId: String, val adopterId: String,
    val wellbeingStatus: String, val notes: String, val submittedAt: String, val photoUrl: String?,
)

data class ApplicationDetails(
    val application: AdoptionApplication,
    val appointments: List<Appointment>,
    val adoption: Adoption?,
    val followUps: List<FollowUp>,
)

interface JourneyApi {
    @GET("api/applications/{id}")
    suspend fun details(@Header("Authorization") auth: String, @Path("id") id: String): ApplicationDetails

    @GET("api/adoptions/{id}/follow-ups")
    suspend fun followUps(@Header("Authorization") auth: String, @Path("id") id: String): List<FollowUp>

    @POST("api/animals/{animalId}/applications")
    suspend fun submit(@Header("Authorization") auth: String, @Path("animalId") animalId: String, @Body request: ApplicationRequest): AdoptionApplication

    @GET("api/applications/me")
    suspend fun mine(@Header("Authorization") auth: String): List<AdoptionApplication>

    @GET("api/staff/applications")
    suspend fun staffApplications(@Header("Authorization") auth: String): List<AdoptionApplication>

    @PATCH("api/applications/{id}/decision")
    suspend fun decide(@Header("Authorization") auth: String, @Path("id") id: String, @Body request: ApplicationDecisionRequest): AdoptionApplication

    @POST("api/applications/{id}/appointments")
    suspend fun schedule(@Header("Authorization") auth: String, @Path("id") id: String, @Body request: AppointmentRequest): Appointment

    @PATCH("api/appointments/{id}")
    suspend fun updateAppointment(@Header("Authorization") auth: String, @Path("id") id: String, @Body request: AppointmentUpdateRequest): Appointment

    @POST("api/adoptions")
    suspend fun finalise(@Header("Authorization") auth: String, @Body request: AdoptionRequest): Adoption

    @GET("api/adoptions/me")
    suspend fun adoptions(@Header("Authorization") auth: String): List<Adoption>

    @POST("api/adoptions/{id}/follow-ups")
    suspend fun followUp(@Header("Authorization") auth: String, @Path("id") id: String, @Body request: FollowUpRequest)
}

class JourneyRepository(private val client: ApiClient) {
    private val api = client.create(JourneyApi::class.java)

    suspend fun submit(animalId: String, motivation: String, permission: Boolean, care: Boolean, notes: String?) =
        api.submit(client.header(), animalId, ApplicationRequest(motivation.trim(), permission, care, notes?.trim())).also {
            client.ownerId?.let { uid -> za.co.yebopets.part3.Drafts.delete(uid,animalId) }
        }

    suspend fun mine() = api.mine(client.header())
    suspend fun staffApplications() = api.staffApplications(client.header())
    suspend fun details(id: String) = api.details(client.header(), id)
    suspend fun followUps(id: String) = api.followUps(client.header(), id)
    suspend fun decide(id: String, status: String, note: String?) = api.decide(client.header(), id, ApplicationDecisionRequest(status, note?.trim()))
    suspend fun schedule(id: String, request: AppointmentRequest) = api.schedule(client.header(), id, request)
    suspend fun updateAppointment(id: String, request: AppointmentUpdateRequest) = api.updateAppointment(client.header(), id, request)
    suspend fun finalise(request: AdoptionRequest) = api.finalise(client.header(), request)
    suspend fun adoptions() = api.adoptions(client.header())
    suspend fun followUp(id: String, status: String, notes: String, photoUrl:String? = null) = api.followUp(client.header(), id, FollowUpRequest(status, notes.trim(),photoUrl))
}
