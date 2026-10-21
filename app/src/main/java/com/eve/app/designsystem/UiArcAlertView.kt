package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.eve.app.util.ThemeManager
import com.eve.app.util.ThemeSwitchAnimator

/**
 * UIArc Alert Component.
 * Genuine native translation of official UIArc Alert specification:
 * - Registry: https://uiarc.dev/r/alert.json
 * - Entrance: Height morphs 0 -> auto with Spring.SMOOTH (500ms), Opacity 0 -> 1 with Ease.ENTER (240ms)
 * - Exit: Opacity 1 -> 0 with Ease.STANDARD (160ms), Height morphs auto -> 0 with Spring.SMOOTH (500ms)
 * - Telegram theme transition protection: Bypasses animation during ThemeManager/ThemeSwitchAnimator transition
 * - Instant fallback when reduced motion is requested
 * - TalkBack announcement on entrance
 */
class UiArcAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val ENTRANCE_OPACITY_DURATION_MS = 240L
        const val EXIT_OPACITY_DURATION_MS = 160L
        const val HEIGHT_DURATION_MS = 500L
    }

    private var heightAnimator: ValueAnimator? = null
    private var opacityAnimator: ValueAnimator? = null
    private var isExiting = false
    private var isEntering = false

    fun setAlertVisible(visible: Boolean) {
        visibility = if (visible) VISIBLE else GONE
    }

    override fun setVisibility(newVisibility: Int) {
        if (newVisibility == visibility && !isExiting && !isEntering) {
            return
        }

        val motionDisabled = !UiArcMotion.isMotionEnabled(context)
        val themeTransitioning = ThemeManager.isTransitioning || ThemeSwitchAnimator.isTransitioning

        if (!isAttachedToWindow || motionDisabled || themeTransitioning) {
            cancelAnimations()
            super.setVisibility(newVisibility)
            alpha = if (newVisibility == VISIBLE) 1f else 0f
            layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            return
        }

        if (newVisibility == VISIBLE) {
            animateEntrance()
        } else if (newVisibility == GONE || newVisibility == INVISIBLE) {
            animateExit(newVisibility)
        }
    }

    private fun cancelAnimations() {
        heightAnimator?.cancel()
        heightAnimator = null
        opacityAnimator?.cancel()
        opacityAnimator = null
        isExiting = false
        isEntering = false
    }

    private fun animateEntrance() {
        val wasExiting = isExiting
        cancelAnimations()
        isEntering = true

        super.setVisibility(VISIBLE)

        val parentView = parent as? View
        val availableWidth = when {
            width > 0 -> width
            parentView != null && parentView.width > 0 -> parentView.width - parentView.paddingLeft - parentView.paddingRight
            else -> resources.displayMetrics.widthPixels
        }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(widthSpec, heightSpec)
        val targetHeight = measuredHeight

        val startingHeight = if (wasExiting) height else 0
        val startingAlpha = if (wasExiting) alpha else 0f

        if (targetHeight <= 0) {
            layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            alpha = 1f
            isEntering = false
            return
        }

        layoutParams?.height = startingHeight
        requestLayout()
        alpha = startingAlpha

        val hAnim = ValueAnimator.ofInt(startingHeight, targetHeight).apply {
            duration = HEIGHT_DURATION_MS
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener {
                val h = it.animatedValue as Int
                layoutParams?.height = h
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    requestLayout()
                    heightAnimator = null
                    isEntering = false
                }
            })
        }
        heightAnimator = hAnim
        hAnim.start()

        val oAnim = ValueAnimator.ofFloat(startingAlpha, 1f).apply {
            duration = ENTRANCE_OPACITY_DURATION_MS
            interpolator = UiArcMotion.Ease.ENTER
            addUpdateListener {
                alpha = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    opacityAnimator = null
                }
            })
        }
        opacityAnimator = oAnim
        oAnim.start()

        if (text.isNotBlank()) {
            announceForAccessibility(text)
        }
    }

    private fun animateExit(finalVisibility: Int) {
        val wasEntering = isEntering
        cancelAnimations()
        isExiting = true

        val startingHeight = height
        val startingAlpha = alpha

        if (startingHeight <= 0) {
            super.setVisibility(finalVisibility)
            layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            alpha = 1f
            isExiting = false
            return
        }

        val oAnim = ValueAnimator.ofFloat(startingAlpha, 0f).apply {
            duration = EXIT_OPACITY_DURATION_MS
            interpolator = UiArcMotion.Ease.STANDARD
            addUpdateListener {
                alpha = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    opacityAnimator = null
                }
            })
        }
        opacityAnimator = oAnim
        oAnim.start()

        val hAnim = ValueAnimator.ofInt(startingHeight, 0).apply {
            duration = HEIGHT_DURATION_MS
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener {
                val h = it.animatedValue as Int
                layoutParams?.height = h
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super@UiArcAlertView.setVisibility(finalVisibility)
                    layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    alpha = 1f
                    requestLayout()
                    heightAnimator = null
                    isExiting = false
                }
            })
        }
        heightAnimator = hAnim
        hAnim.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAnimations()
    }
}
