package com.timewall.app.security

/**
 * Pure rules for the app-lock PIN and its brute-force lockout.
 * No Android imports, so every rule is covered by JVM unit tests.
 */
object PinPolicy {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6

    /** Consecutive wrong PINs before a lockout starts. */
    const val LOCKOUT_THRESHOLD = 5
    private const val BASE_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 15 * 60_000L

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    /** Lockout duration after the given number of consecutive failures (0 = no lockout). */
    fun lockoutMillis(failedAttempts: Int): Long {
        if (failedAttempts < LOCKOUT_THRESHOLD) return 0L
        val exponent = (failedAttempts - LOCKOUT_THRESHOLD).coerceAtMost(20)
        return (BASE_LOCKOUT_MS shl exponent).coerceAtMost(MAX_LOCKOUT_MS)
    }

    /** Returns (new failure count, lockedUntil wall-clock ms, or 0 when not locked). */
    fun afterWrongAttempt(failedAttempts: Int, nowMs: Long): Pair<Int, Long> {
        val next = failedAttempts + 1
        val lock = lockoutMillis(next)
        return next to (if (lock > 0L) nowMs + lock else 0L)
    }

    /**
     * Remaining lockout, capped at MAX_LOCKOUT_MS so that moving the system clock
     * cannot extend a lockout indefinitely.
     */
    fun remainingLockoutMs(lockedUntilMs: Long, nowMs: Long): Long {
        if (lockedUntilMs <= 0L) return 0L
        return (lockedUntilMs - nowMs).coerceIn(0L, MAX_LOCKOUT_MS)
    }
}
