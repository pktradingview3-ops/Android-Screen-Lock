package com.timewall.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.timewall.app.BuildConfig
import com.timewall.app.apply.WallpaperActions
import com.timewall.app.domain.LayoutId
import com.timewall.app.domain.Orientation
import com.timewall.app.domain.WallpaperConfig
import kotlinx.coroutines.Dispatchers
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================ Layout picker (home)

@Composable
fun LayoutPickerScreen(
    config: WallpaperConfig,
    onSelect: (LayoutId) -> Unit,
    onEdit: () -> Unit,
    onHelp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("picker_screen"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Choose a layout",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onHelp, modifier = Modifier.testTag("btn_help")) {
                Text("Help")
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag("layout_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(LayoutId.values().toList(), key = { it.name }) { layout ->
                LayoutCard(
                    layout = layout,
                    selected = layout == config.layout,
                    previewConfig = config.copy(layout = layout),
                    onClick = { onSelect(layout) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_edit"),
        ) {
            Text("Edit and set wallpaper")
        }
        Text(
            "TimeWall ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("version_label"),
        )
    }
}

@Composable
private fun LayoutCard(
    layout: LayoutId,
    selected: Boolean,
    previewConfig: WallpaperConfig,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("layout_${layout.name}")
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WallpaperPreview(
                config = previewConfig,
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(9f / 19.5f),
            )
            Column(Modifier.weight(1f)) {
                Text(layout.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (layout.orientation == Orientation.VERTICAL) "Vertical" else "Horizontal",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (selected) {
                    Text("Selected", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("Tap to select", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ============================================================ Editor

@Composable
fun EditorScreen(
    config: WallpaperConfig,
    onChange: ((WallpaperConfig) -> WallpaperConfig) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("editor_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
            Text("Back to layouts")
        }
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
                modifier = Modifier.testTag("chip_12h"),
            )
            FilterChip(
                selected = config.use24h,
                onClick = { onChange { it.copy(use24h = true) } },
                label = { Text("24-hour") },
                modifier = Modifier.testTag("chip_24h"),
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
                modifier = Modifier.testTag("switch_date"),
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("name_field"),
        )

        HorizontalDivider()

        Text("Where to show TimeWall", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = {
                WallpaperActions.openLiveWallpaperPreview(context)
                    .onFailure {
                        status = "This phone has no wallpaper picker. Set it from Settings > Wallpaper."
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_live"),
        ) {
            Text("Home and lock screen (live time)")
        }
        Text(
            "Recommended. The time updates every minute. The system preview opens and you confirm there.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedButton(
            enabled = !busy,
            onClick = {
                busy = true
                status = "Setting lock screen image..."
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        WallpaperActions.setLockScreenSnapshot(context, config)
                    }
                    busy = false
                    status = result.fold(
                        onSuccess = {
                            val at = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                            "Lock screen image set at $at. Its time does not update. Tap again to refresh it."
                        },
                        onFailure = { "Could not set lock screen image: ${it.message}" },
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_static"),
        ) {
            Text("Lock screen only (still image)")
        }
        Text(
            "Use this to show TimeWall only on the lock screen. Android cannot keep a live clock on " +
                "the lock screen alone, so this image shows the time from when you tap. The home " +
                "screen stays as it was.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("The phone's own clock", style = MaterialTheme.typography.titleMedium)
        Text(
            "vivo can show its own clock on the lock screen too. TimeWall cannot hide it, because " +
                "that is a phone setting. Open the lock screen settings below and change or turn off " +
                "the clock there, then check the lock screen again.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = {
                WallpaperActions.openLockScreenSettings(context)
                    .onFailure {
                        status = "Could not open settings. Open Settings > Lock screen, home screen and wallpaper."
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_lock_settings"),
        ) {
            Text("Open lock screen settings")
        }

        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("status"),
            )
        }
    }
}

// ============================================================ Help

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("help_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("btn_help_back")) {
            Text("Back to layouts")
        }
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
            "1. Open Edit and tap \"Set live wallpaper\".\n" +
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

        Text("Two clocks on the lock screen?", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your vivo can show its own clock on the lock screen. TimeWall cannot turn it off, " +
                "because it is a phone setting. Use Edit > Open lock screen settings, and change " +
                "the clock style or turn it off if your phone allows it.",
        )

        Text("Lock screen only", style = MaterialTheme.typography.titleMedium)
        Text(
            "Android does not allow a live clock on the lock screen alone on most phones. " +
                "\"Lock screen only\" sets a still image with the time from when you tap. " +
                "Tap it again to update the time.",
        )
    }
}
