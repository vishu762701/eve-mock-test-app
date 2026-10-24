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
    private var scrollObserver: ViewTreeObserver? = null
    private val visibilityCheck = Runnable { checkInView() }
    private val activeAnimators = mutableListOf<ValueAnimator>()
    private val metricEntries = mutableMapOf<Int, MetricEntry>()

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        checkInView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (scrollObserver == null && viewTreeObserver.isAlive) {
            scrollObserver = viewTreeObserver.also {
                it.addOnScrollChangedListener(scrollListener)
            }
        }
        post(visibilityCheck)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(visibilityCheck)
        scrollObserver?.takeIf { it.isAlive }?.removeOnScrollChangedListener(scrollListener)
        scrollObserver = null
        cancelAllAnimations()
        super.onDetachedFromWindow()
    }

    fun cancelAllAnimations() {
        metricEntries.values.forEach { entry ->
            if (entry.animator != null) {
                entry.displayedCount = entry.textView.text.toString().toLongOrNull()
                entry.animator?.cancel()
                entry.animator = null
                // A canceled first reveal has not completed its entrance.
                if (entry.displayedCount != entry.targetCount) entry.hasAnimatedEntrance = false
            }
            entry.textView.animate().cancel()
            entry.textView.alpha = 1f
            entry.textView.translationY = 0f
        }
        activeAnimators.clear()
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
        // Every new target must wait for actual >=50% visibility, even after
        // a previous metric successfully completed its first entrance.
        if (!isFiftyPercentVisible()) {
            // Scroll can hide the band while the 1.6s counter is still running.
            // Keep the last visible integer as the next animation's starting point.
            if (activeAnimators.isNotEmpty()) cancelAllAnimations()
            return
        }
        hasTriggeredInView = true
        triggerPendingMetrics()
    }

    private fun triggerPendingMetrics() {
        metricEntries.values.forEach { entry ->
            if (entry.targetCount != null && entry.animator == null &&
                (!entry.hasAnimatedEntrance || entry.displayedCount != entry.targetCount)
            ) {
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

        // Stop counters when a hidden tab receives an update without a scroll event.
        // Snapshot before cancellation so returning to the viewport can resume.
        if (!isFiftyPercentVisible() && activeAnimators.isNotEmpty()) {
            cancelAllAnimations()
        }

        // A repeated emission must not jump an in-flight counter to its final value.
        if (entry.targetCount == targetCount && entry.animator != null) return
        if (entry.targetCount == targetCount && entry.hasAnimatedEntrance &&
            entry.displayedCount == targetCount
        ) return

        // A changed server value may arrive mid-animation; restart from the number
        // actually on screen, never from the previously completed server value.
        if (entry.animator != null) {
            entry.displayedCount = textView.text.toString().toLongOrNull() ?: entry.displayedCount
            entry.animator?.cancel()
            entry.animator = null
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
        if (!isFiftyPercentVisible()) {
            // Preserve a previously completed number when a fresh target arrives offscreen.
            textView.text = entry.displayedCount?.toString() ?: "—"
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
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) {
                        textView.text = targetStr
                        entry.displayedCount = targetCount
                    }
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
