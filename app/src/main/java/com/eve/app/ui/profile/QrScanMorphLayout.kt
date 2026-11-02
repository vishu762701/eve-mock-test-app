
package com.eve.app.ui.profile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.PowerManager
import com.eve.app.telegram.qr.QrCameraBackdrop
import com.eve.app.telegram.qr.QrScannerGlass
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.view.PreviewView
import com.eve.app.designsystem.DesignMotion
import com.eve.app.telegram.qr.QrRender
import com.eve.app.telegram.qr.QrScanMorph
import kotlin.math.abs

/** ADAPTED Telegram QrScanOverlay button-to-screen clipping host; CameraX remains EVE-owned. */
internal class QrScanMorphLayout(context: Context) : FrameLayout(context) {
    var onScannerLayout: (() -> Unit)? = null
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        onScannerLayout?.invoke()
    }
    companion object { const val EXTRA_ORIGIN = "eve_qr_scanner_button_screen_bounds" }
    private val source = QrScanMorph()
    private val fromScreen = RectF()
    private val fromLocal = RectF()
    private val clip = Path()
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val location = IntArray(2)
    private var progress = 0f
    private var closing = false
    private var animator: ValueAnimator? = null
    private var snapshot: ImageView? = null
    private val cameraBackdrop = QrCameraBackdrop()
    private var previewStreaming = false

    // Telegram LiteMode is account/app infrastructure. EVE uses Android's
    // power-save setting and the same API31 effect boundary instead.
    private fun cameraBlurAllowed(): Boolean = QrRender.hasEffects() &&
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode != true

    fun setPreviewStreaming(streaming: Boolean) {
        previewStreaming = streaming
        if (!streaming) cameraBackdrop.discardLive()
        invalidate()
    }

    fun scannerGlass(radius: Float) = QrScannerGlass(radius,
        { canvas, owner -> cameraBackdrop.draw(canvas, owner, this, cameraBlurAllowed()) },
        { cameraBlurAllowed() })

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (child !is PreviewView) return super.drawChild(canvas, child, drawingTime)
        val recording = cameraBackdrop.beginFrame(canvas, width, height,
            resources.displayMetrics.density,
            previewStreaming && !closing && progress >= 1f && cameraBlurAllowed())
        return try {
            super.drawChild(recording, child, drawingTime)
        } finally {
            cameraBackdrop.endFrame(canvas, width, height)
        }
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setOrigin(points: FloatArray?) {
        if (points?.size == 4 && points.all { it.isFinite() } &&
            points[2] > points[0] && points[3] > points[1]) {
            fromScreen.set(points[0], points[1], points[2], points[3])
        } else fromScreen.setEmpty()
        invalidate()
    }

    fun setAlreadyOpened() {
        stopAnimator()
        progress = 1f
        closing = false
        invalidate()
    }

    fun animateSourceOpen(onEnd: () -> Unit) {
        closing = false
        animateTo(1f, QrScanMorph.OPEN_MS, onEnd)
    }
    fun animateSourceClose(onEnd: () -> Unit) {
        closing = true
        animateTo(0f, QrScanMorph.CLOSE_MS, onEnd)
    }

    private fun animateTo(target: Float, millis: Long, onEnd: () -> Unit) {
        stopAnimator()
        if (!isAttachedToWindow || width <= 0 || height <= 0 ||
            !DesignMotion.enabled(context)) {
            progress = target
            invalidate()
            onEnd()
            return
        }
        val start = progress
        val a = ValueAnimator.ofFloat(start, target)
        animator = a
        a.duration = (millis * abs(target - start)).toLong().coerceAtLeast(1)
        a.interpolator = LinearInterpolator()
        a.addUpdateListener { progress = it.animatedValue as Float; invalidate() }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animator === animation) {
                    animator = null
                    progress = target
                    invalidate()
                    onEnd()
                }
            }
        })
        a.start()
    }

    /** The camera's last preview frame stays in RAM only until this view closes. */
    fun captureCloseFrame(preview: PreviewView) {
        if (snapshot != null) return
        val original = runCatching { preview.bitmap }.getOrNull() ?: return
        if (original.width <= 0 || original.height <= 0) {
            original.recycle()
            return
        }
        val factor = minOf(.5f, 1000f / maxOf(original.width, original.height))
        val w = (original.width * factor).toInt().coerceAtLeast(1)
        val h = (original.height * factor).toInt().coerceAtLeast(1)
        val scaled = try { Bitmap.createScaledBitmap(original, w, h, true) }
            catch (_: OutOfMemoryError) { null }
        if (scaled !== original) original.recycle()
        if (scaled == null) return
        if (cameraBlurAllowed()) {
            // QrScanOverlay:540-546: half-size closing frame -> 1/8 copy,
            // radius-6 stack blur. This bounded copy is RAM-only and owned
            // by the Java compositor until detach/dispose.
            var blur: Bitmap? = null
            try {
                blur = Bitmap.createScaledBitmap(scaled,
                    (scaled.width / 8).coerceAtLeast(1),
                    (scaled.height / 8).coerceAtLeast(1), true)
                if (blur === scaled) blur = scaled.copy(Bitmap.Config.ARGB_8888, true)
                blur?.let { ProfileStackBlur.blurBitmap(it, 6) }
                cameraBackdrop.setSnapshotBlur(blur)
                blur = null // ownership transferred
            } catch (_: RuntimeException) {
                // Keep source flat glass when a closing blur cannot be built.
            } catch (_: OutOfMemoryError) {
                // A failed optional blur must not prevent Back/closing.
            } finally {
                blur?.recycle()
            }
        }
        val frame = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(scaled)
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        snapshot = frame
        addView(frame, 1, LayoutParams(-1, -1))
    }

    private fun stopAnimator() {
        val current = animator
        animator = null
        current?.cancel()
    }

    private fun calculateFrom() {
        getLocationOnScreen(location)
        if (fromScreen.isEmpty) {
            val side = 48f * resources.displayMetrics.density
            val y = height - side
            fromLocal.set(width / 2f - side / 2f, y - side / 2f,
                width / 2f + side / 2f, y + side / 2f)
        } else {
            fromLocal.set(fromScreen.left - location[0], fromScreen.top - location[1],
                fromScreen.right - location[0], fromScreen.bottom - location[1])
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        if (closing || progress < 1f) true else super.onInterceptTouchEvent(event)

    override fun onTouchEvent(event: MotionEvent): Boolean =
        if (closing || progress < 1f) true else super.onTouchEvent(event)

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        calculateFrom()
        source.sample(fromLocal.left, fromLocal.top, fromLocal.right,
            fromLocal.bottom, width, height, progress, closing,
            QrRender.screenCornerRadius(this))
        val save = canvas.save()
        if (progress < 1f) {
            clip.rewind()
            clip.addRoundRect(source.left, source.top, source.right, source.bottom,
                source.radius, source.radius, Path.Direction.CW)
            canvas.clipPath(clip)
        }
        dark.alpha = (255f * source.blackAlpha).toInt().coerceIn(0, 255)
        canvas.drawRect(source.left, source.top, source.right, source.bottom, dark)
        if (progress < 1f) {
            canvas.translate((source.left + source.right) / 2f,
                (source.top + source.bottom) / 2f)
            canvas.scale(source.scale, source.scale)
            canvas.translate(-width / 2f, -height / 2f)
        }
        val layer = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(),
            (255f * source.contentAlpha).toInt().coerceIn(0, 255))
        super.dispatchDraw(canvas)
        canvas.restoreToCount(layer)
        canvas.restoreToCount(save)
    }

    fun dispose() {
        stopAnimator()
        previewStreaming = false
        cameraBackdrop.dispose()
        val image = snapshot
        snapshot = null
        if (image != null) {
            val bitmap = (image.drawable as? BitmapDrawable)?.bitmap
            removeView(image)
            image.setImageDrawable(null)
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    override fun onDetachedFromWindow() {
        dispose()
        super.onDetachedFromWindow()
    }
}
