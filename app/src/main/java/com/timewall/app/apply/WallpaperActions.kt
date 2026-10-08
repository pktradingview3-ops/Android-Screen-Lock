package com.timewall.app.apply

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import com.timewall.app.domain.WallpaperConfig
import com.timewall.app.render.WallpaperRenderer
import com.timewall.app.wallpaper.TimeWallpaperService
import java.time.LocalDateTime

/**
 * Every action returns a Result, so the UI can show a clear message instead of crashing.
 */
object WallpaperActions {

    /**
     * Opens the system live-wallpaper preview with TimeWall preselected.
     * The user confirms there; the app never changes the wallpaper silently.
     */
    fun openLiveWallpaperPreview(context: Context): Result<Unit> = runCatching {
        val component = ComponentName(context, TimeWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(direct)
        } catch (e: ActivityNotFoundException) {
            // Some phones do not handle the direct intent. Fall back to the generic chooser.
            val chooser = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser) // throws if there is no picker at all; caller shows a message
        }
    }

    /**
     * Opens the phone's lock-screen / security settings, where the user can change or hide the
     * phone's own lock-screen clock. TimeWall cannot change that setting itself.
     */
    fun openLockScreenSettings(context: Context): Result<Unit> = runCatching {
        try {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Static lock-screen image: renders the time NOW and sets it as the lock-screen image only.
     * The time in that image does not update. Must be called off the main thread.
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
