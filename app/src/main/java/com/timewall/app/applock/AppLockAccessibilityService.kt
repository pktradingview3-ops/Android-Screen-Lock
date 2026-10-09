package com.timewall.app.applock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground and, if it is protected, launches the
 * unlock overlay on top of it.
 *
 * This is how every mainstream app locker works. Android does not let one app
 * intercept another app's launch directly, so the foreground change is observed
 * here and a full-screen unlock screen is shown immediately.
 *
 * Stability notes (this is the part that decides whether the feature actually works):
 *  - Only [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED] is handled. Handling every
 *    event type causes lag and battery drain for no benefit.
 *  - Events are ignored while our own overlay or our own app is in the foreground,
 *    which prevents a loop where the overlay re-triggers itself.
 *  - The unlock screen is started with FLAG_ACTIVITY_NEW_TASK, because a Service
 *    has no task of its own.
 *  - If the overlay is already showing for the same package, it is not started
 *    again.
 */
class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"

        /**
         * After the overlay is dismissed (unlock or cancel), do not re-lock the same
         * package for this long. Without it, dismissing the overlay puts the protected
         * app back in the foreground and the service immediately re-locks it, which
         * looks like the screen is stuck and cannot be dismissed.
         */
        private const val RELOCK_COOLDOWN_MS = 1_200L

        /** Set by the overlay while it is visible, so we do not relaunch ourselves. */
        @Volatile
        var overlayShowing: Boolean = false

        /** The package currently behind the overlay, if any. */
        @Volatile
        var overlayPackage: String? = null

        /** Last time we stopped showing an overlay, per package. */
        private val lastDismissedAt = mutableMapOf<String, Long>()

        fun noteDismissed(packageName: String, nowMs: Long = System.currentTimeMillis()) {
            synchronized(lastDismissedAt) { lastDismissedAt[packageName] = nowMs }
        }

        private fun inCooldown(packageName: String, nowMs: Long): Boolean {
            val at = synchronized(lastDismissedAt) { lastDismissedAt[packageName] } ?: return false
            return nowMs - at < RELOCK_COOLDOWN_MS
        }

        /** True when the user has the service enabled in system settings. */
        @Volatile
        var connected: Boolean = false
            private set
    }

    private lateinit var targets: AppLockTargetStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        targets = AppLockTargetStore.from(this)
        connected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        if (ev.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = ev.packageName?.toString() ?: return
        if (!::targets.isInitialized) targets = AppLockTargetStore.from(this)

        // Never react to our own app or our own overlay.
        if (packageName == this.packageName) return
        if (overlayShowing) return

        // System UI (dialogs, recents, the lock screen) must stay usable.
        if (packageName == SYSTEM_UI) return

        // Safety net for a package that got protected before the picker refused it:
        // locking the launcher or Settings can lock the user out of their own phone.
        if (packageName in UNSAFE_TO_LOCK) return

        if (!targets.needsUnlock(packageName)) return

        // Do not immediately re-lock an app whose overlay the user just dismissed.
        if (inCooldown(packageName, System.currentTimeMillis())) return

        showUnlockOverlay(packageName)
    }

    private fun showUnlockOverlay(packageName: String) {
        overlayPackage = packageName
        val intent = Intent(this, AppUnlockActivity::class.java).apply {
            putExtra(AppUnlockActivity.EXTRA_TARGET_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {
        // Nothing to clean up: no long-running work is started here.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        connected = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }
}
