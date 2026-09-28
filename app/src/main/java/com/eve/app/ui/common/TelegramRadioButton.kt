package com.eve.app.ui.common

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.R
import com.eve.app.util.ThemeSwitchAnimator

/**
 * Telegram-style custom animated Radio Button (matching CheckBox2.java / RadioButton.java pattern).
 * Smoothly scales and fills the selection dot while animating the outer ring color over ~220ms with an ease-out curve.
 * Fully compatible with RadioGroup and CompoundButton.
 */
class TelegramRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.radioButtonStyle
) : AppCompatRadioButton(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val indicatorRadius = 10f * density
    private val dotRadius = 5.5f * density
    private val strokeWidthPx = 2f * density

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val argbEvaluator = ArgbEvaluator()
    private var checkProgress = if (isChecked) 1f else 0f
    private var checkAnimator: ValueAnimator? = null

    init {
        // Clear default Android radio graphic
        buttonDrawable = null

        // 2b & 2c: Bounded, theme-adaptive 12dp rounded-rect ripple and selected subtle background
        val cornerPx = 12f * density
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        val rippleColor = if (isDark) {
            Color.argb(38, 255, 255, 255) // ~15% white
        } else {
            Color.argb(31, 0, 0, 0) // ~12% dark
        }

        val checkedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(ContextCompat.getColor(context, R.color.eve_surface_variant))
        }

        val uncheckedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(Color.TRANSPARENT)
        }

        val contentStateList = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), checkedBg)
            addState(intArrayOf(), uncheckedBg)
        }

        val maskDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(Color.WHITE)
        }

        background = RippleDrawable(ColorStateList.valueOf(rippleColor), contentStateList, maskDrawable)

        // Ensure sufficient left padding for custom-drawn indicator
        val desiredLeftPad = (34f * density).toInt()
        val currentLeft = paddingLeft
        if (currentLeft < desiredLeftPad) {
            setPadding(desiredLeftPad, paddingTop, paddingRight, paddingBottom)
        }
    }

    override fun setChecked(checked: Boolean) {
        val changed = (checked != isChecked)
        super.setChecked(checked)
        refreshDrawableState()
        if (changed) {
            animateCheckProgress(if (checked) 1f else 0f)
        } else {
            checkProgress = if (checked) 1f else 0f
            invalidate()
        }
    }

    private fun animateCheckProgress(target: Float) {
        if (!isAttachedToWindow || width == 0) {
            checkProgress = target
            invalidate()
            return
        }

        checkAnimator?.cancel()
        checkAnimator = ValueAnimator.ofFloat(checkProgress, target).apply {
            duration = 220L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { va ->
                checkProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val unselectedColor = ContextCompat.getColor(context, R.color.eve_grey)
        val selectedColor = ContextCompat.getColor(context, R.color.eve_text)

        val currentRingColor = argbEvaluator.evaluate(checkProgress, unselectedColor, selectedColor) as Int
        ringPaint.color = currentRingColor
        dotPaint.color = selectedColor

        val cx = 14f * density
        val cy = height / 2f

        // Draw outer ring
        canvas.drawCircle(cx, cy, indicatorRadius, ringPaint)

        // Draw smoothly scaling inner dot
        if (checkProgress > 0f) {
            canvas.drawCircle(cx, cy, dotRadius * checkProgress, dotPaint)
        }

        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        checkAnimator?.cancel()
    }
}
