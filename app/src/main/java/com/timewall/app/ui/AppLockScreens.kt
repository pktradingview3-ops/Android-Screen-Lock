package com.timewall.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.timewall.app.security.AppSession
import com.timewall.app.security.BiometricGate
import com.timewall.app.security.PinPolicy
import com.timewall.app.security.VerifyOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================ Lock screen (shown when locked)

@Composable
fun AppLockScreen(vm: AppLockViewModel) {
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    val scope = rememberCoroutineScope()
    val biometricOn by vm.biometric.collectAsState()
    val canUseBiometric = BiometricGate.canUse(context)

    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lockedUntilMs by remember { mutableLongStateOf(System.currentTimeMillis() + vm.lockoutRemainingMs()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Countdown while locked out. Re-runs whenever a new lockout starts.
    LaunchedEffect(lockedUntilMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            if (lockedUntilMs <= nowMs) break
            delay(500)
        }
    }

    val remainingMs = (lockedUntilMs - nowMs).coerceAtLeast(0L)
    val lockedOut = remainingMs > 0L

    fun submit() {
        if (busy || lockedOut || pin.length < PinPolicy.MIN_LENGTH) return
        val entered = pin
        pin = ""
        busy = true
        scope.launch {
            val outcome = vm.verify(entered)
            busy = false
            when (outcome) {
                VerifyOutcome.Success -> {
                    message = null
                    AppSession.unlock()
                }
                is VerifyOutcome.Wrong -> message = "Wrong PIN. ${outcome.attemptsLeft} attempt(s) left."
                is VerifyOutcome.LockedOut -> {
                    lockedUntilMs = outcome.untilWallMs
                    nowMs = System.currentTimeMillis()
                    message = "Too many wrong attempts. Wait before trying again."
                }
            }
        }
    }

    fun tryFingerprint() {
        if (activity == null) return
        BiometricGate.prompt(
            activity = activity,
            onSuccess = { AppSession.unlock() },
            onError = { message = it },
        )
    }

    // Offer the fingerprint prompt once when the lock screen appears, if the user enabled it.
    LaunchedEffect(Unit) {
        if (biometricOn && canUseBiometric) tryFingerprint()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lock_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("TimeWall is locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Enter your PIN to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(26.dp))

            PinPad(
                pin = pin,
                enabled = !busy && !lockedOut,
                tagPrefix = "lock_",
                onDigit = { digit -> if (pin.length < PinPolicy.MAX_LENGTH) pin = pin + digit },
                onDelete = { pin = pin.dropLast(1) },
                onSubmit = { submit() },
            )

            Spacer(Modifier.height(16.dp))
            message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("lock_error"),
                )
            }
            if (lockedOut) {
                val seconds = (remainingMs + 999L) / 1000L
                Spacer(Modifier.height(4.dp))
                Text(
                    "Try again in $seconds second(s).",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("lock_countdown"),
                )
            }

            if (biometricOn && canUseBiometric) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { tryFingerprint() },
                    modifier = Modifier.testTag("lock_fingerprint"),
                ) {
                    Text("Use fingerprint")
                }
            }

            Spacer(Modifier.height(26.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Forgot your PIN? Open Android Settings > Apps > TimeWall > Clear data. " +
                            "This also deletes your saved TimeWall settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ============================================================ App lock settings

private enum class SetupStep { MENU, NEW_PIN, CONFIRM_PIN, VERIFY_FOR_CHANGE, VERIFY_FOR_DISABLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(vm: AppLockViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by vm.enabled.collectAsState()
    val biometricOn by vm.biometric.collectAsState()
    val canUseBiometric = BiometricGate.canUse(context)

    var step by remember { mutableStateOf(SetupStep.MENU) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun returnToMenu() {
        step = SetupStep.MENU
        pin = ""
        firstPin = ""
    }

    // Back inside a PIN step returns to the menu instead of leaving the screen.
    BackHandler(enabled = step != SetupStep.MENU) {
        returnToMenu()
        message = null
    }

    fun submit() {
        if (busy) return
        val entered = pin
        when (step) {
            SetupStep.NEW_PIN -> {
                if (PinPolicy.isValidPin(entered)) {
                    firstPin = entered
                    pin = ""
                    message = null
                    step = SetupStep.CONFIRM_PIN
                } else {
                    message = "PIN must be 4 to 6 digits."
                }
            }
            SetupStep.CONFIRM_PIN -> {
                if (entered != firstPin) {
                    returnToMenu()
                    step = SetupStep.NEW_PIN
                    message = "PINs do not match. Enter a new PIN."
                } else {
                    busy = true
                    scope.launch {
                        val result = vm.setPin(entered)
                        busy = false
                        returnToMenu()
                        message = result.fold(
                            onSuccess = { "App lock is on." },
                            onFailure = { "Could not save the PIN. Try again." },
                        )
                    }
                }
            }
            SetupStep.VERIFY_FOR_CHANGE, SetupStep.VERIFY_FOR_DISABLE -> {
                val current = step
                pin = ""
                busy = true
                scope.launch {
                    val outcome = vm.verify(entered)
                    busy = false
                    when (outcome) {
                        VerifyOutcome.Success -> {
                            if (current == SetupStep.VERIFY_FOR_CHANGE) {
                                pin = ""
                                step = SetupStep.NEW_PIN
                                message = "Enter the new PIN."
                            } else {
                                vm.disableLock()
                                returnToMenu()
                                message = "App lock is off."
                            }
                        }
                        is VerifyOutcome.Wrong -> message = "Wrong PIN. ${outcome.attemptsLeft} attempt(s) left."
                        is VerifyOutcome.LockedOut -> message = "Too many wrong attempts. Wait before trying again."
                    }
                }
            }
            SetupStep.MENU -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (step) {
                                SetupStep.MENU -> "TimeWall lock"
                                SetupStep.NEW_PIN -> "New PIN"
                                SetupStep.CONFIRM_PIN -> "Confirm PIN"
                                SetupStep.VERIFY_FOR_CHANGE -> "Current PIN"
                                SetupStep.VERIFY_FOR_DISABLE -> "Turn off"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (enabled) "On" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step != SetupStep.MENU) {
                                returnToMenu()
                                message = null
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_applock_back"),
                    ) {
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
                .testTag("applock_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (step == SetupStep.MENU) {
                StatusCard(enabled = enabled)
                Spacer(Modifier.height(2.dp))
            } else {
                Text(
                    when (step) {
                        SetupStep.NEW_PIN -> "Choose a 4 to 6 digit PIN."
                        SetupStep.CONFIRM_PIN -> "Enter the same PIN again."
                        SetupStep.VERIFY_FOR_CHANGE -> "Enter your current PIN."
                        SetupStep.VERIFY_FOR_DISABLE -> "Enter your PIN to turn app lock off."
                        SetupStep.MENU -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            when (step) {
                SetupStep.MENU -> {
                    if (enabled) {
                        Button(
                            onClick = {
                                message = null
                                pin = ""
                                step = SetupStep.VERIFY_FOR_CHANGE
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_applock_change"),
                        ) { Text("Change PIN") }

                        OutlinedButton(
                            onClick = {
                                vm.lockNow()
                                // Leave settings so that after unlocking the user lands on the layout list.
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_applock_now"),
                        ) { Text("Lock TimeWall now") }

                        OutlinedButton(
                            onClick = {
                                message = null
                                pin = ""
                                step = SetupStep.VERIFY_FOR_DISABLE
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_applock_off"),
                        ) { Text("Turn off app lock") }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Fingerprint too", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (canUseBiometric) "Unlock with fingerprint. The PIN always works too."
                                    else "No fingerprint is set up on this phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = biometricOn,
                                enabled = canUseBiometric,
                                onCheckedChange = { vm.setBiometric(it) },
                                modifier = Modifier.testTag("switch_applock_bio"),
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                message = null
                                pin = ""
                                step = SetupStep.NEW_PIN
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_applock_on"),
                        ) { Text("Turn on app lock") }
                    }
                }
                else -> {
                    PinPad(
                        pin = pin,
                        enabled = !busy,
                        tagPrefix = "setpin_",
                        onDigit = { digit -> if (pin.length < PinPolicy.MAX_LENGTH) pin = pin + digit },
                        onDelete = { pin = pin.dropLast(1) },
                        onSubmit = { submit() },
                    )
                }
            }

            message?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(14.dp)
                            .testTag("applock_message"),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(enabled: Boolean) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (enabled) "App lock is on" else "App lock is off",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("applock_status"),
                )
                Text(
                    if (enabled) {
                        "TimeWall asks for your PIN when you open it again after leaving it for more " +
                            "than ${AppSession.GRACE_MS / 1000} seconds."
                    } else {
                        "Turn it on to protect TimeWall itself with a PIN."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
