/*
 * Derived from Telegram Android CheckBox2.java & CheckBoxBase.java.
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBox2.java
 * and CheckBoxBase.java:110-126, 278-295, 396-412, 631-650.
 */

package com.eve.app.ui.common

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import org.telegram.ui.Components.CubicBezierInterpolator
import kotlin.math.sqrt

/**
 * Authentic Telegram circular selection checkmark component.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBox2.java
 * and CheckBoxBase.java:
 * - Circle geometry: 21dp default diameter, rad = dp(size / 2)
 * - Checkmark vertex: (cx - 1.5dp, cy + 4dp) with 4dp left wing and 9dp right wing at 45 degrees
 * - Check stroke: 1.9dp round cap / stroke join (CheckBoxBase.java:121)
 * - Unchecked ring stroke: 1.5dp (CheckBoxBase.java:125)
 * - Motion: 200ms ObjectAnimator with EASE_OUT progress curve (CheckBoxBase.java:278, 292)
 */
class TelegramCheckBox2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), Checkable {

    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(view: TelegramCheckBox2, isChecked: Boolean)
    }

    private val density: Float = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()

    private var componentSize: Float = 21f // default 21dp like Telegram CheckBox2

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        strokeWidth = dp(1.9f)
    }

    private val checkPath = Path()

    private var _progress: Float = 0f
    private var checkAnimator: ObjectAnimator? = null
    private var attachedToWindow = false
    private var isCheckedState = false
    private var isDisabled = false

    var colorUnchecked: Int = ContextCompat.getColor(context, R.color.text_secondary)
    var colorBackground: Int = ContextCompat.getColor(context, R.color.accent)
    var colorCheck: Int = ContextCompat.getColor(context, R.color.on_accent)
    var colorDisabled: Int = ContextCompat.getColor(context, R.color.text_secondary)

    var onCheckedChangeListener: OnCheckedChangeListener? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TelegramCheckBox2)
            val checked = a.getBoolean(R.styleable.TelegramCheckBox2_android_checked, false)
            val sz = a.getDimension(R.styleable.TelegramCheckBox2_size, 0f)
            if (sz > 0f) {
                componentSize = sz / density
            }
            a.recycle()
            setChecked(checked, animated = false)
        }
        updatePaintsScale()
        updatePalette()
    }

    fun setSize(sizeDp: Float) {
        if (componentSize == sizeDp) return
        componentSize = sizeDp
        updatePaintsScale()
        requestLayout()
        invalidate()
    }

    private fun updatePaintsScale() {
        val scale = componentSize / 21f
        backgroundPaint.strokeWidth = dp(1.5f * scale)
        checkPaint.strokeWidth = dp(1.9f * scale)
    }

    private fun motionEnabled(): Boolean =
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f && (android.os.Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled())
        } catch (_: Exception) {
            true
        }

    fun setColors(unchecked: Int, checked: Int, check: Int) {
        colorUnchecked = unchecked
        colorBackground = checked
        colorCheck = check
        invalidate()
    }

    fun setDisabled(disabled: Boolean) {
        if (isDisabled != disabled) {
            isDisabled = disabled
            invalidate()
        }
    }

    @Keep
    fun setProgress(value: Float) {
        if (_progress == value) return
        _progress = value
        invalidate()
    }

    @Keep
    fun getProgress(): Float = _progress

    private fun cancelCheckAnimator() {
        checkAnimator?.cancel()
        checkAnimator = null
    }

    private fun animateToCheckedState(newCheckedState: Boolean) {
        cancelCheckAnimator()
        val target = if (newCheckedState) 1f else 0f
        checkAnimator = ObjectAnimator.ofFloat(this, "progress", target).apply {
            duration = 200L
            interpolator = CubicBezierInterpolator.EASE_OUT
            start()
        }
    }

    override fun isChecked(): Boolean = isCheckedState

    override fun setChecked(checked: Boolean) {
        setChecked(checked, animated = motionEnabled())
    }

    fun setChecked(checked: Boolean, animated: Boolean) {
        if (checked == isCheckedState) return
        isCheckedState = checked
        if (attachedToWindow && animated && motionEnabled()) {
            animateToCheckedState(checked)
        } else {
            cancelCheckAnimator()
            setProgress(if (checked) 1f else 0f)
        }
        onCheckedChangeListener?.onCheckedChanged(this, checked)
    }

    override fun toggle() {
        if (isEnabled && !isDisabled) {
            setChecked(!isCheckedState)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        updatePalette()
    }

    override fun onDetachedFromWindow() {
        cancelCheckAnimator()
        attachedToWindow = false
        super.onDetachedFromWindow()
    }

    fun updatePalette() {
        try {
            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            colorBackground = palette.accent
            colorUnchecked = palette.textSecondary
            colorCheck = palette.onAccent
            invalidate()
        } catch (_: Exception) {
            // Context without theme engine
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = dp(componentSize.toInt())
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (wMode) {
            MeasureSpec.EXACTLY -> wSize
            MeasureSpec.AT_MOST -> minOf(desired, wSize)
            else -> desired
        }
        val height = when (hMode) {
            MeasureSpec.EXACTLY -> hSize
            MeasureSpec.AT_MOST -> minOf(desired, hSize)
            else -> desired
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (visibility != VISIBLE) return

        val cx = width / 2f
        val cy = height / 2f
        val scale = componentSize / 21f
        val rad = (dp(componentSize.toInt()) / 2f).coerceAtLeast(1f)

        val roundProgress = if (_progress >= 0.5f) 1.0f else _progress / 0.5f
        val checkProgress = if (_progress < 0.5f) 0.0f else (_progress - 0.5f) / 0.5f

        // Draw unchecked circle or background fill
        if (_progress <= 0f) {
            backgroundPaint.color = if (isDisabled) colorDisabled else colorUnchecked
            backgroundPaint.style = Paint.Style.STROKE
            canvas.drawCircle(cx, cy, rad - dp(1f * scale), backgroundPaint)
        } else {
            // Animating or checked
            val bg = if (isDisabled) colorDisabled else colorBackground
            fillPaint.color = bg
            if (roundProgress < 1.0f) {
                // Outer ring fading from unchecked to background color
                val rD = ((Color.red(bg) - Color.red(colorUnchecked)) * roundProgress).toInt()
                val gD = ((Color.green(bg) - Color.green(colorUnchecked)) * roundProgress).toInt()
                val bD = ((Color.blue(bg) - Color.blue(colorUnchecked)) * roundProgress).toInt()
                backgroundPaint.color = Color.rgb(
                    Color.red(colorUnchecked) + rD,
                    Color.green(colorUnchecked) + gD,
                    Color.blue(colorUnchecked) + bD
                )
                canvas.drawCircle(cx, cy, rad - dp(1f * scale), backgroundPaint)
                canvas.drawCircle(cx, cy, (rad - dp(0.5f * scale)) * roundProgress, fillPaint)
            } else {
                canvas.drawCircle(cx, cy, rad - dp(0.5f * scale), fillPaint)
            }

            // Draw Telegram checkmark vertex
            if (checkProgress > 0f) {
                checkPaint.color = colorCheck
                val checkSide = dp(9f * scale) * checkProgress
                val smallCheckSide = dp(4f * scale) * checkProgress
                val x = cx - dp(1.5f * scale)
                val y = cy + dp(4f * scale)
                val leftSide = sqrt(smallCheckSide * smallCheckSide / 2.0f)
                val rightSide = sqrt(checkSide * checkSide / 2.0f)

                checkPath.reset()
                checkPath.moveTo(x - leftSide, y - leftSide)
                checkPath.lineTo(x, y)
                checkPath.lineTo(x + rightSide, y - rightSide)
                canvas.drawPath(checkPath, checkPaint)
            }
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.CheckBox"
        info.isCheckable = true
        info.isChecked = isCheckedState
        info.isEnabled = isEnabled && !isDisabled
    }
}
