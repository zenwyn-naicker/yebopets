package za.co.yebopets

import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.feature.profile.AdopterProfile

class ProfileTest {
    @Test
    fun validReadyProfileIsAccepted() {
        assertNull(
            AdopterProfile(
                phone = "+27821234567",
                careReady = true
            ).validate()
        )
    }

    @Test
    fun invalidPhoneIsRejected() {
        assertNotNull(
            AdopterProfile(phone = "abc", careReady = true).validate()
        )
    }

    @Test
    fun readinessIsRequired() {
        assertNotNull(
            AdopterProfile(
                phone = "+27821234567",
                careReady = false
            ).validate()
        )
    }

    @Test
    fun invalidDistanceIsRejected() {
        assertNotNull(
            AdopterProfile(
                phone = "+27821234567",
                careReady = true,
                maxDistanceKm = 0
            ).validate()
        )
    }
}