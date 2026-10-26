package za.co.yebopets

import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.core.*

class AnimalSearchTest {
    private val pet = Animal("nearby", "Luna", "Dog", "Mixed", 24, "Medium", "Female", "Moderate",
        "Berea", false, true, true, 0, "Care", "Nearby pet", distanceKm = 999.0)

    private fun search(origin: String, radius: Int, query: String = "") = AnimalSearch.cached(
        listOf(pet, pet.copy(id = "draft", status = "Draft")), query, "Any", "Any", "Any", "Any", "Any", origin, radius)

    @Test fun nearbyTownRemainsVisibleOfflineAndDistanceIsRecalculated() {
        val result = search("Durban North", 25)
        assertEquals(listOf("nearby"), result.map { it.id })
        assertEquals(8.5, result.single().distanceKm!!, 0.2)
        assertTrue(search("Durban North", 5).isEmpty())
    }

    @Test fun changingOriginAndTrimmingSearchMatchesApiBehaviour() {
        assertEquals(0.0, search("Berea", 5, "  luna  ").single().distanceKm!!, 0.0)
        assertNull(search("Any", 5).single().distanceKm)
        assertTrue(search("Any", 50, "not a pet").isEmpty())
    }
}
