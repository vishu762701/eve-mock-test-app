/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/OutlineTextContainerView.java:30-220 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/OutlineEditText.java:20-100 @ 3de37c8cfb03
 * Derived from Telegram Android OutlineTextContainerView.java @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import com.eve.app.util.VibrationHelper
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Typealias for Telegram canonical naming.
 */
typealias OutlineTextContainerView = TelegramOutlineEditText

/**
 * Authentic Telegram outlined text input container.
 * Upstream: org.telegram.ui.Components.OutlineTextContainerView.java
 * Draws a rounded outline with floating label animation, focus state, and error handling.
 */
class TelegramOutlineEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dpInt(v: Float): Int = (v * density + 0.5f).toInt()

    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)
    var editText: EditText
        private set
    private val tvError: TextView
    private val inputFrame: FrameLayout

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val outlineRect = RectF()

    private var hintText: String = ""
    private var selectionProgress: Float = 0f
    private var titleProgress: Float = 0f
    private var errorProgress: Float = 0f

    private var selectionAnimator: ValueAnimator? = null
    private var titleAnimator: ValueAnimator? = null

    var endIconMode: Int = 0

    var isErrorEnabled: Boolean
        get() = tvError.visibility == View.VISIBLE
        set(value) {
            if (!value) clearError()
        }

    var error: CharSequence?
        get() = tvError.text
        set(value) = setError(value?.toString())

    var hint: CharSequence?
        get() = hintText
        set(value) = setHint(value?.toString().orEmpty())

    init {
        orientation = VERTICAL
        setWillNotDraw(false)

        textPaint.typeface = FontManager.telegramMediumTypeface(context)
        textPaint.textSize = dp(14f)

        inputFrame = object : FrameLayout(context) {
            init {
                setWillNotDraw(false)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                drawOutlineAndLabel(canvas)
            }
        }
        inputFrame.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpInt(54f))
        inputFrame.setPadding(dpInt(14f), dpInt(6f), dpInt(14f), dpInt(6f))

        val defaultEt = TelegramEditText(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_VERTICAL
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dpInt(6f), 0, 0)
        }
        editText = defaultEt
        inputFrame.addView(defaultEt)
        super.addView(inputFrame, -1, inputFrame.layoutParams)

        tvError = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpInt(16f)
                marginEnd = dpInt(16f)
                topMargin = dpInt(4f)
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = FontManager.telegramRegularTypeface(context)
            setTextColor(context.getColor(R.color.system_red_text))
            visibility = View.GONE
        }
        super.addView(tvError, -1, tvError.layoutParams)

        setupEditText(defaultEt)
        updateColors()
    }

    private fun setupEditText(et: EditText) {
        et.setOnFocusChangeListener { _, hasFocus ->
            animateFocus(hasFocus)
            val hasText = et.text?.isNotEmpty() == true
            animateTitle(hasFocus || hasText)
        }

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                val isFocused = et.hasFocus()
                animateTitle(hasText || isFocused)
                if (hasText && tvError.visibility == View.VISIBLE) {
                    clearError()
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child == null) return
        if (child === inputFrame || child === tvError) {
            super.addView(child, index, params)
            return
        }
        if (child is EditText) {
            inputFrame.removeAllViews()
            editText = child
            child.background = null
            setupEditText(child)
            inputFrame.addView(child, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_VERTICAL
            ))
            return
        }
        super.addView(child, index, params)
    }

    var text: String
        get() = editText.text?.toString().orEmpty()
        set(value) {
            editText.setText(value)
            if (value.isNotEmpty()) {
                titleProgress = 1f
                inputFrame.invalidate()
            }
        }

    fun setHint(hint: String) {
        hintText = hint
        inputFrame.invalidate()
    }

    fun setError(error: String?) {
        if (error.isNullOrBlank()) {
            clearError()
        } else {
            tvError.text = error
            tvError.visibility = View.VISIBLE
            errorProgress = 1f
            updateColors()
            inputFrame.invalidate()
            shakeError()
        }
    }

    fun clearError() {
        tvError.text = ""
        tvError.visibility = View.GONE
        errorProgress = 0f
        updateColors()
        inputFrame.invalidate()
    }

    fun shakeError() {
        VibrationHelper.vibrateAppError(context)
        val animator = ObjectAnimator.ofFloat(inputFrame, "translationX", 0f, dp(8f), -dp(8f), dp(4f), -dp(4f), 0f).apply {
            duration = 350L
            interpolator = CycleInterpolator(1f)
        }
        animator.start()
    }

    private fun animateFocus(focused: Boolean) {
        selectionAnimator?.cancel()
        val target = if (focused) 1f else 0f
        selectionAnimator = ValueAnimator.ofFloat(selectionProgress, target).apply {
            duration = 200L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                selectionProgress = it.animatedValue as Float
                updateColors()
                inputFrame.invalidate()
            }
            start()
        }
    }

    private fun animateTitle(floating: Boolean) {
        titleAnimator?.cancel()
        val target = if (floating) 1f else 0f
        titleAnimator = ValueAnimator.ofFloat(titleProgress, target).apply {
            duration = 180L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                titleProgress = it.animatedValue as Float
                updateColors()
                inputFrame.invalidate()
            }
            start()
        }
    }

    private fun updateColors() {
        val regularStroke = dp(1f)
        val selectedStroke = dp(1.8f)
        outlinePaint.strokeWidth = regularStroke + (selectedStroke - regularStroke) * selectionProgress

        val normalOutline = palette.hairline
        val focusedOutline = palette.accent
        val errorOutline = context.getColor(R.color.system_red)

        val baseColor = ColorUtils.blendARGB(normalOutline, focusedOutline, selectionProgress)
        val finalColor = ColorUtils.blendARGB(baseColor, errorOutline, errorProgress)
        outlinePaint.color = finalColor

        val normalHint = palette.textSecondary
        val focusedHint = palette.accent
        val baseHintColor = ColorUtils.blendARGB(normalHint, focusedHint, selectionProgress)
        textPaint.color = ColorUtils.blendARGB(baseHintColor, errorOutline, errorProgress)
        tvError.setTextColor(context.getColor(R.color.system_red_text))
    }

    private fun drawOutlineAndLabel(canvas: Canvas) {
        val w = inputFrame.width.toFloat()
        val h = inputFrame.height.toFloat()
        val pad = outlinePaint.strokeWidth / 2f
        val radius = dp(10f)

        outlineRect.set(pad, dp(6f) + pad, w - pad, h - pad)

        // Draw rounded outline
        canvas.drawRoundRect(outlineRect, radius, radius, outlinePaint)

        // Draw floating or centered label
        if (hintText.isNotEmpty()) {
            val labelTextSize = dp(12f) + (dp(15f) - dp(12f)) * (1f - titleProgress)
            textPaint.textSize = labelTextSize

            val labelX = dp(14f)
            val topY = dp(10f)
            val centerY = h / 2f + dp(5f)
            val labelY = centerY - (centerY - topY) * titleProgress

            // If floating, draw background patch to notch the border line
            if (titleProgress > 0.3f) {
                val textW = textPaint.measureText(hintText)
                val bgPaint = Paint().apply {
                    color = palette.surface
                    style = Paint.Style.FILL
                }
                canvas.drawRect(labelX - dp(4f), dp(4f), labelX + textW + dp(4f), dp(10f) + dp(4f), bgPaint)
            }

            canvas.drawText(hintText, labelX, labelY, textPaint)
        }
    }
}
