package com.eve.app.ui.login

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.ThemeManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * Task D: Ambient Background Motion (Option 1).
 * Renders extremely subtle, slow-moving blurred color spots derived from the app's palette.
 * Lightweight, hardware-accelerated Canvas drawing with zero battery drain when paused or detached.
 * Touches are never intercepted.
 */
class AmbientBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintBlob1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBlob2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress: Float = 0f
    private var animator: ValueAnimator? = null
    private var isPlaying = false

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun areAnimationsEnabled(): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale > 0f
        } catch (_: Throwable) {
            true
        }
    }

    fun startAmbientMotion() {
        if (!areAnimationsEnabled()) {
            stopAmbientMotion()
            invalidate()
            return
        }
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 18000L // 18 seconds slow gentle loop
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener { va ->
                    progress = va.animatedValue as Float
                    invalidate()
                }
            }
        }
        if (animator?.isStarted != true) {
            animator?.start()
            isPlaying = true
        }
    }

    fun pauseAmbientMotion() {
        if (animator?.isRunning == true) {
            animator?.pause()
            isPlaying = false
        }
    }

    fun resumeAmbientMotion() {
        if (areAnimationsEnabled() && animator?.isPaused == true) {
            animator?.resume()
            isPlaying = true
        } else if (areAnimationsEnabled() && animator?.isRunning != true) {
            startAmbientMotion()
        }
    }

    fun stopAmbientMotion() {
        animator?.cancel()
        animator = null
        isPlaying = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) {
            startAmbientMotion()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAmbientMotion()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            resumeAmbientMotion()
        } else {
            pauseAmbientMotion()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val isDark = ThemeManager.isDarkMode(context)
        val angle = progress * 2.0 * Math.PI

        // Primary blob: drifting around upper center
        val blob1Radius = (w * 0.55f).coerceAtLeast(180f)
        val cx1 = w * 0.35f + (cos(angle) * (w * 0.12f)).toFloat()
        val cy1 = h * 0.28f + (sin(angle) * (h * 0.08f)).toFloat()

        // Secondary blob: drifting around lower center
        val blob2Radius = (w * 0.65f).coerceAtLeast(220f)
        val cx2 = w * 0.68f + (sin(angle) * (w * 0.14f)).toFloat()
        val cy2 = h * 0.72f + (cos(angle) * (h * 0.10f)).toFloat()

        val color1 = if (isDark) {
            Color.argb(16, 80, 80, 80) // 6% dark glow
        } else {
            Color.argb(12, 17, 17, 17) // 4.5% subtle contrast tint
        }
        val color2 = if (isDark) {
            Color.argb(14, 197, 155, 39) // ~5.5% warm gold subtle aura
        } else {
            Color.argb(11, 197, 155, 39) // ~4% subtle warm aura
        }

        paintBlob1.shader = RadialGradient(
            cx1, cy1, blob1Radius,
            intArrayOf(color1, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx1, cy1, blob1Radius, paintBlob1)

        paintBlob2.shader = RadialGradient(
            cx2, cy2, blob2Radius,
            intArrayOf(color2, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx2, cy2, blob2Radius, paintBlob2)
    }
}
