package com.timewall.app.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface VerifyOutcome {
    data object Success : VerifyOutcome
    data class Wrong(val attemptsLeft: Int) : VerifyOutcome
    data class LockedOut(val untilWallMs: Long) : VerifyOutcome
}

/**
 * Checks PIN attempts and enforces lockout. A mutex makes sure two quick taps cannot
 * both count as separate attempts in a racy way.
 */
class AppLockManager(
    private val store: AppLockStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    fun isEnabled(): Boolean = store.isEnabled

    fun lockoutRemainingMs(): Long = PinPolicy.remainingLockoutMs(store.lockedUntilMs, clock())

    suspend fun verify(pin: String): VerifyOutcome = mutex.withLock {
        val remaining = lockoutRemainingMs()
        if (remaining > 0L) {
            return@withLock VerifyOutcome.LockedOut(clock() + remaining)
        }
        val matched = withContext(Dispatchers.Default) { store.verifyPin(pin) }
        if (matched) {
            store.resetFailures()
            VerifyOutcome.Success
        } else {
            val (failed, lockedUntil) = PinPolicy.afterWrongAttempt(store.failedAttempts, clock())
            store.recordFailure(failed, lockedUntil)
            if (lockedUntil > 0L) {
                VerifyOutcome.LockedOut(lockedUntil)
            } else {
                VerifyOutcome.Wrong(PinPolicy.LOCKOUT_THRESHOLD - failed)
            }
        }
    }
}
