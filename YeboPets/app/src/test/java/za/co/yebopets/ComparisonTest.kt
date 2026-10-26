package za.co.yebopets

import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.feature.saved.ComparisonSelection

class ComparisonTest {
    @Test
    fun selectingAnExistingPetRemovesIt() {
        assertEquals(
            setOf("b"),
            ComparisonSelection.toggle(setOf("a", "b"), "a")
        )
    }

    @Test
    fun thirdPetIsAllowed() {
        assertEquals(
            setOf("a", "b", "c"),
            ComparisonSelection.toggle(setOf("a", "b"), "c")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun fourthPetIsRejected() {
        ComparisonSelection.toggle(setOf("a", "b", "c"), "d")
    }
}