package com.timewall.app.apply

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import com.timewall.app.domain.WallpaperConfig
import com.timewall.app.render.WallpaperRenderer
import com.timewall.app.wallpaper.TimeWallpaperService
import java.time.LocalDateTime

object WallpaperActions {

    /**
     * Opens the system live-wallpaper preview with TimeWall preselected.
     * The user confirms there. The app never changes the wallpaper silently.
     */
    fun openLiveWallpaperPreview(context: Context) {
        val component = ComponentName(context, TimeWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            component,
        )
        try {
            context.startActivity(direct)
        } catch (e: ActivityNotFoundException) {
            // Some OEMs do not handle the direct intent. Fall back to the wallpaper chooser.
            context.startActivity(
                Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Static fallback: renders the time NOW and sets it as the lock-screen image only.
     * The time in this image does not update; it is a snapshot. Call from a background thread.
     */
    fun setLockScreenSnapshot(context: Context, config: WallpaperConfig): Result<Unit> = runCatching {
        val wm = WallpaperManager.getInstance(context)
        val metrics = context.resources.displayMetrics
        val width = wm.desiredMinimumWidth.takeIf { it > 0 } ?: metrics.widthPixels
        val height = wm.desiredMinimumHeight.takeIf { it > 0 } ?: metrics.heightPixels

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            WallpaperRenderer.render(Canvas(bitmap), width, height, config, LocalDateTime.now())
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        } finally {
            bitmap.recycle()
        }
    }
}
