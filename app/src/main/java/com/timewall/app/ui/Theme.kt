package com.timewall.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TimeWall theme.
 *
 * The app is about a black wallpaper with bold white text, so the palette stays close
 * to that: a near-black surface and one bright accent for selection. Light mode is
 * supported too, because forcing dark made these settings screens clash with the
 * system UI when the phone is in light mode.
 */
private val Accent = Color(0xFF9FE870)
private val AccentContainer = Color(0xFF1F3D12)
private val AccentLight = Color(0xFF2E7D32)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF10240A),
    primaryContainer = AccentContainer,
    onPrimaryContainer = Accent,
    secondary = Color(0xFFB9C6B0),
    secondaryContainer = Color(0xFF2A3327),
    onSecondaryContainer = Color(0xFFD6E4CC),
    background = Color(0xFF0C0E0B),
    onBackground = Color(0xFFE8EBE4),
    surface = Color(0xFF121511),
    onSurface = Color(0xFFE8EBE4),
    surfaceVariant = Color(0xFF1D211B),
    onSurfaceVariant = Color(0xFFB3BAAE),
    outline = Color(0xFF3A4038),
    outlineVariant = Color(0xFF262B23),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F0B4),
    onPrimaryContainer = Color(0xFF0A1F04),
    secondaryContainer = Color(0xFFE3E8DD),
    onSecondaryContainer = Color(0xFF1B2118),
    background = Color(0xFFF7F9F4),
    onBackground = Color(0xFF191C17),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C17),
    surfaceVariant = Color(0xFFEDF1E7),
    onSurfaceVariant = Color(0xFF454B41),
    outline = Color(0xFF757C70),
    outlineVariant = Color(0xFFC5CBC0),
)

/** Slightly tighter type scale, so long app names and labels fit narrow screens. */
private val TimeWallTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodySmall = base.bodySmall.copy(lineHeight = 17.sp),
    )
}

@Composable
fun TimeWallTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = TimeWallTypography,
        content = content,
    )
}
