package com.timewall.app.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide lock state. The app is locked:
 *  - when the process starts and app lock is on,
 *  - when it returns to the foreground after being in the background longer than GRACE_MS.
 * A short background trip (for example, to the system wallpaper picker and back) does not lock it.
 */
object AppSession {

    const val GRACE_MS = 30_000L

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAtMs: Long? = null

    /** Monotonic clock. Overridable in unit tests. */
    var clock: () -> Long = { SystemClock.elapsedRealtime() }

    fun onProcessStart(lockEnabled: Boolean) {
        backgroundedAtMs = null
        _locked.value = lockEnabled
    }

    fun onAppStopped() {
        backgroundedAtMs = clock()
    }

    fun onAppStarted(lockEnabled: Boolean) {
        val since = backgroundedAtMs
        backgroundedAtMs = null
        if (!lockEnabled) {
            _locked.value = false
            return
        }
        if (since == null || clock() - since > GRACE_MS) {
            _locked.value = true
        }
    }

    fun lockNow(lockEnabled: Boolean) {
        if (lockEnabled) _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }
}
