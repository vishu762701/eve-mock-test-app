package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.eve.app.R

/**
 * UIArc Switch Component.
 * Genuine native translation of official UIArc Switch specification:
 * - Registry: https://uiarc.dev/r/switch.json
 * - Interactive tactile toggle with critically damped glide spring,
 *   thumb stretch along travel, snappy width release, and standard track color crossfade.
 */
class UiArcSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialSwitchStyle
) : DesignSwitch(context, attrs, defStyleAttr) {

    // Geometry tokens matching exact design specifications
    private val switchW = resources.getDimensionPixelSize(R.dimen.design_switch_width)
    private val switchH = resources.getDimensionPixelSize(R.dimen.design_switch_height)
    private val thumbDiameter = resources.getDimension(R.dimen.design_switch_thumb)
    private val touchMin = resources.getDimensionPixelSize(R.dimen.design_touch_min)

    // Travel distance between off and on positions
    private val travel = (switchW - switchH).toFloat()

    // Thumb stretch along travel when pressed (+5dp in density pixels)
    private val stretchPx = 5f * resources.displayMetrics.density

    // Motion states
    private var thumbPosition = if (isChecked) 1.0f else 0.0f
    private var stretchFraction = 0.0f
    private var trackColorFraction = if (isChecked) 1.0f else 0.0f

    private var glideAnimator: ValueAnimator? = null
    private var stretchAnimator: ValueAnimator? = null
    private var trackAnimator: ValueAnimator? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        switchMinWidth = switchW
        minimumWidth = switchW
        minimumHeight = touchMin
        switchPadding = resources.getDimensionPixelSize(R.dimen.design_chip_gap)
        showText = false
        splitTrack = false
        gravity = (gravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or Gravity.CENTER_VERTICAL
        background = null
        setPadding(0, 0, 0, 0)

        // Null out default drawables so custom onDraw renders the complete UIArc fluid motion
        thumbDrawable = EmptyDrawable(travel.toInt(), thumbDiameter.toInt())
        trackDrawable = EmptyDrawable(switchW, switchH)
    }

    override fun setChecked(checked: Boolean) {
        val changed = isChecked != checked
        super.setChecked(checked)
        if (!changed) return

        if (!UiArcMotion.isMotionEnabled(context)) {
            thumbPosition = if (checked) 1.0f else 0.0f
            trackColorFraction = if (checked) 1.0f else 0.0f
            stretchFraction = 0.0f
            invalidate()
            return
        }

        animateGlide(target = if (checked) 1.0f else 0.0f)
        animateTrackColor(target = if (checked) 1.0f else 0.0f)
    }

    private fun animateGlide(target: Float) {
        glideAnimator?.cancel()
        val start = thumbPosition
        glideAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = UiArcMotion.Spring.GLIDE.settlingDurationMs
            interpolator = UiArcMotion.Spring.GLIDE
            addUpdateListener { va ->
                thumbPosition = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateTrackColor(target: Float) {
        trackAnimator?.cancel()
        val start = trackColorFraction
        trackAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = UiArcMotion.Duration.FAST
            interpolator = UiArcMotion.Ease.STANDARD
            addUpdateListener { va ->
                trackColorFraction = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateStretch(target: Float) {
        stretchAnimator?.cancel()
        val start = stretchFraction
        stretchAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = if (target > 0f) UiArcMotion.Duration.FAST else UiArcMotion.Spring.SNAPPY.settlingDurationMs
            interpolator = if (target > 0f) UiArcMotion.Ease.ENTER else UiArcMotion.Spring.SNAPPY
            addUpdateListener { va ->
                stretchFraction = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (UiArcMotion.isMotionEnabled(context)) {
                    animateStretch(1.0f)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (UiArcMotion.isMotionEnabled(context)) {
                    animateStretch(0.0f)
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw track
        val inactiveTrack = ContextCompat.getColor(context, R.color.track)
        val activeTrack = ContextCompat.getColor(context, R.color.system_green)
        val currentTrackColor = ColorUtils.blendARGB(inactiveTrack, activeTrack, trackColorFraction)

        trackPaint.color = currentTrackColor
        val trackTop = (height - switchH) / 2f
        val trackBottom = trackTop + switchH
        val trackLeft = (width - switchW) / 2f
        val trackRight = trackLeft + switchW
        val cornerRadius = switchH / 2f

        canvas.drawRoundRect(trackLeft, trackTop, trackRight, trackBottom, cornerRadius, cornerRadius, trackPaint)

        // Draw thumb with dynamic stretch along travel
        thumbPaint.color = ContextCompat.getColor(context, R.color.solid_white)

        val thumbRadius = thumbDiameter / 2f
        val thumbPadding = (switchH - thumbDiameter) / 2f
        val currentTravel = thumbPosition * travel
        val extraStretch = stretchFraction * stretchPx

        val baseCenterY = trackTop + thumbPadding + thumbRadius

        // When moving/stretched, thumb forms an oval stretched along the horizontal axis
        val thumbLeft: Float
        val thumbRight: Float
        if (isChecked) {
            thumbRight = trackLeft + thumbPadding + currentTravel + thumbDiameter
            thumbLeft = thumbRight - thumbDiameter - extraStretch
        } else {
            thumbLeft = trackLeft + thumbPadding + currentTravel
            thumbRight = thumbLeft + thumbDiameter + extraStretch
        }

        canvas.drawRoundRect(
            thumbLeft,
            baseCenterY - thumbRadius,
            thumbRight,
            baseCenterY + thumbRadius,
            thumbRadius,
            thumbRadius,
            thumbPaint
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        glideAnimator?.cancel()
        stretchAnimator?.cancel()
        trackAnimator?.cancel()
    }

    private class EmptyDrawable(private val w: Int, private val h: Int) : Drawable() {
        override fun getIntrinsicWidth(): Int = w
        override fun getIntrinsicHeight(): Int = h
        override fun draw(canvas: Canvas) {}
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated by Android")
        override fun getOpacity(): Int = PixelFormat.TRANSPARENT
    }
}
