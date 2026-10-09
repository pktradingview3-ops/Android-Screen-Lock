package com.timewall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.timewall.app.security.PinPolicy

/** Dots showing how many digits have been entered (never the digits themselves). */
@Composable
fun PinDots(length: Int, tag: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag(tag),
    ) {
        repeat(PinPolicy.MAX_LENGTH) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < length) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

/**
 * Numeric keypad. Test tags are "<tagPrefix>key_<digit>", "<tagPrefix>delete",
 * "<tagPrefix>submit", and "<tagPrefix>dots".
 */
@Composable
fun PinPad(
    pin: String,
    enabled: Boolean,
    tagPrefix: String,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    val canType = enabled && pin.length < PinPolicy.MAX_LENGTH
    val canSubmit = enabled && pin.length >= PinPolicy.MIN_LENGTH

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PinDots(length = pin.length, tag = "${tagPrefix}dots")
        Spacer(Modifier.height(4.dp))
        listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9')).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    PinKey(
                        label = digit.toString(),
                        tag = "${tagPrefix}key_$digit",
                        enabled = canType,
                        onClick = { onDigit(digit) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PinKey(label = "Del", tag = "${tagPrefix}delete", enabled = enabled && pin.isNotEmpty(), onClick = onDelete)
            PinKey(label = "0", tag = "${tagPrefix}key_0", enabled = canType, onClick = { onDigit('0') })
            PinKey(label = "OK", tag = "${tagPrefix}submit", enabled = canSubmit, onClick = onSubmit)
        }
    }
}

@Composable
private fun PinKey(label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(width = 76.dp, height = 60.dp)
            .testTag(tag),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
