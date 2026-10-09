package com.timewall.app.applock

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.timewall.app.security.AppLockManager
import com.timewall.app.security.AppLockStore
import com.timewall.app.security.BiometricGate
import com.timewall.app.security.PinPolicy
import com.timewall.app.security.VerifyOutcome
import com.timewall.app.ui.PinPad
import com.timewall.app.ui.TimeWallTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The full-screen unlock screen shown on top of a protected app.
 *
 * Stability rules that make this reliable:
 *  - [WindowManager.LayoutParams.FLAG_SECURE] so the protected app's content cannot
 *    be screenshotted behind it.
 *  - Back is swallowed: the user cannot dismiss it and peek at the app.
 *  - `noHistory` + `excludeFromRecents` so it leaves no trace in Recents.
 *  - On unlock the target app is brought back to the front, so the user lands where
 *    they intended instead of on a blank launcher.
 *  - Wrong attempts use the same [AppLockManager] lockout as the app's own lock, so
 *    this screen cannot be brute-forced either.
 */
class AppUnlockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    /** The protected package this overlay belongs to. */
    private var currentTarget: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        currentTarget = targetPackage
        val label = appLabel(targetPackage)

        AppLockAccessibilityService.overlayShowing = true
        AppLockAccessibilityService.overlayPackage = targetPackage

        setContent {
            TimeWallTheme {
                UnlockScreen(
                    appLabel = label,
                    onUnlocked = {
                        AppLockTargetStore.from(this).markUnlocked(targetPackage)
                        clearOverlayFlags()
                        packageManager.getLaunchIntentForPackage(targetPackage)?.let { back ->
                            back.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { startActivity(back) }
                        }
                        finish()
                    },
                    onCancelled = {
                        clearOverlayFlags()
                        val home = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                            addCategory(android.content.Intent.CATEGORY_HOME)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { startActivity(home) }
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // singleInstance + noHistory: a second lock request arrives here, not in onCreate.
        // Re-arming the target keeps the overlay pointed at the right app.
        setIntent(intent)
        intent.getStringExtra(EXTRA_TARGET_PACKAGE)?.let { currentTarget = it }
        AppLockAccessibilityService.overlayShowing = true
        AppLockAccessibilityService.overlayPackage = currentTarget
    }

    private fun clearOverlayFlags() {
        AppLockAccessibilityService.overlayShowing = false
        AppLockAccessibilityService.overlayPackage = null
        // Tell the service not to immediately re-lock this package; otherwise dismissing
        // the overlay just brings the protected app forward and it locks again at once.
        currentTarget?.let { AppLockAccessibilityService.noteDismissed(it) }
    }

    override fun onDestroy() {
        clearOverlayFlags()
        super.onDestroy()
    }

    /** Best-effort human-readable name for the protected app. */
    private fun appLabel(packageName: String): String = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)
}

@Composable
private fun UnlockScreen(
    appLabel: String,
    onUnlocked: () -> Unit,
    onCancelled: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val store = remember(context) { AppLockStore.from(context) }
    val manager = remember(store) { AppLockManager(store) }
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Enter your PIN to open $appLabel") }
    var lockoutUntil by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // Tick the countdown while a lockout is active.
    LaunchedEffect(lockoutUntil) {
        while (lockoutUntil > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            delay(500)
        }
        if (lockoutUntil > 0L) {
            lockoutUntil = 0L
            message = "Enter your PIN to open $appLabel"
        }
    }

    val lockedOut = lockoutUntil > now

    // Offer fingerprint once, if the user turned it on.
    LaunchedEffect(Unit) {
        if (store.biometricEnabled && activity != null && BiometricGate.canUse(context)) {
            BiometricGate.prompt(activity, onSuccess = onUnlocked, onError = { /* PIN still works */ })
        }
    }

    // Swallow back so the protected app cannot be reached without unlocking.
    BackHandler(enabled = true) { onCancelled() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = appLabel, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            PinPad(
                pin = pin,
                enabled = !lockedOut,
                tagPrefix = "appunlock_",
                onDigit = { digit ->
                    if (pin.length < PinPolicy.MAX_LENGTH) pin += digit
                },
                onDelete = { pin = pin.dropLast(1) },
                onSubmit = {
                    val candidate = pin
                    pin = ""
                    scope.launch {
                        when (val outcome = manager.verify(candidate)) {
                            is VerifyOutcome.Success -> onUnlocked()
                            is VerifyOutcome.Wrong ->
                                message = "Wrong PIN. ${outcome.attemptsLeft} attempt(s) left."
                            is VerifyOutcome.LockedOut -> {
                                lockoutUntil = outcome.untilWallMs
                                message = "Too many attempts. Try again shortly."
                            }
                        }
                    }
                },
            )

            Spacer(Modifier.height(20.dp))
            Button(onClick = onCancelled) { Text("Cancel") }
        }
    }
}
