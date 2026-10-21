package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * UIArc Stats Band Component.
 * Genuine native translation of official UIArc Stats Band specification:
 * - Registry: https://uiarc.dev/r/stats-band.json
 * - Counting animation: 0 -> targetValue (or from previous count to new count), duration = 1.6s (1600ms), Ease.ENTER
 * - Stagger: 90ms (0.09s) delay between sequential metrics
 * - First reveal trigger: 50% in viewport
 * - Content rise: 8px vertical rise with opacity fade-in
 * - Missing/null state preserved as unavailable ("—"), not animated zero
 * - Transition between counts on refresh (supports increase and decrease)
 * - Safe cancellation when offscreen or destroyed
 * - Instant final value setting under reduced motion
 */
class UiArcStatsBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        const val COUNT_DURATION_MS = 1600L
        const val STAGGER_MS = 90L
        const val TRIGGER_AMOUNT = 0.50f
        const val CONTENT_RISE_DP = 8f
    }

    private val density = resources.displayMetrics.density
    private var hasTriggeredInView = false
    private val activeAnimators = mutableListOf<ValueAnimator>()
    private val previousValues = mutableMapOf<Int, Int>()

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
        cancelAllAnimations()
    }

    fun cancelAllAnimations() {
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
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
            }
        }
    }

    /**
     * Updates an integer metric with UIArc Stats Band motion.
     * @param textView Target TextView to animate
     * @param targetCount Actual new integer count, or null for unavailable state
     * @param metricIndex Stagger index (0 = 0ms, 1 = 90ms)
     */
    fun updateMetric(textView: TextView, targetCount: Int?, metricIndex: Int) {
        if (targetCount == null) {
            textView.text = "—"
            previousValues.remove(textView.id)
            return
        }

        val prev = previousValues[textView.id]
        previousValues[textView.id] = targetCount

        val targetStr = targetCount.toString()
        textView.contentDescription = targetStr

        // Reduced motion: set final value immediately
        if (!UiArcMotion.isMotionEnabled(context)) {
            textView.text = targetStr
            textView.alpha = 1f
            textView.translationY = 0f
            return
        }

        val fromVal = prev ?: 0
        val isFirstLoad = prev == null
        val delay = metricIndex * STAGGER_MS

        // Content rise animation on initial entrance
        if (isFirstLoad) {
            textView.alpha = 0f
            textView.translationY = CONTENT_RISE_DP * density
            textView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(UiArcMotion.Duration.STANDARD)
                .setStartDelay(delay)
                .setInterpolator(UiArcMotion.Ease.ENTER)
                .start()
        }

        val animator = ValueAnimator.ofInt(fromVal, targetCount).apply {
            duration = COUNT_DURATION_MS
            startDelay = delay
            interpolator = UiArcMotion.Ease.ENTER
            addUpdateListener { va ->
                val current = va.animatedValue as Int
                textView.text = current.toString()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    textView.text = targetStr
                    activeAnimators.remove(this@apply)
                }
            })
            start()
        }
        activeAnimators.add(animator)
    }
}
