package za.co.yebopets

import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.feature.journey.AdoptionApplication
import za.co.yebopets.feature.staff.applicationQueue

class ApplicationQueueTest {
    private fun application(id: String, status: String, date: String) = AdoptionApplication(
        id, "luna", "owner", status, "A caring home", true, true, null, null, date, date)

    @Test fun waitingApplicationsComeFirstOldestFirstAndOtherStatusesStaySeparate() {
        val queue = applicationQueue(listOf(
            application("approved", "Approved", "2026-10-20T10:00:00Z"),
            application("new", "Submitted", "2026-10-22T10:00:00Z"),
            application("old", "UnderReview", "2026-10-18T10:00:00Z"),
            application("waiting", "MoreInfo", "2026-10-21T10:00:00Z"),
            application("declined", "Declined", "2026-10-19T10:00:00Z")), emptyList())
        assertEquals(listOf("old", "new"), queue.review.map { it.id })
        assertEquals(listOf("waiting", "approved", "declined"), queue.other.map { it.id })
    }

    @Test fun searchFindsReferencesAndTrimsPetSearchAcrossBothSections() {
        val items = listOf(application("reference-1", "Submitted", "2026-10-22T10:00:00Z"),
            application("reference-2", "Approved", "2026-10-22T10:00:00Z"))
        val byPet = applicationQueue(items, emptyList(), "  LUNA  ")
        assertEquals(1, byPet.review.size)
        assertEquals(1, byPet.other.size)
        val byReference = applicationQueue(items, emptyList(), "reference-2")
        assertTrue(byReference.review.isEmpty())
        assertEquals("reference-2", byReference.other.single().id)
        assertTrue(applicationQueue(items, emptyList(), "missing").review.isEmpty())
    }

    @Test fun waitingOrderComparesInstantsAcrossTimeZones() {
        val queue = applicationQueue(listOf(
            application("later", "Submitted", "2026-10-22T08:30:00Z"),
            application("earlier", "Submitted", "2026-10-22T10:00:00+02:00")), emptyList())
        assertEquals(listOf("earlier", "later"), queue.review.map { it.id })
    }
}
