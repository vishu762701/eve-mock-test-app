package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * UIArc Expandable View / Accordion Component.
 * Genuine native translation of official UIArc Expandable Card & Accordion specifications:
 * - Registry: https://uiarc.dev/r/expandable-card.json and https://uiarc.dev/r/accordion.json
 * - Critically damped height spring (spring.smooth) where close exactly mirrors open
 * - Content enters with enter easing (240ms) and exits fast (160ms) with subtle vertical shift (-6dp)
 * - Collapsed panels leave accessibility focus tree
 * - Reduced motion branch for instantaneous expand/collapse
 */
class UiArcExpandableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var isExpanded: Boolean = false
        private set

    var onExpansionChangeListener: ((Boolean) -> Unit)? = null

    private var heightAnimator: ValueAnimator? = null
    private var contentTargetHeight: Int = 0
    private val subtleShiftPx = 6f * resources.displayMetrics.density

    init {
        orientation = VERTICAL
        clipChildren = true
        clipToPadding = true
    }

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        if (isExpanded == expanded) return
        isExpanded = expanded

        if (!animate || !UiArcMotion.isMotionEnabled(context)) {
            heightAnimator?.cancel()
            visibility = if (expanded) View.VISIBLE else View.GONE
            alpha = if (expanded) 1.0f else 0.0f
            translationY = 0f
            layoutParams = layoutParams?.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            onExpansionChangeListener?.invoke(expanded)
            return
        }

        if (expanded) {
            expandWithMotion()
        } else {
            collapseWithMotion()
        }
    }

    fun toggle(animate: Boolean = true) {
        setExpanded(!isExpanded, animate)
    }

    private fun expandWithMotion() {
        heightAnimator?.cancel()
        visibility = View.VISIBLE
        alpha = 0f
        translationY = -subtleShiftPx

        // Measure natural content height
        measure(
            MeasureSpec.makeMeasureSpec((parent as? View)?.width ?: resources.displayMetrics.widthPixels, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        contentTargetHeight = measuredHeight

        heightAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener { va ->
                val fraction = va.animatedValue as Float
                val currentH = (contentTargetHeight * fraction).toInt()
                layoutParams = layoutParams?.apply { height = currentH }

                // Content opacity & translation
                val enterFraction = (fraction / (UiArcMotion.Duration.STANDARD.toFloat() / duration)).coerceIn(0f, 1f)
                val interpolatedEnter = UiArcMotion.Ease.ENTER.getInterpolation(enterFraction)
                alpha = interpolatedEnter
                translationY = -subtleShiftPx * (1f - interpolatedEnter)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    layoutParams = layoutParams?.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
                    alpha = 1f
                    translationY = 0f
                    onExpansionChangeListener?.invoke(true)
                }
            })
            start()
        }
    }

    private fun collapseWithMotion() {
        heightAnimator?.cancel()
        val initialHeight = height

        heightAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener { va ->
                val fraction = va.animatedValue as Float
                layoutParams = layoutParams?.apply { height = (initialHeight * fraction).toInt() }

                // Exit fade runs faster (160ms)
                val exitFraction = (1f - fraction) * (duration / UiArcMotion.Duration.FAST.toFloat())
                val clampedExit = exitFraction.coerceIn(0f, 1f)
                alpha = 1f - UiArcMotion.Ease.STANDARD.getInterpolation(clampedExit)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    layoutParams = layoutParams?.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
                    alpha = 0f
                    translationY = 0f
                    onExpansionChangeListener?.invoke(false)
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        heightAnimator?.cancel()
    }
}
