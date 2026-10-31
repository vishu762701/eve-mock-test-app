package com.eve.app.ui.profile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.abs

/**
 * ProfileActionsView.java:520-587 uses ButtonBounce.java:60-94,119-126 with diff=.04.
 * Isolated EVE view-host integration. The cubic Bézier and overshoot parameters
 * are exact source values, while native LinearLayout input dispatch is adapted.
 */
internal class ProfileActionBounce private constructor(private val view: View) : View.OnTouchListener {
    companion object {
        const val PRESS_DURATION_MS = 60L
        const val RELEASE_DURATION_MS = 350L
        const val MAX_DRIFT_PX = 20f
        const val LONG_PRESS_HAPTIC_MS = 250L
        const val SCALE_DIFF = 0.04f
        private const val OVERSHOOT = 5f

        fun install(view: View): ProfileActionBounce {
            val effect = ProfileActionBounce(view)
            view.setOnTouchListener(effect)
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    effect.cancelAnimationAndReset()
                }
            })
            return effect
        }
    }

    private val pressCurve = PathInterpolator(0.25f, 0.1f, 0.25f, 1f)
    private var pressProgress = 0f
    private var animator: ValueAnimator? = null
    private var isDown = false
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    private fun animatePressed(pressed: Boolean) {
        animator?.cancel()
        val next = ValueAnimator.ofFloat(pressProgress, if (pressed) 1f else 0f)
        animator = next
        next.interpolator = if (pressed) pressCurve else OvershootInterpolator(OVERSHOOT)
        next.duration = if (pressed) PRESS_DURATION_MS else RELEASE_DURATION_MS
        next.addUpdateListener {
            pressProgress = it.animatedValue as Float
            val scale = 1f - SCALE_DIFF * pressProgress
            view.scaleX = scale
            view.scaleY = scale
        }
        next.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animator === animation) {
                    animator = null
                    pressProgress = if (pressed) 1f else 0f
                    val scale = 1f - SCALE_DIFF * pressProgress
                    view.scaleX = scale
                    view.scaleY = scale
                }
            }
        })
        next.start()
    }

    private fun cancelAnimationAndReset() {
        animator?.cancel()
        animator = null
        isDown = false
        pressProgress = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!v.isEnabled || !v.isClickable) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDown = true
                downX = event.x
                downY = event.y
                downTime = event.eventTime
                animatePressed(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDown && (abs(event.x - downX) > MAX_DRIFT_PX ||
                        abs(event.y - downY) > MAX_DRIFT_PX)) {
                    isDown = false
                    animatePressed(false)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val click = isDown &&
                    event.x >= 0 && event.y >= 0 &&
                    event.x < v.width && event.y < v.height
                isDown = false
                animatePressed(false)
                if (click) {
                    if (event.eventTime - downTime > LONG_PRESS_HAPTIC_MS) {
                        v.performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        )
                    }
                    // performClick routes through the existing listener and keeps
                    // keyboard/TalkBack activation on the platform click pathway.
                    v.performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                if (isDown) {
                    isDown = false
                    animatePressed(false)
                }
                return true
            }
            else -> return isDown
        }
    }
}
