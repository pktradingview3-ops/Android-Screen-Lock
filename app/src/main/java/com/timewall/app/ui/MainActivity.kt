package com.timewall.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timewall.app.security.AppSession

/**
 * FragmentActivity (not ComponentActivity) because the fingerprint prompt needs it.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeWallTheme {
                TimeWallApp()
            }
        }
    }
}

private enum class Destination { LAYOUTS, EDITOR, HELP, APP_LOCK, APP_LOCK_PICKER }

@Composable
fun TimeWallApp(
    vm: ConfigViewModel = viewModel(),
    lockVm: AppLockViewModel = viewModel(),
) {
    val config by vm.config.collectAsState()
    val locked by AppSession.locked.collectAsState()
    val lockEnabled by lockVm.enabled.collectAsState()
    var destination by rememberSaveable { mutableStateOf(Destination.LAYOUTS) }

    // With app lock on, block screenshots and hide the preview in the recents screen.
    val activity = LocalContext.current.findActivity()
    LaunchedEffect(lockEnabled) {
        val window = activity?.window ?: return@LaunchedEffect
        if (lockEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // System back goes to the layout list from any sub-screen (instead of closing the app).
    BackHandler(enabled = !locked && destination != Destination.LAYOUTS) {
        destination = Destination.LAYOUTS
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            // Edge-to-edge: keep content clear of the status bar and gesture bar.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when {
            locked -> AppLockScreen(vm = lockVm)
            else -> when (destination) {
                Destination.LAYOUTS -> LayoutPickerScreen(
                    config = config,
                    onSelect = { layout -> vm.update { it.copy(layout = layout) } },
                    onEdit = { destination = Destination.EDITOR },
                    onHelp = { destination = Destination.HELP },
                    onAppLock = { destination = Destination.APP_LOCK },
                    onLockApps = { destination = Destination.APP_LOCK_PICKER },
                    appLockOn = lockEnabled,
                )
                Destination.EDITOR -> EditorScreen(
                    config = config,
                    onChange = { transform -> vm.update(transform) },
                    onBack = { destination = Destination.LAYOUTS },
                )
                Destination.HELP -> HelpScreen(
                    onBack = { destination = Destination.LAYOUTS },
                )
                Destination.APP_LOCK -> AppLockSettingsScreen(
                    vm = lockVm,
                    onBack = { destination = Destination.LAYOUTS },
                )
                Destination.APP_LOCK_PICKER -> com.timewall.app.applock.AppLockPickerScreen(
                    onBack = { destination = Destination.LAYOUTS },
                )
            }
        }
    }
}
