package com.timewall.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timewall.app.domain.LayoutId
import com.timewall.app.domain.WallpaperConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Checks the real drawing output on a device, using the same renderer as the wallpaper. */
@RunWith(AndroidJUnit4::class)
class WallpaperRendererTest {

    private val width = 540
    private val height = 1200
    private val now = LocalDateTime.of(2026, 10, 8, 20, 7)

    private fun render(config: WallpaperConfig, time: LocalDateTime = now): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        WallpaperRenderer.render(Canvas(bitmap), width, height, config, time)
        return bitmap
    }

    private fun whitePixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val p = bitmap.getPixel(x, y)
                if (Color.red(p) > 200 && Color.green(p) > 200 && Color.blue(p) > 200) count++
            }
        }
        return count
    }

    @Test
    fun everyLayout_isBlackBackgroundWithWhiteText() {
        LayoutId.values().forEach { layout ->
            val bitmap = render(WallpaperConfig(layout = layout))
            assertEquals("background must be black for $layout", Color.BLACK, bitmap.getPixel(2, 2))
            assertTrue("layout $layout should draw white text", whitePixels(bitmap) > 1000)
        }
    }

    @Test
    fun timeChange_changesTheImage() {
        val a = render(WallpaperConfig(layout = LayoutId.L1), LocalDateTime.of(2026, 10, 8, 20, 7))
        val b = render(WallpaperConfig(layout = LayoutId.L1), LocalDateTime.of(2026, 10, 8, 20, 8))
        assertFalse(a.sameAs(b))
    }

    @Test
    fun dateToggle_addsPixelsOnLayoutsWithDate() {
        val withDate = render(WallpaperConfig(layout = LayoutId.L1, showDate = true))
        val withoutDate = render(WallpaperConfig(layout = LayoutId.L1, showDate = false))
        assertTrue(whitePixels(withDate) > whitePixels(withoutDate))
    }

    @Test
    fun longName_isCappedAndDoesNotCrash() {
        val bitmap = render(WallpaperConfig(layout = LayoutId.L4, name = "X".repeat(200)))
        assertTrue(whitePixels(bitmap) > 1000)
    }

    @Test
    fun emptyNameDrawsNothingExtra() {
        val none = render(WallpaperConfig(layout = LayoutId.L2, name = ""))
        val named = render(WallpaperConfig(layout = LayoutId.L2, name = "Ayush"))
        assertTrue(whitePixels(named) > whitePixels(none))
    }

    @Test
    fun zeroSizeCanvas_doesNotCrash() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        WallpaperRenderer.render(Canvas(bitmap), 0, 0, WallpaperConfig(), now)
    }
}
