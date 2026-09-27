package com.eve.app.ui.common

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
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
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        val unselectedColor = if (isDark) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
        val selectedColor = if (isDark) Color.WHITE else Color.parseColor("#111111")

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
