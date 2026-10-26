package za.co.yebopets.feature.settings

import retrofit2.http.*
import za.co.yebopets.core.ApiClient

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val roles: List<String>? = null
)

data class UserSettings(
    val searchRadiusKm: Int = 25,
    val preferredSpecies: String = "Any",
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val statusAlertsEnabled: Boolean = true,
    val appointmentRemindersEnabled: Boolean = true,
    val syncOnMobileData: Boolean = false,
    val biometricsEnabled: Boolean = false
) {
    fun isValid(): Boolean =
        language in listOf("en","zu") && searchRadiusKm in 5..50 &&
            preferredSpecies in listOf("Any", "Dog", "Cat")
}

interface SettingsApi {
    @GET("api/me/access")
    suspend fun access(@Header("Authorization") auth: String): UserAccount

    @GET("api/me")
    suspend fun me(
        @Header("Authorization") auth: String,
        @Query("requestedRole") requestedRole: String
    ): UserAccount

    @GET("api/me/settings")
    suspend fun get(@Header("Authorization") auth: String): UserSettings

    @PUT("api/me/settings")
    suspend fun put(
        @Header("Authorization") auth: String,
        @Body settings: UserSettings
    ): UserSettings
}

class SettingsRepository(private val client: ApiClient, private val requestedRole: String) {
    private val api = client.create(SettingsApi::class.java)

    suspend fun access(): UserAccount = cached("access", UserAccount::class.java) { api.access(client.header()) }

    suspend fun account(): UserAccount = cached("account",UserAccount::class.java) { api.me(client.header(), requestedRole) }

    private suspend fun <T> cached(key: String, type: Class<T>, fetch: suspend () -> T): T {
        val uid=client.ownerId ?: return fetch()
        val prefs=za.co.yebopets.part3.Local.prefs(uid)
        return try { fetch().also { prefs.edit().putString(key, com.google.gson.Gson().toJson(it)).apply() } }
        catch (e: Exception) {
            if(e !is java.io.IOException && e !is com.google.firebase.FirebaseNetworkException) throw e
            prefs.getString(key,null)?.let { com.google.gson.Gson().fromJson(it,type) } ?: throw e }
    }

    suspend fun load(): UserSettings = cached("settings",UserSettings::class.java) { api.get(client.header()) }

    suspend fun save(settings: UserSettings): UserSettings {
        require(settings.isValid()) { "Choose a valid species and radius." }
        return api.put(client.header(), settings).also { result ->
            client.ownerId?.let { uid -> za.co.yebopets.part3.Local.prefs(uid).edit()
                .putString("settings", com.google.gson.Gson().toJson(result))
                .putBoolean("mobile",result.syncOnMobileData).apply() }
        }
    }
}