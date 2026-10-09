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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Home screen.
 *
 * The old header put the title and three text buttons in a single Row, which
 * overflowed on a narrow phone: the buttons were squeezed and the title wrapped
 * badly. The title now sits in a proper app bar and the actions live in their own
 * row of equal-width cards below it, so nothing competes for width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutPickerScreen(
    config: WallpaperConfig,
    onSelect: (LayoutId) -> Unit,
    onEdit: () -> Unit,
    onHelp: () -> Unit,
    onAppLock: () -> Unit,
    onLockApps: () -> Unit,
    appLockOn: Boolean = false,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TimeWall", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Choose a layout",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onHelp, modifier = Modifier.testTag("btn_help")) {
                        Icon(Icons.Rounded.Info, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("picker_screen"),
        ) {
            // Quick actions: two equal cards, so they fit any width.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_lock_apps"),
                    icon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    title = "Locked apps",
                    subtitle = "Protect other apps",
                    onClick = onLockApps,
                )
                QuickActionCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_applock"),
                    icon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    title = "TimeWall lock",
                    subtitle = if (appLockOn) "On" else "Off",
                    highlighted = appLockOn,
                    onClick = onAppLock,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("layout_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 12.dp,
                ),
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

            // Bottom bar: the primary action plus the version line.
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_edit"),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Edit and set wallpaper")
                }
                Text(
                    "TimeWall ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 10.dp)
                        .testTag("version_label"),
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    .width(96.dp)
                    .aspectRatio(9f / 19.5f),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(layout.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (layout.orientation == Orientation.VERTICAL) "Vertical" else "Horizontal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Text(
                            "Tap to select",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ============================================================ Editor

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(config.layout.label, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("editor_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WallpaperPreview(
                config = config,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .aspectRatio(9f / 19.5f)
                    .align(Alignment.CenterHorizontally),
            )

            SectionCard(title = "Clock") {
                Text("Time format", style = MaterialTheme.typography.titleSmall)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Show date", style = MaterialTheme.typography.titleSmall)
                        if (!config.layout.supportsDate) {
                            Text(
                                "This layout has no date line.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    supportingText = { Text("${config.name.length}/${WallpaperConfig.MAX_NAME_LENGTH}") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_field"),
                )
            }

            SectionCard(title = "Where to show TimeWall") {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "The phone's own clock") {
                Text(
                    "vivo can show its own clock on the lock screen too. TimeWall cannot hide it, because " +
                        "that is a phone setting. Open the lock screen settings below and change or turn off " +
                        "the clock there, then check the lock screen again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }

            status?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(14.dp)
                            .testTag("status"),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

// ============================================================ Help

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Help", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_help_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("help_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HelpBlock(
                "What this app does",
                "TimeWall draws a black wallpaper with a large bold time, in several layouts. " +
                    "It uses Android's official wallpaper system only.",
            )
            HelpBlock(
                "What this app does NOT do",
                "It does not replace or change your lock screen PIN, pattern, or fingerprint. " +
                    "It does not connect to the internet and does not collect data. " +
                    "It does not read what is on your screen.",
            )
            HelpBlock(
                "Locking your other apps",
                "Open Locked apps, turn it on, then pick the apps to protect. Each one asks for your " +
                    "PIN when it opens. This needs the accessibility permission, and nothing locks " +
                    "without it.\n\n" +
                    "The home screen, Settings and the installers can never be locked: doing so would " +
                    "lock you out of your own phone.",
            )
            HelpBlock(
                "How to set the live wallpaper",
                "1. Open Edit and tap \"Home and lock screen (live time)\".\n" +
                    "2. In the system preview, tap Set wallpaper.\n" +
                    "3. Choose Home and lock screen, or Lock screen only, if your phone offers it.\n" +
                    "4. Lock the phone and check the time.",
            )
            HelpBlock(
                "If the time does not show on the lock screen",
                "Some phones, including some vivo Funtouch OS versions, apply live wallpapers to " +
                    "Home and Lock together. Choose that option. If the phone stops the app in the " +
                    "background, set Battery > App battery usage to Unrestricted for TimeWall.",
            )
            HelpBlock(
                "Two clocks on the lock screen?",
                "Your vivo can show its own clock on the lock screen. TimeWall cannot turn it off, " +
                    "because it is a phone setting. Use Edit > Open lock screen settings, and change " +
                    "the clock style or turn it off if your phone allows it.",
            )
            HelpBlock(
                "Lock screen only",
                "Android does not allow a live clock on the lock screen alone on most phones. " +
                    "\"Lock screen only\" sets a still image with the time from when you tap. " +
                    "Tap it again to update the time.",
            )
            HelpBlock(
                "Forgot your PIN?",
                "There is no in-app reset, on purpose, because a reset would also be a way past the " +
                    "lock. Open Android Settings > Apps > TimeWall > Clear data. This removes your " +
                    "saved TimeWall settings too.",
            )
        }
    }
}

@Composable
private fun HelpBlock(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
