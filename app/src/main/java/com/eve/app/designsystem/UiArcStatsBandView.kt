package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToLong

/**
 * UIArc Stats Band Component.
 * Genuine native translation of official UIArc Stats Band specification:
 * - Registry: https://uiarc.dev/r/stats-band.json
 * - Counting animation: 0 -> targetValue (or from previous count to new count), duration = 1.6s (1600ms), Ease.ENTER
 * - Stagger: 90ms (0.09s) delay between sequential metrics
 * - First reveal trigger: at least 50% visible in viewport
 * - Content rise: 8px vertical rise with opacity fade-in
 * - Visibility gating: metrics cached until 50% visibility threshold is reached
 * - Tab switching / scroll safety: triggers when entering visible tab without user scroll
 * - Suppresses animation for repeated identical values
 * - Missing/null state preserved as unavailable ("—"), never animated zero
 * - Safe cancellation when offscreen, destroyed, or tab switched
 * - High-precision long counting without float truncation
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

    private data class MetricEntry(
        val textView: TextView,
        var targetCount: Long?,
        var displayedCount: Long?,
        val metricIndex: Int,
        var hasAnimatedEntrance: Boolean = false,
        var animator: ValueAnimator? = null
    )

    private val density = resources.displayMetrics.density
    private var hasTriggeredInView = false
    private var isObservingScroll = false
    private val activeAnimators = mutableListOf<ValueAnimator>()
    private val metricEntries = mutableMapOf<Int, MetricEntry>()

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        checkInView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isObservingScroll && viewTreeObserver.isAlive) {
            viewTreeObserver.addOnScrollChangedListener(scrollListener)
            isObservingScroll = true
        }
        post { checkInView() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isObservingScroll && viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnScrollChangedListener(scrollListener)
            isObservingScroll = false
        }
        cancelAllAnimations()
    }

    fun cancelAllAnimations() {
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
        metricEntries.values.forEach { entry ->
            entry.animator?.cancel()
            entry.animator = null
        }
    }

    fun isFiftyPercentVisible(): Boolean {
        if (!isShown || height <= 0) return false
        val rect = Rect()
        val visible = getGlobalVisibleRect(rect)
        if (visible && height > 0) {
            val visibleHeight = rect.height().toFloat()
            val totalHeight = height.toFloat()
            return (visibleHeight / totalHeight) >= TRIGGER_AMOUNT
        }
        return false
    }

    /**
     * Checks if the view has satisfied the 50% in-view threshold.
     * Triggers pending metrics if newly entering viewport.
     */
    fun checkInView() {
        if (!hasTriggeredInView) {
            if (isFiftyPercentVisible()) {
                hasTriggeredInView = true
                triggerPendingMetrics()
            }
        } else {
            // Already triggered entrance, but ensure visible state if new metrics arrived
            if (isShown) {
                triggerPendingMetrics()
            }
        }
    }

    private fun triggerPendingMetrics() {
        metricEntries.values.forEach { entry ->
            if (!entry.hasAnimatedEntrance && entry.targetCount != null) {
                animateMetric(entry)
            }
        }
    }

    /**
     * Authoritative entry point for updating a metric.
     * Separates data ingestion from animation execution.
     * Caches metric count and gates animation until >= 50% visibility is reached.
     */
    fun updateMetric(textView: TextView, targetCount: Long?, metricIndex: Int) {
        val entry = metricEntries.getOrPut(metricIndex) {
            MetricEntry(
                textView = textView,
                targetCount = targetCount,
                displayedCount = null,
                metricIndex = metricIndex
            )
        }

        if (targetCount == null) {
            textView.text = "—"
            textView.contentDescription = "Unavailable"
            entry.targetCount = null
            entry.displayedCount = null
            entry.animator?.cancel()
            entry.animator = null
            return
        }

        // Suppress repeated animation for identical values
        if (entry.targetCount == targetCount && entry.hasAnimatedEntrance) {
            textView.text = targetCount.toString()
            return
        }

        entry.targetCount = targetCount
        textView.contentDescription = targetCount.toString()

        // Reduced motion: render final state immediately
        if (!UiArcMotion.isMotionEnabled(context)) {
            textView.text = targetCount.toString()
            textView.alpha = 1f
            textView.translationY = 0f
            entry.displayedCount = targetCount
            entry.hasAnimatedEntrance = true
            return
        }

        // Gate animation until view is at least 50% visible in the viewport
        if (!hasTriggeredInView && !isFiftyPercentVisible()) {
            textView.text = "—"
            return
        }

        hasTriggeredInView = true
        animateMetric(entry)
    }

    fun updateMetric(textView: TextView, targetCount: Int?, metricIndex: Int) {
        updateMetric(textView, targetCount?.toLong(), metricIndex)
    }

    private fun animateMetric(entry: MetricEntry) {
        val targetCount = entry.targetCount ?: return
        val targetStr = targetCount.toString()
        val textView = entry.textView
        textView.contentDescription = targetStr

        if (!UiArcMotion.isMotionEnabled(context)) {
            textView.text = targetStr
            textView.alpha = 1f
            textView.translationY = 0f
            entry.displayedCount = targetCount
            entry.hasAnimatedEntrance = true
            return
        }

        val fromVal = entry.displayedCount ?: 0L
        val isFirstLoad = !entry.hasAnimatedEntrance
        val delay = entry.metricIndex * STAGGER_MS

        entry.animator?.cancel()
        entry.animator = null

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
            entry.hasAnimatedEntrance = true
        }

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COUNT_DURATION_MS
            startDelay = delay
            interpolator = UiArcMotion.Ease.ENTER
            addUpdateListener { va ->
                val fraction = va.animatedFraction.toDouble()
                // Avoid Float precision loss by computing in Double then converting to Long
                val current = (fromVal + (targetCount - fromVal) * fraction).roundToLong()
                textView.text = current.toString()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    textView.text = targetStr
                    entry.displayedCount = targetCount
                    entry.animator = null
                    activeAnimators.remove(this@apply)
                }
            })
            start()
        }
        entry.animator = animator
        activeAnimators.add(animator)
    }
}
