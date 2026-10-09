package com.timewall.app.applock

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * An app's launcher icon.
 *
 * Icons are loaded through PackageManager and drawn with Image(), so no image-loading
 * library (Coil/Glide) is needed - that would add several MB to the APK for something
 * that is already on disk. Results are cached per package for the life of the screen.
 *
 * If the icon cannot be loaded, the first letter of the label is drawn on a coloured
 * tile instead, so the row never collapses to an empty gap.
 */
@Composable
fun AppIcon(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) { loadAppIcon(context, packageName) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(size.dp)
                .clip(RoundedCornerShape(11.dp)),
        )
    } else {
        // Fallback tile with the first letter.
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.trim().take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Loads and rasterises an app icon. Returns null if anything goes wrong. */
private fun loadAppIcon(context: Context, packageName: String): Bitmap? = runCatching {
    val pm = context.packageManager
    val drawable: Drawable = pm.getApplicationIcon(packageName)
    drawable.toBitmap(96)
}.getOrNull()

/** Draws a Drawable into a Bitmap at the requested size, preserving transparency. */
private fun Drawable.toBitmap(sizePx: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    }
    val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return out
}

/** Kept for callers that only have a PackageManager. */
@Suppress("unused")
private fun PackageManager.labelFor(packageName: String): String =
    runCatching { getApplicationLabel(getApplicationInfo(packageName, 0)).toString() }
        .getOrDefault(packageName)
