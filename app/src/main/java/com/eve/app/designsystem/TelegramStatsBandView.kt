/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Cells/StatisticPostInfoCell.java:30-190 @ 3de37c8cfb03
 * Derived from Telegram Android StatisticActivity.java and NumberTextView.java
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

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
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import org.telegram.ui.Components.CubicBezierInterpolator
import kotlin.math.roundToLong

/**
 * 1:1 Telegram stats band view.
 * Staggered metric counting animation with CubicBezierInterpolator.EASE_OUT_QUINT physics
 * and viewport visibility gating.
 */
open class TelegramStatsBandView @JvmOverloads constructor(
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

    init {
        orientation = HORIZONTAL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scrollObserver = viewTreeObserver
        scrollObserver?.addOnScrollChangedListener(scrollListener)
        post(visibilityCheck)
    }

    override fun onDetachedFromWindow() {
        cancelAllAnimations()
        if (scrollObserver?.isAlive == true) {
            scrollObserver?.removeOnScrollChangedListener(scrollListener)
        }
        scrollObserver = null
        removeCallbacks(visibilityCheck)
        super.onDetachedFromWindow()
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
        if (!isShown) return false
        val rect = Rect()
        val isPartiallyVisible = getGlobalVisibleRect(rect)
        if (!isPartiallyVisible) return false

        val visibleArea = rect.width() * rect.height()
        val totalArea = width * height
        if (totalArea <= 0) return false

        return (visibleArea.toFloat() / totalArea.toFloat()) >= TRIGGER_AMOUNT
    }

    fun checkInView() {
        if (!hasTriggeredInView && isFiftyPercentVisible()) {
            hasTriggeredInView = true
            triggerPendingMetrics()
            if (scrollObserver?.isAlive == true) {
                scrollObserver?.removeOnScrollChangedListener(scrollListener)
            }
            scrollObserver = null
        }
    }

    private fun triggerPendingMetrics() {
        metricEntries.values.forEach { entry ->
            if (!entry.hasAnimatedEntrance && entry.targetCount != null) {
                animateMetric(entry)
            }
        }
    }

    fun updateMetric(textView: TextView, targetCount: Long?, metricIndex: Int) {
        val entry = metricEntries.getOrPut(metricIndex) {
            MetricEntry(textView, targetCount, null, metricIndex)
        }

        val previousTarget = entry.targetCount
        entry.targetCount = targetCount

        if (targetCount == null) {
            entry.animator?.cancel()
            entry.animator = null
            entry.displayedCount = null
            entry.hasAnimatedEntrance = false
            textView.text = "—"
            return
        }

        if (!TelegramAnimationRuntime.isMotionEnabled(context)) {
            entry.animator?.cancel()
            entry.animator = null
            entry.displayedCount = targetCount
            entry.hasAnimatedEntrance = true
            textView.text = targetCount.toString()
            return
        }

        if (hasTriggeredInView) {
            if (previousTarget != targetCount || !entry.hasAnimatedEntrance) {
                animateMetric(entry)
            }
        } else {
            if (entry.displayedCount == null) {
                textView.text = "0"
            }
            post(visibilityCheck)
        }
    }

    fun updateMetric(textView: TextView, targetCount: Int?, metricIndex: Int) {
        updateMetric(textView, targetCount?.toLong(), metricIndex)
    }

    private fun animateMetric(entry: MetricEntry) {
        entry.animator?.cancel()
        val startVal = entry.displayedCount ?: 0L
        val endVal = entry.targetCount ?: return

        entry.hasAnimatedEntrance = true
        val textView = entry.textView

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COUNT_DURATION_MS
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            startDelay = (entry.metricIndex * STAGGER_MS).coerceAtLeast(0L)

            addUpdateListener { va ->
                val fraction = va.animatedFraction
                val current = (startVal + (endVal - startVal) * fraction).roundToLong()
                entry.displayedCount = current
                textView.text = current.toString()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    activeAnimators.remove(animation)
                    entry.animator = null
                }
                override fun onAnimationEnd(animation: Animator) {
                    activeAnimators.remove(animation)
                    entry.animator = null
                    entry.displayedCount = endVal
                    textView.text = endVal.toString()
                }
            })
        }

        entry.animator = animator
        activeAnimators.add(animator)
        animator.start()
    }
}
