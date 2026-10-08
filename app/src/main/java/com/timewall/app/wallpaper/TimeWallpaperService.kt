package com.timewall.app.wallpaper

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.timewall.app.data.ConfigStore
import com.timewall.app.render.WallpaperRenderer
import java.time.LocalDateTime

/**
 * Live wallpaper: draws the black background, time and optional name, and redraws at each
 * minute boundary. It only draws while visible, so it stays cheap in the background.
 *
 * Note: whether this shows on the lock screen alone, or only together with the home screen,
 * depends on the phone's launcher/OEM. Verified on vivo Y31 in Phase 0 (see docs/device-test.md).
 */
class TimeWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = TimeEngine()

    private inner class TimeEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val handler = Handler(Looper.getMainLooper())
        private val prefs: SharedPreferences by lazy { ConfigStore.prefs(this@TimeWallpaperService) }

        private val tick = Runnable {
            draw()
            scheduleNextMinute()
        }

        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Keep a strong reference to the listener in this engine (prefs holds it weakly).
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            handler.removeCallbacks(tick)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                draw()
                scheduleNextMinute()
            } else {
                handler.removeCallbacks(tick)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            // Config changed in the app: redraw straight away if visible.
            if (visible) draw()
        }

        private fun scheduleNextMinute() {
            handler.removeCallbacks(tick)
            if (!visible) return
            val now = System.currentTimeMillis()
            // 50 ms after the next minute boundary, so the new minute is already current.
            val delay = 60_000L - (now % 60_000L) + 50L
            handler.postDelayed(tick, delay)
        }

        private fun draw() {
            if (!visible || surfaceWidth <= 0 || surfaceHeight <= 0) return
            val holder = surfaceHolder
            val canvas = try {
                holder.lockCanvas()
            } catch (e: Exception) {
                null
            } ?: return
            try {
                WallpaperRenderer.render(
                    canvas = canvas,
                    width = surfaceWidth,
                    height = surfaceHeight,
                    config = ConfigStore.read(prefs),
                    now = LocalDateTime.now(),
                )
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
