package com.timewall.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {

    @Test
    fun validPin_isFourToSixDigitsOnly() {
        assertFalse(PinPolicy.isValidPin("123"))
        assertTrue(PinPolicy.isValidPin("1234"))
        assertTrue(PinPolicy.isValidPin("123456"))
        assertFalse(PinPolicy.isValidPin("1234567"))
        assertFalse(PinPolicy.isValidPin(""))
        assertFalse(PinPolicy.isValidPin("12a4"))
        assertFalse(PinPolicy.isValidPin(" 1234"))
        assertFalse(PinPolicy.isValidPin("١٢٣٤")) // Arabic-Indic digits are not accepted
    }

    @Test
    fun noLockout_belowThreshold() {
        for (failures in 0 until PinPolicy.LOCKOUT_THRESHOLD) {
            assertEquals(0L, PinPolicy.lockoutMillis(failures))
        }
    }

    @Test
    fun lockout_doublesAfterEachFurtherFailure() {
        assertEquals(30_000L, PinPolicy.lockoutMillis(5))
        assertEquals(60_000L, PinPolicy.lockoutMillis(6))
        assertEquals(120_000L, PinPolicy.lockoutMillis(7))
        assertEquals(240_000L, PinPolicy.lockoutMillis(8))
    }

    @Test
    fun lockout_isCappedAtFifteenMinutes() {
        assertEquals(PinPolicy.MAX_LOCKOUT_MS, PinPolicy.lockoutMillis(20))
        assertEquals(PinPolicy.MAX_LOCKOUT_MS, PinPolicy.lockoutMillis(500))
        assertEquals(PinPolicy.MAX_LOCKOUT_MS, PinPolicy.lockoutMillis(Int.MAX_VALUE))
    }

    @Test
    fun afterWrongAttempt_countsUpAndLocksOnThreshold() {
        val now = 1_000_000L
        var failures = 0
        repeat(PinPolicy.LOCKOUT_THRESHOLD - 1) {
            val (next, lockedUntil) = PinPolicy.afterWrongAttempt(failures, now)
            assertEquals(0L, lockedUntil)
            failures = next
        }
        val (next, lockedUntil) = PinPolicy.afterWrongAttempt(failures, now)
        assertEquals(PinPolicy.LOCKOUT_THRESHOLD, next)
        assertEquals(now + 30_000L, lockedUntil)
    }

    @Test
    fun remainingLockout_isZeroWhenNoLockOrExpired() {
        assertEquals(0L, PinPolicy.remainingLockoutMs(0L, 5_000L))
        assertEquals(0L, PinPolicy.remainingLockoutMs(4_000L, 5_000L))
    }

    @Test
    fun remainingLockout_isCappedSoClockChangesCannotExtendIt() {
        // A lock time far in the future (for example after the clock was moved) is capped.
        assertEquals(
            PinPolicy.MAX_LOCKOUT_MS,
            PinPolicy.remainingLockoutMs(Long.MAX_VALUE / 2, 0L),
        )
        assertEquals(10_000L, PinPolicy.remainingLockoutMs(15_000L, 5_000L))
    }
}
