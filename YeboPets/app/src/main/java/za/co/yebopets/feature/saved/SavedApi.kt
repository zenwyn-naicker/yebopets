package za.co.yebopets.feature.saved

import retrofit2.http.*
import za.co.yebopets.core.Animal

interface SavedApi {
    @GET("api/adopters/me/saved")
    suspend fun load(@Header("Authorization") auth: String): List<Animal>

    @PUT("api/adopters/me/saved/{id}")
    suspend fun save(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    )

    @DELETE("api/adopters/me/saved/{id}")
    suspend fun remove(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    )
}