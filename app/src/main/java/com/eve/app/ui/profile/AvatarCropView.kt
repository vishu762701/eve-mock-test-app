/*
 * Derived from Telegram Android PhotoCropActivity.PhotoCropView
 * (`PhotoCropActivity.java:40-360`), Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/**
 * ADAPTED source crop surface. PhotoCropView's fixed square avatar selection,
 * drag/resize states, scrim, border, corner handles and rule-of-thirds grid are
 * retained. EVE hosts it in an Activity and renders the final square straight
 * to an 800px JPEG instead of returning the raw bitmap to ImageUpdater.
 */
class AvatarCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private enum class Drag { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private var bitmap: Bitmap? = null
    private var imageBox = AvatarCropGeometry.Box(0f, 0f, 0f, 0f)
    private var cropBox = AvatarCropGeometry.Box(0f, 0f, 0f, 0f)
    private var drag = Drag.NONE
    private var lastX = 0f
    private var lastY = 0f

    private val density = resources.displayMetrics.density
    private val inset = 14f * density
    private val cornerTouch = 28f * density
    private val handleLength = 20f * density
    private val minimumSide = 48f * density

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint().apply { color = 0xC8000000.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x3FFAFAFA
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }

    init {
        setBackgroundColor(0xFF333333.toInt())
        isClickable = true
        isFocusable = true
    }

    fun setBitmap(value: Bitmap) {
        bitmap = value
        if (width > 0 && height > 0) {
            layoutBoxes(reset = true)
        }
        invalidate()
    }

