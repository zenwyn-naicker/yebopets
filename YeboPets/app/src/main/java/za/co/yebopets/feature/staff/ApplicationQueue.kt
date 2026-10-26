package za.co.yebopets.feature.staff

import java.time.Instant
import java.time.OffsetDateTime
import za.co.yebopets.core.Animal
import za.co.yebopets.feature.journey.AdoptionApplication

internal fun AdoptionApplication.needsReview() = status == "Submitted" || status == "UnderReview"

internal data class ApplicationQueue(
    val review: List<AdoptionApplication>,
    val other: List<AdoptionApplication>,
)

/** Oldest waiting applications come first; decisions and information requests remain below them. */
internal fun applicationQueue(
    applications: List<AdoptionApplication>,
    animals: List<Animal>,
    query: String = "",
): ApplicationQueue {
    val names = animals.associate { it.id to it.name }
    val term = query.trim()
    val filtered = applications.filter { application ->
        term.isEmpty() || listOf(application.id, application.animalId, names[application.animalId].orEmpty())
            .any { it.contains(term, ignoreCase = true) }
    }
    val (review, other) = filtered.partition { it.needsReview() }
    fun timestamp(value: String): Instant = runCatching { OffsetDateTime.parse(value).toInstant() }.getOrDefault(Instant.EPOCH)
    return ApplicationQueue(
        review.sortedWith(compareBy<AdoptionApplication> { timestamp(it.submittedAt) }.thenBy { it.id }),
        other.sortedWith(compareByDescending<AdoptionApplication> { timestamp(it.updatedAt) }.thenBy { it.id }),
    )
}
