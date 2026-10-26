package za.co.yebopets.core

import kotlin.math.*

/** Matches the API's town-centre distance estimates; cached distances may belong to another search origin. */
object AnimalSearch {
    private val towns = mapOf(
        "Durban North" to (-29.7853 to 31.0413),
        "Berea" to (-29.8495 to 30.9934),
        "Umhlanga" to (-29.7260 to 31.0850),
        "Westville" to (-29.8277 to 30.9280),
    )

    fun distance(first: String, second: String): Double? {
        val a = towns[first] ?: return null
        val b = towns[second] ?: return null
        val latitude = Math.toRadians(b.first - a.first)
        val longitude = Math.toRadians(b.second - a.second)
        val value = sin(latitude / 2).pow(2) + cos(Math.toRadians(a.first)) *
            cos(Math.toRadians(b.first)) * sin(longitude / 2).pow(2)
        return round(6371 * 2 * asin(sqrt(value.coerceIn(0.0, 1.0))) * 10) / 10
    }

    fun cached(animals: List<Animal>, query: String, species: String, size: String, activity: String,
               sex: String, age: String, origin: String, radius: Int): List<Animal> {
        val search = query.trim()
        return animals.asSequence()
            .filter { it.status == "Published" }
            .filter { species == "Any" || it.species == species }
            .filter { size == "Any" || it.size == size }
            .filter { activity == "Any" || it.activity == activity }
            .filter { sex == "Any" || it.sex == sex }
            .filter { age == "Any" || age == when { it.ageMonths < 12 -> "Young"; it.ageMonths < 96 -> "Adult"; else -> "Senior" } }
            .filter { search.isEmpty() || it.name.contains(search, true) || it.breed.contains(search, true) }
            .mapNotNull { pet ->
                if (origin == "Any") pet.copy(distanceKm = null)
                else distance(origin, pet.area)?.takeIf { it <= radius }?.let { pet.copy(distanceKm = it) }
            }
            .sortedBy { it.name }
            .toList()
    }
}
