package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import com.eve.app.telegram.qr.QrScanFound
import kotlin.math.min

/**
 * Adapted EVE Canvas integration of Telegram QrScanOverlay.java:579-599,659-708
 * @ DrKLO/Telegram 3de37c8cfb039452be7398909461da6288babf15.
 * No Telegram account or camera stack is imported.
 */
internal data class QrScannerFrameBounds(
    val left: Float, val top: Float, val right: Float, val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

internal object QrScannerFrameGeometry {
    fun bounds(w: Int, h: Int, density: Float): QrScannerFrameBounds {
        val side = min(w, h).coerceAtLeast(0) * .66f
        val cx = w.coerceAtLeast(0) / 2f
        val cy = h.coerceAtLeast(0) / 2f - 20f * density
        return QrScannerFrameBounds(cx - side / 2f, cy - side / 2f,
            cx + side / 2f, cy + side / 2f)
    }
}

internal class QrScannerFrameOverlay(context: Context) : View(context) {
    private val dp = resources.displayMetrics.density
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000 }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    private val path = Path()
    private val rect = RectF()
    private val found = QrScanFound(dp)
    private var sourceFrame: RectF? = null
    private var compositionScale = 1f

    fun setSourceFrame(frame: RectF, scale: Float) {
        if (sourceFrame == frame && compositionScale == scale) return
        sourceFrame = RectF(frame)
        compositionScale = scale
        invalidate()
    }

    /** Four real ZXing points already mapped to preview-local display coordinates. */
    fun showFound(mappedPoints: FloatArray): Boolean {
        val accepted = found.show(mappedPoints, width.toFloat(), height.toFloat())
        if (accepted) invalidate()
        return accepted
    }

    fun clearFound() { found.clear(); invalidate() }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val needsFrame = found.advance()
        val bounds = QrScannerFrameGeometry.bounds(width, height, dp)
        val measured = sourceFrame
        if (measured != null) rect.set(measured)
        else rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        val radius = 14f * dp * compositionScale
        stroke.strokeWidth = 4f * dp * compositionScale
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.drawPath(path, dim)
        // Source QrScanOverlay.drawScanner fades white arms as foundK rises.
        stroke.alpha = (255f * (1f - found.progress())).toInt().coerceIn(0, 255)
        val arm = 28f * dp * compositionScale
        for (corner in 0 until 4) {
            val right = corner == 1 || corner == 2
            val bottom = corner >= 2
            val save = canvas.save()
            canvas.translate(if (right) rect.right else rect.left,
                if (bottom) rect.bottom else rect.top)
            canvas.scale(if (right) -1f else 1f, if (bottom) -1f else 1f)
            path.reset()
            path.fillType = Path.FillType.WINDING
            path.moveTo(0f, arm)
            path.lineTo(0f, radius)
            path.quadTo(0f, 0f, radius, 0f)
            path.lineTo(arm, 0f)
            canvas.drawPath(path, stroke)
            canvas.restoreToCount(save)
        }
        found.draw(canvas)
        if (needsFrame) postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        found.clear()
        super.onDetachedFromWindow()
    }
}
