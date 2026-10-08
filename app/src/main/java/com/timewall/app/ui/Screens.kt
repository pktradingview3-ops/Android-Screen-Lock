package com.timewall.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.timewall.app.apply.WallpaperActions
import com.timewall.app.domain.LayoutId
import com.timewall.app.domain.Orientation
import com.timewall.app.domain.WallpaperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- Layout picker

@Composable
fun LayoutPickerScreen(
    config: WallpaperConfig,
    onSelect: (LayoutId) -> Unit,
    onEdit: () -> Unit,
    onHelp: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Choose a layout", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onHelp) { Text("Help") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(LayoutId.values().toList()) { layout ->
                val selected = layout == config.layout
                val previewConfig = config.copy(layout = layout)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(layout) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WallpaperPreview(
                            config = previewConfig,
                            modifier = Modifier.width(110.dp).aspectRatio(9f / 19.5f),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(layout.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (layout.orientation == Orientation.VERTICAL) "Vertical" else "Horizontal",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (selected) {
                                Text("Selected", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Edit and set wallpaper")
        }
    }
}

// ---------------------------------------------------------------- Editor

@Composable
fun EditorScreen(
    config: WallpaperConfig,
    onChange: ((WallpaperConfig) -> WallpaperConfig) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(config.layout.label, style = MaterialTheme.typography.titleLarge)

        WallpaperPreview(
            config = config,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(9f / 19.5f)
                .align(Alignment.CenterHorizontally),
        )

        HorizontalDivider()

        Text("Time format", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !config.use24h,
                onClick = { onChange { it.copy(use24h = false) } },
                label = { Text("12-hour") },
            )
            FilterChip(
                selected = config.use24h,
                onClick = { onChange { it.copy(use24h = true) } },
                label = { Text("24-hour") },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Show date", style = MaterialTheme.typography.titleMedium)
                if (!config.layout.supportsDate) {
                    Text(
                        "This layout has no date line.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Switch(
                checked = config.isDateVisible,
                enabled = config.layout.supportsDate,
                onCheckedChange = { checked -> onChange { it.copy(showDate = checked) } },
            )
        }

        OutlinedTextField(
            value = config.name,
            onValueChange = { value ->
                onChange { it.copy(name = value.take(WallpaperConfig.MAX_NAME_LENGTH)) }
            },
            label = { Text("Name or text (optional)") },
            supportingText = {
                Text("${config.name.length}/${WallpaperConfig.MAX_NAME_LENGTH}")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Text("Apply", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = { WallpaperActions.openLiveWallpaperPreview(context) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set live wallpaper (recommended)")
        }
        Text(
            "Time updates every minute. The system preview opens; you confirm there. " +
                "On some phones the live wallpaper applies to Home and Lock screen together.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedButton(
            onClick = {
                status = "Setting lock screen image..."
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        WallpaperActions.setLockScreenSnapshot(context, config)
                    }
                    status = result.fold(
                        onSuccess = { "Lock screen image set. The time in it will NOT update." },
                        onFailure = { "Could not set lock screen image: ${it.message}" },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set lock screen snapshot (static)")
        }
        Text(
            "Fallback only. Applies immediately, and the time is frozen at the moment you tap.",
            style = MaterialTheme.typography.bodySmall,
        )

        status?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start) }
    }
}

// ---------------------------------------------------------------- Help

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Help", style = MaterialTheme.typography.titleLarge)

        Text("What this app does", style = MaterialTheme.typography.titleMedium)
        Text(
            "TimeWall draws a black wallpaper with a large bold time, in several layouts. " +
                "It uses Android's official wallpaper system only.",
        )

        Text("What this app does NOT do", style = MaterialTheme.typography.titleMedium)
        Text(
            "It does not replace or change your lock screen PIN, pattern, or fingerprint. " +
                "It does not use accessibility, overlay, or device-admin permissions. " +
                "It does not connect to the internet and does not collect data.",
        )

        Text("How to set the live wallpaper", style = MaterialTheme.typography.titleMedium)
        Text(
            "1. Open Editor and tap \"Set live wallpaper\".\n" +
                "2. In the system preview, tap Set wallpaper.\n" +
                "3. Choose Home and lock screen, or Lock screen only, if your phone offers it.\n" +
                "4. Lock the phone and check the time.",
        )

        Text("If the time does not show on the lock screen", style = MaterialTheme.typography.titleMedium)
        Text(
            "Some phones, including some vivo Funtouch OS versions, apply live wallpapers to " +
                "Home and Lock together. Choose that option. If the phone stops the app in the " +
                "background, set Battery > App battery usage to Unrestricted for TimeWall.",
        )

        Text("Lock screen snapshot", style = MaterialTheme.typography.titleMedium)
        Text(
            "This sets a still image with the time at the moment you tap. Use it only if the " +
                "live wallpaper does not work on your phone.",
        )
    }
}
