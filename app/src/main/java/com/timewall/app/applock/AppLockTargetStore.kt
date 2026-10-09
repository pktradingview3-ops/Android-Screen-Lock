package com.timewall.app.applock

import android.content.Context
import android.content.SharedPreferences

/**
 * Which apps the user has chosen to protect, plus the per-app unlock state.
 *
 * A protected app stays unlocked for [GRACE_MS] after the user unlocks it, so the
 * user is not asked again when they switch away for a moment and come back.
 */
class AppLockTargetStore private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "timewall_applock_targets"
        private const val KEY_PROTECTED = "protected"
        private const val KEY_UNLOCKED_AT = "unlocked_at"
        private const val KEY_ENABLED = "enabled"

        /** How long an app stays unlocked after a successful unlock. */
        const val GRACE_MS = LockDecision.GRACE_MS

        fun from(context: Context): AppLockTargetStore = AppLockTargetStore(context)
    }

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Master switch: is per-app locking active at all? */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).commit()
        }

    /** Package names the user wants locked. */
    val protectedPackages: Set<String>
        get() = prefs.getStringSet(KEY_PROTECTED, emptySet())?.toSet() ?: emptySet()

    fun isProtected(packageName: String): Boolean = packageName in protectedPackages

    fun protect(packageName: String) {
        val next = protectedPackages + packageName
        prefs.edit().putStringSet(KEY_PROTECTED, next).commit()
    }

    fun unprotect(packageName: String) {
        val next = protectedPackages - packageName
        prefs.edit().putStringSet(KEY_PROTECTED, next).commit()
        clearUnlocked(packageName)
    }

    /** Called when the user successfully unlocks a specific app. */
    fun markUnlocked(packageName: String, nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong("$KEY_UNLOCKED_AT:$packageName", nowMs).commit()
    }

    fun clearUnlocked(packageName: String) {
        prefs.edit().remove("$KEY_UNLOCKED_AT:$packageName").commit()
    }

    /**
     * Forget every unlock grace window.
     *
     * Called when the screen turns off. Without this, unlocking an app and then locking
     * the phone would leave that app unlocked for the rest of the grace window, so
     * picking the phone up again could open it with no PIN.
     */
    fun clearAllUnlocked() {
        val edit = prefs.edit()
        prefs.all.keys.filter { it.startsWith("$KEY_UNLOCKED_AT:") }.forEach(edit::remove)
        edit.commit()
    }

    /** True when the app is protected AND not inside its unlock grace window. */
    fun needsUnlock(packageName: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val unlockedAt = prefs.getLong("$KEY_UNLOCKED_AT:$packageName", 0L)
        return LockDecision.needsUnlock(
            enabled = enabled,
            isProtected = isProtected(packageName),
            unlockedAtMs = unlockedAt,
            nowMs = nowMs,
        )
    }
}
