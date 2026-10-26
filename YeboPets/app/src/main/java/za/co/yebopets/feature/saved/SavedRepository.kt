package za.co.yebopets.feature.saved

import za.co.yebopets.core.Animal
import za.co.yebopets.core.ApiClient

class SavedRepository(private val client: ApiClient) {
    private val offline = client.ownerId?.let { za.co.yebopets.part3.OfflineSaved(client, it) }
    private val api = client.create(SavedApi::class.java)

    suspend fun load(): List<Animal> = offline?.load() ?: api.load(client.header())

    suspend fun save(id: String) {
        require(id.isNotBlank()) { "Choose a pet first." }
        if (offline != null) offline.set(id,true) else api.save(client.header(), id)
    }

    suspend fun remove(id: String) {
        require(id.isNotBlank()) { "Choose a pet first." }
        if (offline != null) offline.set(id,false) else api.remove(client.header(), id)
    }
}