package com.eve.app.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

/**
 * MASTER_TASK_V3: Home screen hero panel wash background for Light theme.
 * Builds ONE small bitmap (100x70) lazily, cached in companion object,
 * using smoothstep interpolation across a 5-column x 4-row pastel color grid.
 * Draws scaled with filter & dither clipped to a 28dp rounded rect plus 1dp hairline (#E9E9EE).
 */
class HomePanelWashDrawable(
    private val cornerRadiusPx: Float,
    private val strokeWidthPx: Float
) : Drawable() {

    companion object {
        const val GRID_COLS = 5
        const val GRID_ROWS = 4

        val GRID_COLORS: Array<IntArray> = arrayOf(
            intArrayOf(0xFFF2F9FE.toInt(), 0xFFF3EDFD.toInt(), 0xFFFCDFFA.toInt(), 0xFFFEE4E2.toInt(), 0xFFFEF0CE.toInt()),
            intArrayOf(0xFFECF9FE.toInt(), 0xFFECEDFD.toInt(), 0xFFF4E0FD.toInt(), 0xFFFEE3ED.toInt(), 0xFFFEE7DC.toInt()),
            intArrayOf(0xFFECF8FE.toInt(), 0xFFE9EFFD.toInt(), 0xFFEEE4FD.toInt(), 0xFFF9E1FA.toInt(), 0xFFFEE2EC.toInt()),
            intArrayOf(0xFFF1F8FE.toInt(), 0xFFEFF6FD.toInt(), 0xFFF0EEFD.toInt(), 0xFFF6E9FD.toInt(), 0xFFFBECF9.toInt())
        )

        const val BITMAP_WIDTH = 100
        const val BITMAP_HEIGHT = 70
        const val STROKE_COLOR = 0xFFE9E9EE.toInt()

        @Volatile
        private var cachedBitmap: Bitmap? = null

        /**
         * Pure interpolation function (testable in JVM tests without Android runtime).
         * Uses smoothstep (3t^2 - 2t^3) interpolation along x and y dimensions.
         */
        fun interpolateGridPixels(
            grid: Array<IntArray>,
            width: Int,
            height: Int
        ): IntArray {
            val pixels = IntArray(width * height)
            val numRows = grid.size
            val numCols = grid[0].size

            for (y in 0 until height) {
                val v = if (height > 1) y.toFloat() / (height - 1) else 0f
                val gy = v * (numRows - 1)
                val row0 = gy.toInt().coerceIn(0, numRows - 2)
                val row1 = (row0 + 1).coerceIn(0, numRows - 1)
                val ty = gy - row0
                val smoothTy = ty * ty * (3f - 2f * ty)

                for (x in 0 until width) {
                    val u = if (width > 1) x.toFloat() / (width - 1) else 0f
                    val gx = u * (numCols - 1)
                    val col0 = gx.toInt().coerceIn(0, numCols - 2)
                    val col1 = (col0 + 1).coerceIn(0, numCols - 1)
                    val tx = gx - col0
                    val smoothTx = tx * tx * (3f - 2f * tx)

                    val c00 = grid[row0][col0]
                    val c10 = grid[row0][col1]
                    val c01 = grid[row1][col0]
                    val c11 = grid[row1][col1]

                    fun interpChannel(shift: Int): Int {
                        val v00 = (c00 shr shift) and 0xFF
                        val v10 = (c10 shr shift) and 0xFF
                        val v01 = (c01 shr shift) and 0xFF
                        val v11 = (c11 shr shift) and 0xFF

                        val top = v00 * (1f - smoothTx) + v10 * smoothTx
                        val bottom = v01 * (1f - smoothTx) + v11 * smoothTx
                        return (top * (1f - smoothTy) + bottom * smoothTy).roundToInt().coerceIn(0, 255)
                    }

                    val r = interpChannel(16)
                    val g = interpChannel(8)
                    val b = interpChannel(0)
                    val a = 0xFF

                    pixels[y * width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            return pixels
        }

        fun getOrCreateBitmap(): Bitmap {
            cachedBitmap?.let { return it }
            synchronized(this) {
                cachedBitmap?.let { return it }
                val pixels = interpolateGridPixels(GRID_COLORS, BITMAP_WIDTH, BITMAP_HEIGHT)
                val bmp = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
                bmp.setPixels(pixels, 0, BITMAP_WIDTH, 0, 0, BITMAP_WIDTH, BITMAP_HEIGHT)
                cachedBitmap = bmp
                return bmp
            }
        }
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = STROKE_COLOR
    }

    private val boundsF = RectF()
    private val strokeBoundsF = RectF()
    private val clipPath = Path()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        boundsF.set(bounds)
        val inset = strokeWidthPx / 2f
        strokeBoundsF.set(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset
        )
        clipPath.reset()
        clipPath.addRoundRect(boundsF, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val bmp = getOrCreateBitmap()

        val saveCount = canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(bmp, null, boundsF, bitmapPaint)
        canvas.restoreToCount(saveCount)

        if (strokeWidthPx > 0f) {
            canvas.drawRoundRect(strokeBoundsF, cornerRadiusPx, cornerRadiusPx, strokePaint)
        }
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(bounds, cornerRadiusPx)
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
        strokePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
