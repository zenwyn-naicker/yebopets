/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Get data from the internet
 * Type: [Source Code]
 * Available at: <https://developer.android.com/codelabs/basic-android-kotlin-compose-getting-data-internet> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Square, Inc.
 * Date: 2026
 * Title: Retrofit
 * Type: [Source Code]
 * Available at: <https://square.github.io/retrofit/> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Square, Inc.
 * Date: 2026
 * Title: OkHttp
 * Type: [Source Code]
 * Available at: <https://github.com/square/okhttp> [Accessed 9 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.core

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class Animal(
    val id: String,
    val name: String,
    val species: String,
    val breed: String,
    val ageMonths: Int,
    val size: String,
    val sex: String,
    val activity: String,
    val area: String,
    val needsYard: Boolean,
    val goodWithChildren: Boolean,
    val goodWithPets: Boolean,
    val experienceRequired: Int,
    val care: String,
    val description: String,
    val status: String = "Published",
    val distanceKm: Double? = null,
    val photoUrl: String? = null
)

object Choices {
    val species = listOf("Any", "Dog", "Cat")
    val sizes = listOf("Any", "Small", "Medium", "Large")
    val activities = listOf("Calm", "Moderate", "Active")
    val ages = listOf("Any", "Young", "Adult", "Senior")
    val areas = listOf("Durban North", "Berea", "Umhlanga", "Westville")
}

class ApiClient(
    baseUrl: String,
    val ownerId: String? = null,
    private val token: suspend () -> String
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> create(type: Class<T>): T = retrofit.create(type)

    fun mediaUrl(path: String): String {
        require(Regex("/api/media/[A-Za-z0-9-]+").matches(path)) { "Invalid media path." }
        return requireNotNull(retrofit.baseUrl().resolve(path.removePrefix("/"))).toString()
    }

    suspend fun header(): String {
        val value = token()
        check(value.isNotBlank()) { "No authenticated session." }
        return "Bearer $value"
    }
}

interface AnimalsApi {
    @GET("api/animals")
    suspend fun browse(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("species") species: String,
        @Query("size") size: String,
        @Query("activity") activity: String,
        @Query("sex") sex: String,
        @Query("ageGroup") age: String,
        @Query("area") area: String,
        @Query("radiusKm") radius: Int
    ): List<Animal>

    @GET("api/animals/{id}")
    suspend fun details(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Animal
}

class AnimalsRepository(private val client: ApiClient) {
    private val api = client.create(AnimalsApi::class.java)

    suspend fun browse(
        query: String,
        species: String,
        size: String,
        activity: String,
        sex: String,
        age: String,
        area: String,
        radius: Int
    ): List<Animal> {
        require(query.trim().length <= 80) {
            "Search must be at most 80 characters."
        }

        return try {
            api.browse(client.header(), query.trim(), species, size, activity, sex, age, area, radius)
                .also { za.co.yebopets.part3.Local.cache(client.ownerId, it) }
        } catch (e: Exception) {
            if(e !is java.io.IOException && e !is com.google.firebase.FirebaseNetworkException) throw e
            val uid=client.ownerId ?: throw e
            AnimalSearch.cached(za.co.yebopets.part3.Local.pets(uid), query, species, size, activity, sex, age, area, radius)
        }
    }

    suspend fun details(id: String): Animal =
        try { api.details(client.header(), id).also { za.co.yebopets.part3.Local.cache(client.ownerId, listOf(it)) } }
        catch (e: Exception) {
            if(e !is java.io.IOException && e !is com.google.firebase.FirebaseNetworkException) throw e
            val uid=client.ownerId ?: throw e
            za.co.yebopets.part3.Local.pets(uid).find { it.id == id } ?: throw e
        }
}
