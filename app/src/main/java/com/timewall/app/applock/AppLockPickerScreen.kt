package com.timewall.app.applock

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.unit.dp

/**
 * Lets the user pick which installed apps are protected, and shows whether the
 * accessibility service is actually enabled (without it, nothing locks).
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
)

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    return runCatching {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            // Only apps the user can actually launch, and never ourselves.
            .filter { it.packageName != context.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || isLaunchableSystemApp(pm, it) }
            .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }.getOrDefault(emptyList())
}

private fun isLaunchableSystemApp(pm: PackageManager, info: ApplicationInfo): Boolean =
    pm.getLaunchIntentForPackage(info.packageName) != null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { AppLockTargetStore.from(context) }

    var enabled by remember { mutableStateOf(store.enabled) }
    var protected by remember { mutableStateOf(store.protectedPackages) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var serviceOn by remember { mutableStateOf(AppLockAccessibilityService.connected) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = loadInstalledApps(context)
        serviceOn = isServiceEnabled(context)
    }

    val visible = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lock apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ServiceStatusCard(
                    serviceOn = serviceOn,
                    onOpenSettings = { context.openAccessibilitySettings() },
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock apps", fontWeight = FontWeight.Bold)
                            Text(
                                if (protected.isEmpty()) "No apps selected yet"
                                else "${protected.size} app(s) locked",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { on ->
                                enabled = on
                                store.enabled = on
                            },
                            modifier = Modifier.testTag("applock_enabled_switch"),
                        )
                    }
                }
            }

            item {
                Text(
                    "Pick the apps to protect. Each one asks for your PIN when it opens.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            items(visible, key = { it.packageName }) { app ->
                val checked = app.packageName in protected
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_row_${app.packageName}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) {
                                    store.unprotect(app.packageName)
                                    protected = store.protectedPackages
                                } else {
                                    store.protect(app.packageName)
                                    protected = store.protectedPackages
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                            Text(app.packageName, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { on ->
                                if (on) store.protect(app.packageName) else store.unprotect(app.packageName)
                                protected = store.protectedPackages
                            },
                        )
                    }
                }
            }

            if (visible.isEmpty() && apps.isNotEmpty()) {
                item { Text("No app matches that name.", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(serviceOn: Boolean, onOpenSettings: () -> Unit) {
    val tint = if (serviceOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (serviceOn) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    if (serviceOn) "App lock is active" else "App lock is off",
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                if (serviceOn) {
                    "TimeWall can see which app opened, so it can lock the ones you picked."
                } else {
                    "Turn on the TimeWall app lock in Android's accessibility settings. Without it, no app can be locked."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (!serviceOn) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open accessibility settings")
                }
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
