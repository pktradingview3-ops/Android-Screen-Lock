package com.timewall.app.applock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground and, if it is protected, shows the
 * unlock screen on top of it.
 *
 * This is how every mainstream app locker works: Android does not let one app
 * intercept another app's launch, so the foreground change is observed here.
 *
 * ## The dismissal problem (and why the old code let apps open without a PIN)
 *
 * Closing the unlock screen puts the protected app back in the foreground, which the
 * service sees as "a protected app just opened" - so it locks it again. The previous
 * fix was a 1.2 s cooldown that **dropped** those events. That was wrong, and it was
 * a real security hole: an event dropped while the app was in the foreground is never
 * re-delivered, so after pressing Cancel the app stayed open with no PIN, forever.
 *
 * The correct model is here:
 *  - Dismissal puts the protected app into a short **suppression window**. Events for
 *    it during that window are ignored, which absorbs the transition.
 *  - The window ends as soon as any other app comes to the front (the launcher does,
 *    because Cancel sends the user home).
 *  - A **re-check is always scheduled** for the end of the window. If the protected app
 *    is still in front and still locked, the overlay is shown then. So a dropped event
 *    can no longer mean permanent access - it only means a short delay.
 *
 * ## Other stability rules
 *  - Only TYPE_WINDOW_STATE_CHANGED is handled; handling everything wastes battery.
 *  - Our own app and System UI are ignored, which stops the overlay re-triggering itself.
 *  - [overlayShowing] is timestamped, so a stale flag (after an odd process death) can
 *    never permanently disable locking.
 */
class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TimeWallLock"
        private const val SYSTEM_UI = "com.android.systemui"

        /**
         * How long a just-dismissed app is left alone, so the transition back to it is
         * not mistaken for a fresh launch. Kept short: the user is sent to the home
         * screen at the same time, so they are not looking at the protected app.
         */
        private const val SUPPRESS_MS = 1_000L

        /**
         * If [overlayShowing] has been true for longer than this, the overlay is gone
         * (it died without cleaning up) and the flag must not keep blocking locks.
         */
        private const val OVERLAY_STALE_MS = 60_000L

        /** Set by the overlay while it is visible, so we do not relaunch ourselves. */
        @Volatile
        var overlayShowing: Boolean = false
            private set

        @Volatile
        private var overlayShownAt: Long = 0L

        /** The package currently behind the overlay, if any. */
        @Volatile
        var overlayPackage: String? = null

        /** Called by the overlay when it becomes visible. */
        fun markOverlayShown(packageName: String) {
            overlayPackage = packageName
            overlayShownAt = System.currentTimeMillis()
            overlayShowing = true
        }

        /** Called by the overlay when it is finished, for any reason. */
        fun markOverlayHidden() {
            overlayShowing = false
            overlayPackage = null
        }

        /**
         * Called when the user dismissed the overlay with Cancel/Back. Starts the
         * suppression window. Must NOT be called on a successful unlock: a real unlock
         * is recorded separately and must not be suppressed.
         */
        @Volatile
        private var suppressPackage: String? = null
        private var suppressDeadline: Long = 0L

        fun noteDismissed(packageName: String) {
            suppressPackage = packageName
            suppressDeadline = System.currentTimeMillis() + SUPPRESS_MS
            instance?.scheduleRecheck(SUPPRESS_MS + 100L)
            Log.d(TAG, "dismissed $packageName, suppressing for ${SUPPRESS_MS}ms")
        }

        private var instance: AppLockAccessibilityService? = null

        /** True when the user has the service enabled in system settings. */
        @Volatile
        var connected: Boolean = false
            private set
    }

    private lateinit var targets: AppLockTargetStore
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Clears every unlock grace window when the screen goes off, so an app unlocked
     * just before locking the phone is locked again when the phone comes back.
     */
    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action != android.content.Intent.ACTION_SCREEN_OFF) return
            if (!::targets.isInitialized) return
            targets.clearAllUnlocked()
            Log.d(TAG, "screen off: cleared unlock grace")
        }
    }

    /** The package most recently seen in the foreground (excluding our own app). */
    @Volatile
    private var foregroundPackage: String? = null

    /**
     * If the suppressed app is still in front when the window ends, lock it then.
     * This is what makes a swallowed event harmless instead of permanent access.
     */
    private val recheck = Runnable {
        val pkg = suppressPackage ?: return@Runnable
        suppressPackage = null
        if (!isOverlayLive() && foregroundPackage == pkg && targets.needsUnlock(pkg)) {
            Log.d(TAG, "re-check: $pkg still in front, locking")
            showUnlockOverlay(pkg)
        }
    }

    private fun scheduleRecheck(delayMs: Long) {
        handler.removeCallbacks(recheck)
        handler.postDelayed(recheck, delayMs)
    }

    private fun isOverlayLive(): Boolean =
        overlayShowing && System.currentTimeMillis() - overlayShownAt < OVERLAY_STALE_MS

    override fun onServiceConnected() {
        super.onServiceConnected()
        targets = AppLockTargetStore.from(this)
        connected = true
        instance = this
        registerReceiver(
            screenOffReceiver,
            android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF),
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        if (ev.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = ev.packageName?.toString() ?: return
        if (!::targets.isInitialized) targets = AppLockTargetStore.from(this)

        // Never react to our own app or our own overlay.
        if (packageName == this.packageName) return

        // System UI (dialogs, recents, the lock screen) must stay usable.
        if (packageName == SYSTEM_UI) return

        // Remember what is really in front, including the launcher and Settings.
        foregroundPackage = packageName

        // Any other app coming forward means the dismissal transition is over.
        if (suppressPackage != null && packageName != suppressPackage) {
            suppressPackage = null
            handler.removeCallbacks(recheck)
        }

        if (isOverlayLive()) return

        // Safety net for a package protected before the picker refused it: locking the
        // launcher or Settings can lock the user out of their own phone.
        if (packageName in UNSAFE_TO_LOCK) return

        if (!targets.needsUnlock(packageName)) return

        // Absorb the transition back into an app whose overlay the user just dismissed.
        val suppressed = suppressPackage
        if (suppressed != null && packageName == suppressed) {
            if (System.currentTimeMillis() < suppressDeadline) {
                Log.d(TAG, "suppressed event for $packageName")
                return
            }
            suppressPackage = null
        }

        showUnlockOverlay(packageName)
    }

    private fun showUnlockOverlay(packageName: String) {
        Log.d(TAG, "locking $packageName")
        markOverlayShown(packageName)
        val intent = Intent(this, AppUnlockActivity::class.java).apply {
            putExtra(AppUnlockActivity.EXTRA_TARGET_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        runCatching { startActivity(intent) }
            .onFailure { markOverlayHidden() }
    }

    override fun onInterrupt() {
        // Nothing to clean up: no long-running work is started here.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        connected = false
        instance = null
        runCatching { unregisterReceiver(screenOffReceiver) }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
        instance = null
        runCatching { unregisterReceiver(screenOffReceiver) }
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
