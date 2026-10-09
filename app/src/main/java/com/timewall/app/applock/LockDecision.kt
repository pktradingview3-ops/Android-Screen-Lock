package com.timewall.app.applock

/**
 * The decision of whether a package must be unlocked right now.
 *
 * Kept as a pure function, separate from [AppLockTargetStore], because this is the
 * security-critical part of the feature and it needs to be testable without an
 * Android device or a Context. Every bug that let an app open without a PIN lived
 * here, so it is covered by unit tests.
 */
object LockDecision {

    /** How long an app stays unlocked after a successful unlock. */
    const val GRACE_MS = 30_000L

    /**
     * @param enabled      the master switch
     * @param isProtected  whether the user picked this package
     * @param unlockedAtMs when the app was last unlocked, 0 if never
     * @param nowMs        current wall clock
     * @return true when the unlock screen must be shown
     */
    fun needsUnlock(
        enabled: Boolean,
        isProtected: Boolean,
        unlockedAtMs: Long,
        nowMs: Long,
    ): Boolean {
        if (!enabled) return false
        if (!isProtected) return false
        // Never unlocked (or cleared): lock it.
        if (unlockedAtMs <= 0L) return true
        // The wall clock moved backwards (NTP sync, the user changing the time). Reading
        // that as a very long grace window would leave the app open indefinitely, so lock.
        if (unlockedAtMs > nowMs) return true
        // Outside the grace window: lock it. Inside: leave it open.
        return nowMs - unlockedAtMs > GRACE_MS
    }
}
