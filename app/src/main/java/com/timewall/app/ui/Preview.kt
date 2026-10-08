package com.timewall.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.timewall.app.domain.WallpaperConfig
import com.timewall.app.render.WallpaperRenderer
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Draws the exact same output as the live wallpaper, using the shared renderer. */
@Composable
fun WallpaperPreview(config: WallpaperConfig, modifier: Modifier = Modifier) {
    val now = remember(config) { LocalDateTime.now() }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
    ) {
        drawIntoCanvas { canvas ->
            WallpaperRenderer.render(
                canvas = canvas.nativeCanvas,
                width = size.width.roundToInt(),
                height = size.height.roundToInt(),
                config = config,
                now = now,
            )
        }
    }
}
