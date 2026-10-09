package com.timewall.app.applock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lock decision is the security-critical part of the app lock: every bug that let
 * a protected app open without a PIN was a mistake in this logic. These tests pin the
 * behaviour down so it cannot regress silently.
 */
class LockDecisionTest {

    private val now = 1_000_000L

    @Test
    fun masterSwitchOff_neverLocks() {
        assertFalse(
            LockDecision.needsUnlock(
                enabled = false,
                isProtected = true,
                unlockedAtMs = 0L,
                nowMs = now,
            ),
        )
    }

    @Test
    fun unprotectedApp_neverLocks() {
        assertFalse(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = false,
                unlockedAtMs = 0L,
                nowMs = now,
            ),
        )
    }

    @Test
    fun protectedApp_neverUnlocked_locks() {
        assertTrue(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = 0L,
                nowMs = now,
            ),
        )
    }

    @Test
    fun justUnlocked_doesNotLock() {
        assertFalse(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = now - 1_000L,
                nowMs = now,
            ),
        )
    }

    @Test
    fun insideGraceWindow_doesNotLock() {
        assertFalse(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = now - (LockDecision.GRACE_MS - 1),
                nowMs = now,
            ),
        )
    }

    @Test
    fun exactlyAtGraceBoundary_doesNotLock() {
        // Boundary: still inside the window, so the user is not asked again yet.
        assertFalse(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = now - LockDecision.GRACE_MS,
                nowMs = now,
            ),
        )
    }

    @Test
    fun justPastGraceWindow_locks() {
        assertTrue(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = now - (LockDecision.GRACE_MS + 1),
                nowMs = now,
            ),
        )
    }

    @Test
    fun clearedUnlock_locksAgain() {
        // This is what happens when the screen turns off: the timestamp is cleared, so
        // the app must ask for the PIN again even though it was unlocked moments ago.
        assertTrue(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = 0L,
                nowMs = now,
            ),
        )
    }

    @Test
    fun clockMovedBackwards_locks() {
        // If the wall clock is adjusted backwards, "now - unlockedAt" goes negative. The
        // safe reading is that the grace window is over, not that it lasts forever.
        assertTrue(
            LockDecision.needsUnlock(
                enabled = true,
                isProtected = true,
                unlockedAtMs = now + 5_000L,
                nowMs = now,
            ),
        )
    }
}
