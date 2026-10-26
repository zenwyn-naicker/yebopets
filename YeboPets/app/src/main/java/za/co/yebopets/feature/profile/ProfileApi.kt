package za.co.yebopets.feature.profile

import retrofit2.http.*
import za.co.yebopets.core.ApiClient

data class AdopterProfile(
    val phone: String = "",
    val area: String = "Durban North",
    val housing: String = "House",
    val hasSecureYard: Boolean = false,
    val hasChildren: Boolean = false,
    val existingPets: Int = 0,
    val experience: Int = 0,
    val activity: String = "Moderate",
    val preferredSpecies: String = "Any",
    val preferredSize: String = "Any",
    val preferredAge: String = "Any",
    val maxDistanceKm: Int = 25,
    val careReady: Boolean = false
) {
    fun validate(): String? = when {
        !Regex("^\\+?[0-9]{9,15}$").matches(phone) ->
            "Enter 9–15 phone digits, optionally starting with +."

        area !in listOf("Durban North", "Berea", "Umhlanga", "Westville") ->
            "Choose a supported area."

        housing !in listOf("House", "Apartment", "Townhouse") ->
            "Choose a housing type."

        existingPets !in 0..5 ->
            "Existing pets must be between 0 and 5."

        experience !in 0..2 ->
            "Choose an experience level."

        activity !in listOf("Calm", "Moderate", "Active") ->
            "Choose an activity level."

        preferredSpecies !in listOf("Any", "Dog", "Cat") ->
            "Choose a species."

        preferredSize !in listOf("Any", "Small", "Medium", "Large") ->
            "Choose a size."

        preferredAge !in listOf("Any", "Young", "Adult", "Senior") ->
            "Choose an age."

        maxDistanceKm !in 5..50 ->
            "Distance must be between 5 and 50 km."

        !careReady ->
            "Confirm that you are ready for long-term care."

        else -> null
    }
}

data class MatchFactor(
    val name: String,
    val points: Int,
    val maximum: Int,
    val reason: String
)

data class MatchResult(
    val animalId: String,
    val overallScore: Int,
    val distanceKm: Double,
    val factors: List<MatchFactor>,
    val advice: String
)

interface ProfileApi {
    @GET("api/adopters/me/profile")
    suspend fun get(@Header("Authorization") auth: String): AdopterProfile

    @PUT("api/adopters/me/profile")
    suspend fun put(
        @Header("Authorization") auth: String,
        @Body profile: AdopterProfile
    ): AdopterProfile

    @GET("api/animals/{id}/match")
    suspend fun match(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): MatchResult
}

class ProfileRepository(private val client: ApiClient) {
    private val api = client.create(ProfileApi::class.java)

    suspend fun load(): AdopterProfile = api.get(client.header())

    suspend fun save(profile: AdopterProfile): AdopterProfile {
        profile.validate()?.let {
            throw IllegalArgumentException(it)
        }

        return api.put(client.header(), profile)
    }

    suspend fun match(id: String): MatchResult =
        api.match(client.header(), id)
}