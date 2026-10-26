package za.co.yebopets.feature.staff

import retrofit2.http.*
import za.co.yebopets.core.Animal
import za.co.yebopets.core.ApiClient

interface StaffApi {
    @GET("api/staff/animals")
    suspend fun animals(@Header("Authorization") auth: String): List<Animal>

    @POST("api/animals")
    suspend fun create(@Header("Authorization") auth: String, @Body animal: Animal): Animal

    @PATCH("api/animals/{id}")
    suspend fun update(@Header("Authorization") auth: String, @Path("id") id: String, @Body animal: Animal): Animal
}

class StaffRepository(private val client: ApiClient) {
    private val api = client.create(StaffApi::class.java)
    suspend fun animals() = api.animals(client.header())
    suspend fun save(animal: Animal, existing: Boolean) = if (existing) api.update(client.header(), animal.id, animal) else api.create(client.header(), animal)
}