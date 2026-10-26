package za.co.yebopets

import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.part3.BiometricSession

class BiometricSessionTest {
    @Test fun coldStartRequestsBiometricsWithoutOpeningProtectedContent() {
        val state = BiometricSession("owner", true)
        assertTrue(state.locked)
        assertFalse(state.opened)
        state.resume()
        assertTrue(state.request > 0)
        val attempt = state.beginAttempt()!!
        state.complete(attempt, true)
        assertFalse(state.locked)
        assertTrue(state.opened)
    }

    @Test fun returningFromBackgroundRequiresANewSuccessfulPrompt() {
        val state = BiometricSession("owner", false)
        state.stop(true)
        assertTrue(state.locked)
        state.resume()
        val attempt = state.beginAttempt()!!
        state.complete(attempt, true)
        assertFalse(state.locked)
    }

    @Test fun lateSuccessFromBeforeBackgroundCannotUnlockNewSession() {
        val state = BiometricSession("owner", true)
        state.resume()
        val stale = state.beginAttempt()!!
        state.stop(true)
        state.resume()
        val current = state.beginAttempt()!!
        state.complete(stale, true)
        assertTrue(state.locked)
        state.complete(current, true)
        assertFalse(state.locked)
    }

    @Test fun cancelledPromptStaysLockedWithoutAnAutomaticRetryLoop() {
        val state = BiometricSession("owner", true)
        state.resume()
        state.complete(state.beginAttempt()!!, false)
        assertTrue(state.locked)
        assertTrue(state.error)
        val request = state.request
        state.resume()
        assertEquals(request, state.request)
        state.retry()
        assertTrue(state.request > request)
        state.complete(state.beginAttempt()!!, true)
        assertFalse(state.locked)
    }

    @Test fun OldAccountPromptCannotAffectFreshGoogleSignIn() {
        val state = BiometricSession("first", true)
        state.resume()
        val stale = state.beginAttempt()!!
        state.accountChanged("second")
        state.stop(true)
        state.resume()
        state.beginAttempt()
        state.complete(stale, true)
        assertEquals("second", state.uid)
        assertTrue(state.locked)
    }
}
