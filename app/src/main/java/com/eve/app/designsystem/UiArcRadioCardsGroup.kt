package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R

/**
 * UIArc Radio Cards Component.
 * Genuine native translation of official UIArc Radio Cards specification:
 * - Registry: https://uiarc.dev/r/radio-cards.json
 * - Selection ring: Single sliding indicator ring gliding between cards on GLIDE spring (550ms, visualDuration 0.42s, bounce 0.00)
 * - Initial placement: Fast opacity fade-in (160ms, Ease.STANDARD)
 * - Native RadioGroup keyboard navigation and mutual exclusion preserved
 * - Instant snap with reduced motion
 */
class UiArcRadioCardsGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RadioGroup(context, attrs) {

    data class RadioCardOption(
        val value: String,
        val title: String,
        val description: String? = null,
        val meta: String? = null
    )

    companion object {
        const val GLIDE_DURATION_MS = 550L
        const val FADE_IN_DURATION_MS = 160L
    }

    private val density = resources.displayMetrics.density
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = ContextCompat.getColor(context, R.color.system_blue)
    }

    private val currentRect = RectF()
    private val targetRect = RectF()
    private var ringAlpha = 0f
    private var isRingPlaced = false
    private var ringAnimator: ValueAnimator? = null
    private var alphaAnimator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        setOnCheckedChangeListener { group, checkedId ->
            updateSelectedCard(checkedId, animate = true)
        }
    }

    fun setCardOptions(
        options: List<RadioCardOption>,
        selectedValue: String,
        onSelectionChanged: (String) -> Unit
    ) {
        removeAllViews()
        var selectedId = View.NO_ID

        options.forEachIndexed { index, option ->
            val cardView = LinearLayout(context).apply {
                id = View.generateViewId()
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                val minH = Math.round(52f * density)
                minimumHeight = minH
                val padH = Math.round(14f * density)
                val padV = Math.round(10f * density)
                setPadding(padH, padV, padH, padV)
                background = ContextCompat.getDrawable(context, R.drawable.design_card_surface)
                isClickable = true
                isFocusable = true

                val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = Math.round(8f * density)
                }
                layoutParams = lp

                val tvTitle = TextView(context).apply {
                    text = option.title
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                }
                addView(tvTitle)

                if (!option.description.isNullOrBlank()) {
                    val tvDesc = TextView(context).apply {
                        text = option.description
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        val dLp = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                            topMargin = Math.round(2f * density)
                        }
                        layoutParams = dLp
                    }
                    addView(tvDesc)
                }

                contentDescription = if (option.description.isNullOrBlank()) option.title else "${option.title}, ${option.description}"

                setOnClickListener {
                    check(id)
                    announceForAccessibility("Selected ${option.title}")
                    onSelectionChanged(option.value)
                }
            }

            addView(cardView)

            if (option.value == selectedValue) {
                selectedId = cardView.id
            }
        }

        if (selectedId != View.NO_ID) {
            check(selectedId)
            post { updateSelectedCard(selectedId, animate = false) }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (checkedRadioButtonId != View.NO_ID) {
            updateSelectedCard(checkedRadioButtonId, animate = false)
        }
    }

    private fun updateSelectedCard(checkedId: Int, animate: Boolean) {
        val selectedView = findViewById<View>(checkedId) ?: return
        val targetL = selectedView.left.toFloat()
        val targetT = selectedView.top.toFloat()
        val targetR = selectedView.right.toFloat()
        val targetB = selectedView.bottom.toFloat()

        targetRect.set(targetL, targetT, targetR, targetB)

        if (!isRingPlaced || !animate || !UiArcMotion.isMotionEnabled(context)) {
            currentRect.set(targetRect)
            ringAlpha = 1f
            isRingPlaced = true
            invalidate()
            return
        }

        // Animate selection ring with GLIDE spring
        val fromL = currentRect.left
        val fromT = currentRect.top
        val fromR = currentRect.right
        val fromB = currentRect.bottom

        ringAnimator?.cancel()
        ringAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = GLIDE_DURATION_MS
            interpolator = UiArcMotion.Spring.CARD_GLIDE
            addUpdateListener { va ->
                val fraction = va.animatedValue as Float
                currentRect.left = fromL + (targetL - fromL) * fraction
                currentRect.top = fromT + (targetT - fromT) * fraction
                currentRect.right = fromR + (targetR - fromR) * fraction
                currentRect.bottom = fromB + (targetB - fromB) * fraction
                invalidate()
            }
            start()
        }

        if (ringAlpha < 1f) {
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(ringAlpha, 1f).apply {
                duration = FADE_IN_DURATION_MS
                interpolator = UiArcMotion.Ease.STANDARD
                addUpdateListener { va ->
                    ringAlpha = va.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isRingPlaced && ringAlpha > 0f && !currentRect.isEmpty) {
            ringPaint.alpha = (ringAlpha * 255).toInt()
            val radius = 14f * density
            canvas.drawRoundRect(currentRect, radius, radius, ringPaint)
        }
    }
}
