package com.timewall.app.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Lets the user pick which installed apps are protected, and shows whether the
 * accessibility service is actually enabled (without it, nothing locks).
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    /** True for apps that must never be locked, because locking them breaks the phone. */
    val isUnsafeToLock: Boolean = false,
)

/** Launcher packages are found by category too, so OEM renames are still caught. */
private fun isLauncher(pm: PackageManager, packageName: String): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return pm.queryIntentActivities(intent, 0).any { it.activityInfo?.packageName == packageName }
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    return runCatching {
        // Build the list from launcher activities rather than getInstalledApplications.
        // On Android 11+ the launcher-intent query is the reliable enumeration, and it is
        // exactly the set a user would want to lock: apps with a launcher icon. The
        // <queries> block in the manifest is what makes it complete.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it != context.packageName }   // never offer our own app
            .mapNotNull { packageName ->
                runCatching {
                    val info = pm.getApplicationInfo(packageName, 0)
                    InstalledApp(
                        packageName = packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        isUnsafeToLock = packageName in UNSAFE_TO_LOCK || isLauncher(pm, packageName),
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy({ it.isUnsafeToLock }, { it.label.lowercase() }))
            .toList()
    }.getOrDefault(emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { AppLockTargetStore.from(context) }

    var enabled by remember { mutableStateOf(store.enabled) }
    var protected by remember { mutableStateOf(store.protectedPackages) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var serviceOn by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // Re-check the service state whenever this screen comes back into view, because the
    // user may have just toggled it in system settings and returned.
    LaunchedEffect(Unit) {
        apps = loadInstalledApps(context)
        serviceOn = isServiceEnabled(context)
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }
    val lockedApps = remember(filtered, protected) {
        filtered.filter { it.packageName in protected && !it.isUnsafeToLock }
    }
    val rest = remember(filtered, protected) {
        filtered.filterNot { it.packageName in protected && !it.isUnsafeToLock }
    }

    fun toggle(app: InstalledApp) {
        if (app.packageName in protected) store.unprotect(app.packageName)
        else store.protect(app.packageName)
        protected = store.protectedPackages
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lock apps", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (protected.isEmpty()) "No apps selected"
                            else "${protected.size} selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_lockapps_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ---- master switch ------------------------------------------------
            item {
                MasterSwitchCard(
                    enabled = enabled,
                    lockedCount = protected.count { pkg -> apps.any { it.packageName == pkg && !it.isUnsafeToLock } },
                    onChange = { on ->
                        enabled = on
                        store.enabled = on
                    },
                )
            }

            // ---- service state ------------------------------------------------
            item {
                ServiceStatusCard(
                    serviceOn = serviceOn,
                    onOpenSettings = { context.openAccessibilitySettings() },
                    onRecheck = { serviceOn = isServiceEnabled(context) },
                )
            }

            // ---- search -------------------------------------------------------
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search"),
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                )
            }

            // ---- empty state --------------------------------------------------
            if (apps.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No apps found",
                        body = "Android did not return any launchable apps. Pull to reopen this screen, or check that the app has the package-visibility declarations it needs.",
                    )
                }
            }

            // ---- locked apps --------------------------------------------------
            if (lockedApps.isNotEmpty()) {
                item { SectionHeader("Locked", lockedApps.size) }
                items(lockedApps, key = { "locked_${it.packageName}" }) { app ->
                    AppRow(app = app, checked = true, enabled = enabled, onToggle = { toggle(app) })
                }
            }

            // ---- everything else ----------------------------------------------
            if (rest.isNotEmpty()) {
                item {
                    SectionHeader(
                        if (query.isBlank()) "All apps" else "Results",
                        rest.size,
                    )
                }
                items(rest, key = { "all_${it.packageName}" }) { app ->
                    AppRow(app = app, checked = false, enabled = enabled, onToggle = { toggle(app) })
                }
            }

            if (filtered.isEmpty() && apps.isNotEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Nothing matches \"$query\"",
                        body = "Try a shorter search, or clear the search box to see every app.",
                    )
                }
            }

            // ---- footer -------------------------------------------------------
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Locking the home screen, Settings or the installers is not allowed: it would " +
                        "lock you out of your own phone. Forgot your PIN? Settings > Apps > TimeWall " +
                        "> Clear data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============================================================ pieces

@Composable
private fun MasterSwitchCard(enabled: Boolean, lockedCount: Int, onChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (enabled) "App lock is on" else "App lock is off",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    when {
                        !enabled && lockedCount > 0 -> "$lockedCount app(s) chosen, but nothing is locked while this is off"
                        !enabled -> "Turn on to lock the apps you pick"
                        lockedCount == 0 -> "Now pick the apps below"
                        else -> "$lockedCount app(s) will ask for your PIN"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                modifier = Modifier.testTag("applock_enabled_switch"),
            )
        }
    }
}

@Composable
private fun ServiceStatusCard(
    serviceOn: Boolean,
    onOpenSettings: () -> Unit,
    onRecheck: () -> Unit,
) {
    val tint = if (serviceOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (serviceOn) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (serviceOn) "Android permission granted" else "Android permission needed",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (serviceOn) {
                    "TimeWall can see which app opened, so it can lock the ones you picked. It never reads what is on the screen."
                } else {
                    "Nothing can be locked until you turn on \"TimeWall app lock\" in Android's accessibility settings."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (!serviceOn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_a11y"),
                    ) { Text("Open settings") }
                    TextButton(onClick = onRecheck, modifier = Modifier.testTag("btn_recheck_a11y")) {
                        Text("Re-check")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val unsafe = app.isUnsafeToLock
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_row_${app.packageName}"),
        colors = CardDefaults.cardColors(
            containerColor = when {
                unsafe -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                checked -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !unsafe) { onToggle() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = app.packageName, label = app.label)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (unsafe) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    when {
                        unsafe -> "Cannot be locked — needed to unlock the phone or fix the app"
                        checked -> "Asks for your PIN"
                        else -> app.packageName
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        unsafe -> MaterialTheme.colorScheme.error
                        checked -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = checked && !unsafe,
                enabled = !unsafe && enabled,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(Modifier.padding(20.dp)) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** True when the user has actually enabled our accessibility service in system settings. */
fun isServiceEnabled(context: Context): Boolean = runCatching {
    val expected = "${context.packageName}/${AppLockAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}.getOrDefault(false)

private fun Context.openAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
}
