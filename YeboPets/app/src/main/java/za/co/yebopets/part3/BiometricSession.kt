package za.co.yebopets.part3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A prompt result can unlock only the same foreground session that requested it. */
internal class BiometricSession(initialUid: String?, enabled: Boolean) {
    var uid by mutableStateOf(initialUid)
        private set
    var locked by mutableStateOf(initialUid != null && enabled)
        private set
    var opened by mutableStateOf(!locked)
        private set
    var error by mutableStateOf(false)
        private set
    var request by mutableIntStateOf(0)
        private set
    private var foreground = false
    private var attempted = false
    private var sequence = 0L
    private var activeAttempt: Long? = null

    fun accountChanged(newUid: String?) {
        if (uid == newUid) return
        uid = newUid
        activeAttempt = null
        locked = false // A new account has just completed the Google sign-in flow.
        opened = true
        error = false
        attempted = false
    }

    fun resume() {
        foreground = true
        if (locked && !attempted) request++
    }

    fun stop(enabled: Boolean) {
        foreground = false
        activeAttempt = null
        attempted = false
        error = false
        locked = uid != null && enabled
        if (!locked) opened = true
    }

    fun beginAttempt(): Long? {
        if (!foreground || !locked || activeAttempt != null) return null
        attempted = true
        error = false
        return (++sequence).also { activeAttempt = it }
    }

    fun complete(attempt: Long, success: Boolean) {
        if (!foreground || activeAttempt != attempt) return
        activeAttempt = null
        error = !success
        if (success) {
            locked = false
            opened = true
        }
    }

    fun retry() {
        if (foreground && locked && activeAttempt == null) request++
    }
}
