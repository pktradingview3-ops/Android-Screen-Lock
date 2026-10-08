package com.timewall.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

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

private enum class Destination { LAYOUTS, EDITOR, HELP }

@Composable
fun TimeWallApp(vm: ConfigViewModel = viewModel()) {
    val config by vm.config.collectAsState()
    var destination by rememberSaveable { mutableStateOf(Destination.LAYOUTS) }

    BackHandler(enabled = destination != Destination.LAYOUTS) {
        destination = Destination.LAYOUTS
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            // Edge-to-edge: keep content clear of status bar and gesture bar.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (destination) {
            Destination.LAYOUTS -> LayoutPickerScreen(
                config = config,
                onSelect = { layout -> vm.update { it.copy(layout = layout) } },
                onEdit = { destination = Destination.EDITOR },
                onHelp = { destination = Destination.HELP },
            )
            Destination.EDITOR -> EditorScreen(
                config = config,
                onChange = { transform -> vm.update(transform) },
                onBack = { destination = Destination.LAYOUTS },
            )
            Destination.HELP -> HelpScreen(
                onBack = { destination = Destination.LAYOUTS },
            )
        }
    }
}