    fun hasBitmap(): Boolean = bitmap != null && cropBox.isValid

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutBoxes(reset = true)
    }

    private fun layoutBoxes(reset: Boolean) {
        val source = bitmap ?: return
        imageBox = AvatarCropGeometry.fitImage(source.width, source.height, width, height, inset)
        cropBox = if (reset || !cropBox.isValid) {
            AvatarCropGeometry.initialSquare(imageBox)
        } else {
            cropBox
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val source = bitmap ?: return
        if (!imageBox.isValid) return

        canvas.drawBitmap(source, null, android.graphics.RectF(
            imageBox.left, imageBox.top, imageBox.right, imageBox.bottom
        ), bitmapPaint)

        // Source scrim covers the fitted bitmap around the crop window
        // (PhotoCropActivity.java:315-322).
        canvas.drawRect(imageBox.left, imageBox.top, imageBox.right, cropBox.top, scrimPaint)
        canvas.drawRect(imageBox.left, cropBox.top, cropBox.left, cropBox.bottom, scrimPaint)
        canvas.drawRect(cropBox.right, cropBox.top, imageBox.right, cropBox.bottom, scrimPaint)
        canvas.drawRect(imageBox.left, cropBox.bottom, imageBox.right, imageBox.bottom, scrimPaint)

        canvas.drawRect(cropBox.left, cropBox.top, cropBox.right, cropBox.bottom, borderPaint)

        val edge = 1f * density
        drawCorner(canvas, cropBox.left + edge, cropBox.top + edge, 1f, 1f)
        drawCorner(canvas, cropBox.right - edge, cropBox.top + edge, -1f, 1f)
        drawCorner(canvas, cropBox.left + edge, cropBox.bottom - edge, 1f, -1f)
        drawCorner(canvas, cropBox.right - edge, cropBox.bottom - edge, -1f, -1f)

        for (index in 1..2) {
            val x = cropBox.left + cropBox.width / 3f * index
            canvas.drawRect(x, cropBox.top + edge, x + edge, cropBox.bottom - edge, gridPaint)
            val y = cropBox.top + cropBox.height / 3f * index
            canvas.drawRect(cropBox.left + edge, y, cropBox.right - edge, y + edge, gridPaint)
        }
    }

    private fun drawCorner(canvas: Canvas, x: Float, y: Float, dx: Float, dy: Float) {
        val thickness = 3f * density
        canvas.drawRect(
            if (dx > 0) x else x - handleLength,
            if (dy > 0) y else y - thickness,
            if (dx > 0) x + handleLength else x,
            if (dy > 0) y + thickness else y,
            handlePaint
        )
        canvas.drawRect(
            if (dx > 0) x else x - thickness,
            if (dy > 0) y else y - handleLength,
            if (dx > 0) x + thickness else x,
            if (dy > 0) y + handleLength else y,
            handlePaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!cropBox.isValid) return false
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drag = hitTest(x, y)
                lastX = x
                lastY = y
                if (drag != Drag.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (drag != Drag.NONE) {
                    val dx = x - lastX
                    val dy = y - lastY
                    cropBox = if (drag == Drag.MOVE) {
                        AvatarCropGeometry.move(cropBox, imageBox, dx, dy)
                    } else {
                        AvatarCropGeometry.resize(cropBox, imageBox, drag.corner(), dx, dy, minimumSide)
                    }
                    lastX = x
                    lastY = y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (drag != Drag.NONE) {
                    drag = Drag.NONE
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                    return true
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun hitTest(x: Float, y: Float): Drag {
        if (abs(x - cropBox.left) <= cornerTouch && abs(y - cropBox.top) <= cornerTouch) {
            return Drag.TOP_LEFT
        }
        if (abs(x - cropBox.right) <= cornerTouch && abs(y - cropBox.top) <= cornerTouch) {
            return Drag.TOP_RIGHT
        }
        if (abs(x - cropBox.left) <= cornerTouch && abs(y - cropBox.bottom) <= cornerTouch) {
            return Drag.BOTTOM_LEFT
        }
        if (abs(x - cropBox.right) <= cornerTouch && abs(y - cropBox.bottom) <= cornerTouch) {
            return Drag.BOTTOM_RIGHT
        }
        if (x in cropBox.left..cropBox.right && y in cropBox.top..cropBox.bottom) {
            return Drag.MOVE
        }
        return Drag.NONE
    }

    private fun Drag.corner(): AvatarCropGeometry.Corner = when (this) {
        Drag.TOP_LEFT -> AvatarCropGeometry.Corner.TOP_LEFT
        Drag.TOP_RIGHT -> AvatarCropGeometry.Corner.TOP_RIGHT
        Drag.BOTTOM_LEFT -> AvatarCropGeometry.Corner.BOTTOM_LEFT
        Drag.BOTTOM_RIGHT -> AvatarCropGeometry.Corner.BOTTOM_RIGHT
        Drag.MOVE, Drag.NONE -> AvatarCropGeometry.Corner.TOP_LEFT
    }

    /**
     * Source getBitmap plus ImageUpdater's 800x800/q80 avatar export
     * (`ImageUpdater.java:845-855`). The returned bitmap is owned by the caller.
     */
    fun cropBitmap(): Bitmap? {
        val source = bitmap ?: return null
        val mapped = AvatarCropGeometry.mapToImage(
            cropBox, imageBox, source.width, source.height
        )
        if (mapped.width <= 0 || mapped.height <= 0) return null
        val side = min(mapped.width, mapped.height)
        val cropped = try {
            Bitmap.createBitmap(source, mapped.x, mapped.y, side, side)
        } catch (_: Exception) {
            return null
        }
        if (side <= 800) return cropped
        val scaled = Bitmap.createScaledBitmap(cropped, 800, 800, true)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

    /** Releases the decoded source on activity destruction. */
    fun release() {
        bitmap = null
        imageBox = AvatarCropGeometry.Box(0f, 0f, 0f, 0f)
        cropBox = AvatarCropGeometry.Box(0f, 0f, 0f, 0f)
        invalidate()
    }

    companion object {
        /** Keeps very large camera images from being scaled up by the crop surface. */
        const val DECODE_MAX_DIMENSION = 2048
    }
}
