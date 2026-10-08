package com.timewall.app.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.timewall.app.domain.LayoutId
import com.timewall.app.domain.TimeFormatter
import com.timewall.app.domain.WallpaperConfig
import java.time.LocalDateTime

/**
 * The single drawing engine. Used by:
 *  - the in-app preview (Compose drawIntoCanvas),
 *  - the live wallpaper (WallpaperService engine),
 *  - the static lock-screen export (Bitmap).
 *
 * Positions are measured on a 1080x2400 reference canvas (the mockup size) and scaled to the
 * real screen. Text is sized by its measured cap height, not by a font-size guess, so the
 * same layout looks right with any system font.
 */
object WallpaperRenderer {

    private const val REF_W = 1080f
    private const val REF_H = 2400f

    fun render(canvas: Canvas, width: Int, height: Int, config: WallpaperConfig, now: LocalDateTime) {
        if (width <= 0 || height <= 0) return

        canvas.drawColor(Color.BLACK)

        val s = Scale(width, height)
        val parts = TimeFormatter.format(now, config.use24h)
        val paint = newPaint()
        val showDate = config.isDateVisible

        when (config.layout) {
            LayoutId.L1 -> {
                if (showDate) {
                    drawText(canvas, paint, s, parts.date, cap = 40f, baseline = 835f,
                        align = Align.LEFT, x = 130f, tracking = 4f)
                }
                drawText(canvas, paint, s, parts.hour, cap = 484f, baseline = 1361f,
                    align = Align.LEFT, x = 160f, maxWidth = 900f)
                drawText(canvas, paint, s, parts.minute, cap = 484f, baseline = 1941f,
                    align = Align.LEFT, x = 160f, maxWidth = 900f)
            }

            LayoutId.L2 -> {
                drawText(canvas, paint, s, parts.hour, cap = 424f, baseline = 1251f,
                    align = Align.CENTER, x = 540f, maxWidth = 900f)
                fillRect(canvas, paint, s, 150f, 1270f, 930f, 1280f)
                drawText(canvas, paint, s, parts.minute, cap = 424f, baseline = 1731f,
                    align = Align.CENTER, x = 540f, maxWidth = 900f)
            }

            LayoutId.L3 -> {
                if (showDate) {
                    drawText(canvas, paint, s, parts.date, cap = 40f, baseline = 320f,
                        align = Align.LEFT, x = 120f, tracking = 3f)
                }
                drawText(canvas, paint, s, parts.hour, cap = 621f, baseline = 1209f,
                    align = Align.RIGHT, x = 930f, scaleX = 0.62f, maxWidth = 760f)
                drawText(canvas, paint, s, parts.minute, cap = 621f, baseline = 1929f,
                    align = Align.RIGHT, x = 930f, scaleX = 0.62f, maxWidth = 760f)
            }

            LayoutId.L4 -> {
                if (showDate) {
                    drawText(canvas, paint, s, parts.date, cap = 40f, baseline = 1017f,
                        align = Align.CENTER, x = 540f, tracking = 4f)
                }
                drawText(canvas, paint, s, "${parts.hour}:${parts.minute}", cap = 250f, baseline = 1324f,
                    align = Align.CENTER, x = 540f, maxWidth = 960f)
            }

            LayoutId.L5 -> {
                drawText(canvas, paint, s, parts.hour, cap = 212f, baseline = 1305f,
                    align = Align.RIGHT, x = 470f, scaleX = 1.05f)
                fillCircle(canvas, paint, s, cx = 540f, cy = 1200f, radius = 30f)
                drawText(canvas, paint, s, parts.minute, cap = 212f, baseline = 1305f,
                    align = Align.LEFT, x = 610f, scaleX = 1.05f)
                fillRect(canvas, paint, s, 400f, 1720f, 680f, 1728f)
            }
        }

        val name = config.displayName
        if (name.isNotEmpty()) {
            drawText(canvas, paint, s, name, cap = 56f, baseline = 2150f,
                align = Align.CENTER, x = 540f, tracking = 4f, maxWidth = 920f)
        }
    }

    // ---- helpers -----------------------------------------------------------------------

    private enum class Align { LEFT, CENTER, RIGHT }

    /** Maps reference (1080x2400) coordinates to the real canvas size. */
    private class Scale(width: Int, height: Int) {
        private val sx = width / REF_W
        private val sy = height / REF_H
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy
    }

    private fun newPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        style = Paint.Style.FILL
    }

    /**
     * Draws one line of white bold text.
     *  - cap: height of the digits/capitals in reference px (sets font size).
     *  - baseline: y of the text baseline in reference px.
     *  - x: anchor x in reference px (left edge, center, or right edge depending on align).
     *  - scaleX: horizontal squeeze (<1) or stretch (>1), for condensed/extended looks.
     *  - maxWidth: if the line is wider than this (reference px), it is squeezed to fit.
     */
    private fun drawText(
        canvas: Canvas,
        paint: Paint,
        s: Scale,
        text: String,
        cap: Float,
        baseline: Float,
        align: Align,
        x: Float,
        scaleX: Float = 1f,
        tracking: Float = 0f,
        maxWidth: Float? = null,
    ) {
        if (text.isEmpty()) return

        // 1) Font size from measured cap height (font-independent).
        paint.textScaleX = 1f
        paint.letterSpacing = 0f
        paint.textSize = 100f
        val probe = Rect()
        paint.getTextBounds("0", 0, 1, probe)
        val capAt100 = probe.height().coerceAtLeast(1).toFloat()
        val size = 100f * s.y(cap) / capAt100
        paint.textSize = size

        // 2) Letter spacing is stored as an em fraction.
        val trackingPx = tracking * s.x(1f)
        paint.letterSpacing = if (size > 0f) trackingPx / size else 0f

        // 3) Horizontal scale, then optional fit to max width.
        paint.textScaleX = scaleX
        var width = paint.measureText(text)
        if (maxWidth != null && width > s.x(maxWidth)) {
            paint.textScaleX = scaleX * (s.x(maxWidth) / width)
            width = paint.measureText(text)
        }

        // 4) Anchor.
        val anchorX = s.x(x)
        val left = when (align) {
            Align.LEFT -> anchorX
            Align.CENTER -> anchorX - width / 2f
            Align.RIGHT -> anchorX - width
        }
        canvas.drawText(text, left, s.y(baseline), paint)

        // Reset so later calls start clean.
        paint.textScaleX = 1f
        paint.letterSpacing = 0f
    }

    private fun fillRect(canvas: Canvas, paint: Paint, s: Scale, l: Float, t: Float, r: Float, b: Float) {
        canvas.drawRect(s.x(l), s.y(t), s.x(r), s.y(b), paint)
    }

    private fun fillCircle(canvas: Canvas, paint: Paint, s: Scale, cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(s.x(cx), s.y(cy), s.x(radius), paint)
    }
}
