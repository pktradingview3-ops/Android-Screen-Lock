package com.timewall.app.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.timewall.app.data.ConfigStore
import com.timewall.app.render.WallpaperRenderer
import java.time.LocalDateTime

/**
 * Live wallpaper: black background, time and optional name.
 *
 * Reliability design:
 *  - Redraws at each minute boundary (Handler), and also on the system TIME_TICK,
 *    TIME_CHANGED and TIMEZONE_CHANGED broadcasts, so a delayed handler never leaves a stale time.
 *  - Draws only while visible. Receivers and callbacks are removed when hidden or destroyed.
 *  - A failing draw is logged and skipped. It never kills the engine, and the surface is always unlocked.
 */
class TimeWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = TimeEngine()

    private inner class TimeEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val handler = Handler(Looper.getMainLooper())
        private val prefs: SharedPreferences by lazy { ConfigStore.prefs(this@TimeWallpaperService) }

        private var visible = false
        private var receiverRegistered = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private val tick = Runnable { refresh() }

        private val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refresh()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // The engine keeps this listener alive; SharedPreferences only holds it weakly.
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            handler.removeCallbacks(tick)
            unregisterTimeReceiver()
            try {
                prefs.unregisterOnSharedPreferenceChangeListener(this)
            } catch (e: Exception) {
                Log.w(TAG, "unregister prefs listener failed", e)
            }
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                registerTimeReceiver()
                refresh()
            } else {
                handler.removeCallbacks(tick)
                unregisterTimeReceiver()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            refresh()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            // Config changed in the app: redraw straight away if visible.
            if (visible) draw()
        }

        /** Draws now and schedules the next minute boundary. */
        private fun refresh() {
            if (!visible) return
            draw()
            scheduleNextMinute()
        }

        private fun scheduleNextMinute() {
            handler.removeCallbacks(tick)
            val now = System.currentTimeMillis()
            // 50 ms after the next minute boundary, so the new minute is already current.
            val delay = 60_000L - (now % 60_000L) + 50L
            handler.postDelayed(tick, delay)
        }

        private fun registerTimeReceiver() {
            if (receiverRegistered) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            this@TimeWallpaperService.registerReceiver(timeReceiver, filter)
            receiverRegistered = true
        }

        private fun unregisterTimeReceiver() {
            if (!receiverRegistered) return
            try {
                this@TimeWallpaperService.unregisterReceiver(timeReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "receiver was not registered", e)
            }
            receiverRegistered = false
        }

        private fun draw() {
            if (!visible || surfaceWidth <= 0 || surfaceHeight <= 0) return
            val holder = surfaceHolder
            val canvas = try {
                holder.lockCanvas()
            } catch (e: Exception) {
                Log.w(TAG, "lockCanvas failed", e)
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
            } catch (e: Exception) {
                Log.e(TAG, "render failed", e)
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Log.w(TAG, "unlockCanvasAndPost failed", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "TimeWallWallpaper"
    }
}
