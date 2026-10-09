package com.timewall.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timewall.app.data.ConfigStore
import com.timewall.app.security.AppLockStore
import com.timewall.app.security.AppSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * PIN lock flows through the real UI. Each test resets app-lock storage first.
 * Fingerprint is not covered here: emulators normally have no enrolled fingerprint.
 */
@RunWith(AndroidJUnit4::class)
class AppLockFlowTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val composeRule = createAndroidComposeRule<MainActivity>()

    // Reset must run BEFORE the activity launches: the activity's ViewModel reads the store on creation.
    private val resetState = object : ExternalResource() {
        override fun before() {
            ConfigStore.prefs(context).edit().clear().commit()
            AppLockStore.from(context).clearAll()
            AppSession.onProcessStart(false)
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(resetState).around(composeRule)

    // ---- Helpers

    private fun exists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /** Waits for a test tag. On timeout, the error names the tag so the failing step is clear. */
    private fun waitForTag(tag: String) {
        try {
            composeRule.waitUntil(10_000) { exists(tag) }
        } catch (e: Throwable) {
            throw AssertionError("Timed out waiting for test tag '$tag'", e)
        }
    }

    /** Waits until a node with this tag shows text containing [text]. */
    private fun waitForTagText(tag: String, text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(tag).and(hasText(text, substring = true)))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Taps a control. Scrolls first, because the PIN pad and settings are in scrollable columns. */
    private fun tap(tag: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun typePin(prefix: String, digits: String) {
        digits.forEach { tap("${prefix}key_$it") }
    }

    /** Makes the app locked as if the process just started with lock enabled. */
    private fun lockAppWithPin(pin: String) {
        AppLockStore.from(context).savePin(pin)
        AppSession.onProcessStart(true)
        waitForTag("lock_screen")
    }

    /** Turns app lock on through the settings screen, then returns to the layout list. */
    private fun enableLockWithPinViaUi(pin: String) {
        composeRule.onNodeWithTag("btn_applock").performClick()
        waitForTag("btn_applock_on")
        tap("btn_applock_on")
        typePin("setpin_", pin)
        tap("setpin_submit")
        typePin("setpin_", pin)
        tap("setpin_submit")
        waitForTag("btn_applock_change")
    }

    // ---- Tests

    @Test
    fun lockedApp_showsLockScreen_andCorrectPinOpensApp() {
        lockAppWithPin("2468")
        composeRule.onNodeWithTag("lock_screen").assertIsDisplayed()

        typePin("lock_", "2468")
        tap("lock_submit")
        waitForTag("picker_screen")
        assertFalse(AppSession.locked.value)
    }

    @Test
    fun wrongPin_showsErrorAndStaysLocked() {
        lockAppWithPin("2468")

        typePin("lock_", "1111")
        tap("lock_submit")
        waitForTagText("lock_error", "Wrong PIN")
        composeRule.onNodeWithTag("lock_screen").assertIsDisplayed()
        assertTrue(AppSession.locked.value)
    }

    @Test
    fun fiveWrongPins_startLockoutCountdown() {
        lockAppWithPin("2468")

        for (attempt in 1..4) {
            typePin("lock_", "1111")
            tap("lock_submit")
            waitForTagText("lock_error", "${5 - attempt} attempt(s) left")
        }
        typePin("lock_", "1111")
        tap("lock_submit")
        waitForTag("lock_countdown")
        composeRule.onNodeWithTag("lock_countdown").assertIsDisplayed()

        // While locked out, even the correct PIN is refused.
        typePin("lock_", "2468")
        tap("lock_submit")
        assertTrue(AppSession.locked.value)
    }

    @Test
    fun turnOnLock_thenLockNow_requiresPinToReturn() {
        enableLockWithPinViaUi("1357")

        tap("btn_applock_back")
        waitForTag("picker_screen")

        composeRule.onNodeWithTag("btn_applock").performClick()
        waitForTag("btn_applock_now")
        tap("btn_applock_now")

        waitForTag("lock_screen")
        typePin("lock_", "1357")
        tap("lock_submit")
        waitForTag("picker_screen")
    }

    @Test
    fun turningOffLock_withWrongPin_keepsLockOn() {
        enableLockWithPinViaUi("1357")
        tap("btn_applock_off")

        typePin("setpin_", "0000")
        tap("setpin_submit")
        waitForTag("applock_message")
        composeRule.onNodeWithTag("applock_message").assertIsDisplayed()
        assertTrue(AppLockStore.from(context).isEnabled)
    }

    @Test
    fun turningOffLock_withCorrectPin_disablesLock() {
        enableLockWithPinViaUi("1357")
        tap("btn_applock_off")

        typePin("setpin_", "1357")
        tap("setpin_submit")
        waitForTag("btn_applock_on")
        assertFalse(AppLockStore.from(context).isEnabled)
    }
}
