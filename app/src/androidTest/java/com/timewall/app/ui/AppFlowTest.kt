package com.timewall.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timewall.app.data.ConfigStore
import com.timewall.app.security.AppLockStore
import com.timewall.app.security.AppSession
import com.timewall.app.domain.LayoutId
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * End-to-end UI flow tests. Each test starts from a clean state (saved settings are cleared),
 * then drives the real app through its buttons, chips, switch and text field.
 */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val clearSettings = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            ConfigStore.prefs(context).edit().clear().commit()
            // App lock must be off so the layout tests are not blocked by the PIN screen.
            AppLockStore.from(context).clearAll()
            AppSession.onProcessStart(false)
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(clearSettings).around(composeRule)

    // ---- Layout list (home screen)

    @Test
    fun home_showsAllFiveLayouts() {
        composeRule.onNodeWithTag("picker_screen").assertIsDisplayed()
        // LazyColumn only composes visible rows, so scroll to each one before checking it.
        LayoutId.values().forEach { layout ->
            scrollToLayout(layout)
            composeRule.onNodeWithTag("layout_${layout.name}").assertIsDisplayed()
        }
    }

    @Test
    fun home_defaultSelectionIsL1() {
        composeRule.onNodeWithTag("layout_L1").assertIsSelected()
    }

    @Test
    fun home_tappingLayoutSelectsIt() {
        selectLayout(LayoutId.L2)
        composeRule.onNodeWithTag("layout_L2").assertIsSelected()
        scrollToLayout(LayoutId.L1)
        composeRule.onNodeWithTag("layout_L1").assertIsNotSelected()
    }

    @Test
    fun home_scrollToLastLayoutAndSelectIt() {
        selectLayout(LayoutId.L5)
        composeRule.onNodeWithTag("layout_L5").assertIsSelected()
    }

    @Test
    fun home_helpButtonOpensHelp_andBackReturns() {
        composeRule.onNodeWithTag("btn_help").performClick()
        composeRule.onNodeWithTag("help_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_help_back").performClick()
        composeRule.onNodeWithTag("picker_screen").assertIsDisplayed()
    }

    @Test
    fun home_editButtonOpensEditor() {
        composeRule.onNodeWithTag("btn_edit").performClick()
        composeRule.onNodeWithTag("editor_screen").assertIsDisplayed()
    }

    // ---- Editor

    @Test
    fun editor_backReturnsToLayouts() {
        openEditor()
        composeRule.onNodeWithTag("btn_back").performClick()
        composeRule.onNodeWithTag("picker_screen").assertIsDisplayed()
    }

    @Test
    fun editor_switchTo24Hour() {
        openEditor()
        composeRule.onNodeWithTag("chip_12h").assertIsSelected()
        composeRule.onNodeWithTag("chip_24h").performClick()
        composeRule.onNodeWithTag("chip_24h").assertIsSelected()
        composeRule.onNodeWithTag("chip_12h").assertIsNotSelected()
    }

    @Test
    fun editor_dateSwitchEnabledForL1() {
        openEditor()
        composeRule.onNodeWithTag("switch_date").assertIsDisplayed()
    }

    @Test
    fun editor_dateSwitchDisabledForL2() {
        selectLayout(LayoutId.L2)
        openEditor()
        composeRule.onNodeWithTag("switch_date").assertIsNotEnabled()
    }

    @Test
    fun editor_nameFieldLimitsTo20Characters() {
        openEditor()
        composeRule.onNodeWithTag("name_field").performTextInput("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        composeRule.onNodeWithText("20/20").fetchSemanticsNode()
    }

    @Test
    fun editor_nameFieldShowsCounter() {
        openEditor()
        composeRule.onNodeWithTag("name_field").performTextInput("Ayush")
        composeRule.onNodeWithText("5/20").fetchSemanticsNode()
    }

    @Test
    fun editor_selectionSurvivesGoingBackAndForth() {
        selectLayout(LayoutId.L4)
        openEditor()
        composeRule.onNodeWithTag("chip_24h").performClick()
        composeRule.onNodeWithTag("btn_back").performClick()
        scrollToLayout(LayoutId.L4)
        composeRule.onNodeWithTag("layout_L4").assertIsSelected()
        openEditor()
        composeRule.onNodeWithTag("chip_24h").assertIsSelected()
    }

    @Test
    fun editor_liveButtonExists() {
        openEditor()
        composeRule.onNodeWithTag("btn_live").fetchSemanticsNode()
        composeRule.onNodeWithTag("btn_static").fetchSemanticsNode()
    }

    @Test
    fun editor_staticSnapshotButtonSetsLockImage() {
        openEditor()
        composeRule.onNodeWithTag("btn_static").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("Lock screen image set", substring = true))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasText("Could not set", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText("Could not set", substring = true))
            .assertCountEquals(0)
        composeRule.onNodeWithTag("status").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editor_hasLockScreenSettingsButton() {
        openEditor()
        composeRule.onNodeWithTag("btn_lock_settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun home_showsVersionLabel() {
        composeRule.onNodeWithTag("version_label").assertIsDisplayed()
    }

    // ---- helpers

    private fun scrollToLayout(layout: LayoutId) {
        composeRule.onNodeWithTag("layout_list").performScrollToIndex(layout.ordinal)
    }

    private fun selectLayout(layout: LayoutId) {
        scrollToLayout(layout)
        composeRule.onNodeWithTag("layout_${layout.name}").performClick()
    }

    private fun openEditor() {
        composeRule.onNodeWithTag("btn_edit").performClick()
        composeRule.onNodeWithTag("editor_screen").assertIsDisplayed()
    }

}
