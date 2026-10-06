package com.eve.app.ui.common

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.EveMotionHelper
import com.eve.app.util.ThemeSwitchAnimator

/**
 * Apple iOS style custom animated Radio Button for Test Options (Section 2f / 3c).
 * - 10dp rounded corners
 * - Selected state uses Apple accent color + filled checkmark indicator
 * - 0.965 scale tactile press feedback with standard Apple motion curve
 */
class TelegramRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.radioButtonStyle
) : AppCompatRadioButton(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val indicatorRadius = 10f * density
    private val strokeWidthPx = 1.6f * density

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val testPillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val testPillBounds = RectF()

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.8f * density
        color = Color.WHITE
    }

    private val checkPath = Path()
    private val argbEvaluator = ArgbEvaluator()
    private var checkProgress = if (isChecked) 1f else 0f
    private var checkAnimator: ValueAnimator? = null
    private var useTestThinMaterialStyle = false

    init {
        // Clear default Android radio graphic
        buttonDrawable = null

        // 10dp rounded cards per Section 2b / 3c
        val cornerPx = 10f * density
        val tokenColor = ContextCompat.getColor(context, R.color.eve_text)
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        val rippleColor = androidx.core.graphics.ColorUtils.setAlphaComponent(
            tokenColor,
            if (isDark) 38 else 31
        )

        val checkedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(ContextCompat.getColor(context, R.color.eve_option_selected_bg))
            setStroke((1.5f * density).toInt(), ContextCompat.getColor(context, R.color.eve_option_selected_stroke))
        }

        val uncheckedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(ContextCompat.getColor(context, R.color.eve_card_bg))
            setStroke((0.5f * density).toInt().coerceAtLeast(1), ContextCompat.getColor(context, R.color.eve_separator))
        }

        val contentStateList = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), checkedBg)
            addState(intArrayOf(), uncheckedBg)
        }

        val maskColor = ContextCompat.getColor(context, R.color.eve_card_bg)
        val maskDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(maskColor)
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
        val wasChecked = isChecked
        super.setChecked(checked)
        if (wasChecked != checked) {
            animateCheckProgress(if (checked) 1f else 0f)
        }
    }

    /** Enables the Test-only capsule treatment while leaving Admin answer editors unchanged. */
    fun enableTestThinMaterialStyle() {
        useTestThinMaterialStyle = true
        background = ColorDrawable(Color.TRANSPARENT)
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        val rippleColor = androidx.core.graphics.ColorUtils.setAlphaComponent(
            ContextCompat.getColor(context, R.color.eve_text),
            if (isDark) 36 else 28
        )
        val rippleMask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * density
            setColor(Color.WHITE)
        }
        foreground = RippleDrawable(ColorStateList.valueOf(rippleColor), null, rippleMask)
        invalidate()
    }

    private fun animateCheckProgress(target: Float) {
        val animScale = try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Exception) {
            1f
        }
        if (animScale == 0f) {
            checkAnimator?.cancel()
            checkProgress = target
            invalidate()
            return
        }

        checkAnimator?.cancel()
        checkAnimator = ValueAnimator.ofFloat(checkProgress, target).apply {
            duration = 200L
            interpolator = EveMotionHelper.standardInterpolator
            addUpdateListener { va ->
                checkProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(0.965f).scaleY(0.965f).alpha(0.88f)
                    .setDuration(90).setInterpolator(EveMotionHelper.standardInterpolator).start()
                try {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                } catch (_: Throwable) {}
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f)
                    .setDuration(180).setInterpolator(EveMotionHelper.standardInterpolator).start()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        val unselectedColor = ContextCompat.getColor(context, R.color.eve_separator)
        val selectedColor = ContextCompat.getColor(
            context,
            if (useTestThinMaterialStyle) R.color.eve_system_green else R.color.eve_accent
        )

        if (useTestThinMaterialStyle && width > 0 && height > 0) {
            testPillStrokePaint.color = ContextCompat.getColor(
                context,
                if (isChecked) R.color.eve_test_option_selected_stroke else R.color.eve_test_option_stroke
            )
            val inset = testPillStrokePaint.strokeWidth / 2f
            testPillBounds.set(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(testPillBounds, height / 2f, height / 2f, testPillStrokePaint)
        }

        val currentRingColor = argbEvaluator.evaluate(checkProgress, unselectedColor, selectedColor) as Int
        ringPaint.color = currentRingColor
        dotPaint.color = selectedColor

        val cx = 16f * density
        val cy = height / 2f

        // Draw outer ring
        canvas.drawCircle(cx, cy, indicatorRadius, ringPaint)

        // Draw filled checkmark indicator on selection
        if (checkProgress > 0f) {
            canvas.drawCircle(cx, cy, indicatorRadius * checkProgress, dotPaint)

            checkPath.reset()
            checkPath.moveTo(cx - 4.5f * density, cy)
            checkPath.lineTo(cx - 1.5f * density, cy + 3f * density)
            checkPath.lineTo(cx + 4.5f * density, cy - 3f * density)
            canvas.drawPath(checkPath, checkPaint)
        }

        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        checkAnimator?.cancel()
    }
}
