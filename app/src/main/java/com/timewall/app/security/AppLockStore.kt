package com.timewall.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * Stores the PIN hash, salt and lockout state. Only hashes are stored, never the PIN.
 * Writes use commit(), so a lockout or new PIN is saved before the process can die.
 */
class AppLockStore(private val prefs: SharedPreferences) {

    companion object {
        private const val PREFS_NAME = "timewall_applock"
        private const val KEY_HASH = "hash"
        private const val KEY_SALT = "salt"
        private const val KEY_ITERATIONS = "iterations"
        private const val KEY_FAILED = "failed_attempts"
        private const val KEY_LOCKED_UNTIL = "locked_until_ms"
        private const val KEY_BIOMETRIC = "biometric"

        fun from(context: Context): AppLockStore = AppLockStore(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        )
    }

    val isEnabled: Boolean
        get() = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    var biometricEnabled: Boolean
        get() = isEnabled && prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BIOMETRIC, value).commit()
        }

    val failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)

    val lockedUntilMs: Long
        get() = prefs.getLong(KEY_LOCKED_UNTIL, 0L)

    /** Returns false if the write could not be committed to storage. */
    fun savePin(pin: String): Boolean {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt, PinHasher.ITERATIONS)
        return prefs.edit()
            .putString(KEY_HASH, encode(hash))
            .putString(KEY_SALT, encode(salt))
            .putInt(KEY_ITERATIONS, PinHasher.ITERATIONS)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .commit()
    }

    fun verifyPin(pin: String): Boolean {
        val hash = prefs.getString(KEY_HASH, null)?.let(::decode) ?: return false
        val salt = prefs.getString(KEY_SALT, null)?.let(::decode) ?: return false
        val iterations = prefs.getInt(KEY_ITERATIONS, PinHasher.ITERATIONS)
        return PinHasher.matches(pin, salt, iterations, hash)
    }

    fun recordFailure(failedAttempts: Int, lockedUntilMs: Long) {
        prefs.edit()
            .putInt(KEY_FAILED, failedAttempts)
            .putLong(KEY_LOCKED_UNTIL, lockedUntilMs)
            .commit()
    }

    fun resetFailures() {
        prefs.edit()
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .commit()
    }

    /** Turns app lock off and removes the PIN hash. */
    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)
}
