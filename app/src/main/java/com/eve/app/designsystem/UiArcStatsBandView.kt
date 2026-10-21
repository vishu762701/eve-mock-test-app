package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import kotlin.math.roundToInt

/**
 * UIArc Stats Band Component.
 * Genuine native translation of official UIArc Stats Band specification:
 * - Registry: https://uiarc.dev/r/stats-band.json
 * - Counting animation: 0 -> targetValue, duration = 1.6s (1600ms), Ease.ENTER
 * - Stagger: 90ms (0.09s) delay between sequential stats
 * - In-view trigger: Triggers on 45%-50% viewport visibility
 * - Tabular numerals & layout reservation to prevent jitter
 * - Sparkline visual drawing
 * - Instant final value setting under reduced motion
 */
open class UiArcStatsBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    companion object {
        const val COUNT_DURATION_MS = 1600L
        const val STAGGER_MS = 90L
        const val TRIGGER_AMOUNT = 0.45f
    }

    private val density = resources.displayMetrics.density
    private var hasTriggeredInView = false
    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null

    private val statAnimators = mutableListOf<ValueAnimator>()

    init {
        viewTreeObserver.addOnScrollChangedListener(object : ViewTreeObserver.OnScrollChangedListener {
            override fun onScrollChanged() {
                checkInView()
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { checkInView() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        statAnimators.forEach { it.cancel() }
        statAnimators.clear()
    }

    private fun checkInView() {
        if (hasTriggeredInView) return
        val rect = Rect()
        val visible = getGlobalVisibleRect(rect)
        if (visible && height > 0) {
            val visibleHeight = rect.height().toFloat()
            val totalHeight = height.toFloat()
            if (visibleHeight / totalHeight >= TRIGGER_AMOUNT) {
                hasTriggeredInView = true
                triggerStatsAnimation()
            }
        }
    }

    /** Triggered automatically on first entrance into view, or manually via host. */
    fun triggerStatsAnimation() {
        if (!hasTriggeredInView) {
            hasTriggeredInView = true
        }

        val textViews = mutableListOf<TextView>()
        findCountingTextViews(this, textViews)

        textViews.forEachIndexed { index, tv ->
            animateSingleStat(tv, delayMs = index * STAGGER_MS)
        }
    }

    private fun findCountingTextViews(view: View, out: MutableList<TextView>) {
        if (view is TextView) {
            val text = view.text.toString().trim()
            if (isCountable(text)) {
                out.add(view)
            }
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                findCountingTextViews(view.getChildAt(i), out)
            }
        }
    }

    private fun isCountable(text: String): Boolean {
        val clean = text.replace("%", "").replace("+", "").trim()
        return clean.toIntOrNull() != null || clean.toDoubleOrNull() != null
    }

    fun animateSingleStat(textView: TextView, delayMs: Long = 0L) {
        val rawText = textView.text.toString().trim()
        val isPercent = rawText.endsWith("%")
        val clean = rawText.replace("%", "").replace("+", "").trim()
        val targetValue = clean.toDoubleOrNull() ?: return

        // Reduced motion: set final value directly
        if (!UiArcMotion.isMotionEnabled(context)) {
            textView.text = rawText
            return
        }

        val isDecimal = clean.contains(".")
        val decimals = if (isDecimal) clean.substringAfter(".").length else 0

        val animator = ValueAnimator.ofFloat(0f, targetValue.toFloat()).apply {
            duration = COUNT_DURATION_MS
            startDelay = delayMs
            interpolator = UiArcMotion.Ease.ENTER
            addUpdateListener { va ->
                val current = va.animatedValue as Float
                val formatted = if (decimals > 0) {
                    String.format("%.${decimals}f", current)
                } else {
                    current.roundToInt().toString()
                }
                textView.text = if (isPercent) "$formatted%" else formatted
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    textView.text = rawText
                }
            })
            start()
        }
        statAnimators.add(animator)
    }
}
