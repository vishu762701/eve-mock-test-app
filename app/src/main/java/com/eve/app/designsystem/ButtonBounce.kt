package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import com.eve.app.util.TelegramThemeEasing

/**
 * Port of Telegram's ButtonBounce (org.telegram.ui.Components.ButtonBounce).
 * Source reference: TMessagesProj/src/main/java/org/telegram/ui/Components/ButtonBounce.java
 * Provides the official Telegram press spring-back motion and scale calculations.
 */
class ButtonBounce constructor(
    var view: View? = null,
    private val durationPressMultiplier: Float = 1f,
    private val durationReleaseMultiplier: Float = 1f,
    private val overshoot: Float = 5.0f
) {
    constructor(viewToInvalidate: View?) : this(viewToInvalidate, 1f, 1f, 5.0f)

    constructor(viewToInvalidate: View?, durationMultiplier: Float, overshoot: Float) :
        this(viewToInvalidate, durationMultiplier, durationMultiplier, overshoot)

    private var releaseDelay: Long = 0L
    private var additionalInvalidate: Runnable? = null
    private var animator: ValueAnimator? = null
    private var bounceAnimator: ValueAnimator? = null
    private var bounceScale = 1f
    private var isPressedState = false
    private var pressedT = 0f

    fun setAdditionalInvalidate(runnable: Runnable?) {
        this.additionalInvalidate = runnable
    }

    fun setReleaseDelay(delay: Long): ButtonBounce {
        this.releaseDelay = delay
        return this
    }

    fun setPressed(pressed: Boolean) {
        if (isPressedState != pressed) {
            isPressedState = pressed
            animator?.cancel()
            animator = ValueAnimator.ofFloat(pressedT, if (pressed) 1f else 0f).apply {
                addUpdateListener { anm ->
                    pressedT = anm.animatedValue as Float
                    view?.scaleX = getScale(0.04f)
                    view?.scaleY = getScale(0.04f)
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (animation === animator) {
                            animator = null
                            pressedT = if (pressed) 1f else 0f
                            view?.scaleX = getScale(0.04f)
                            view?.scaleY = getScale(0.04f)
                            invalidate()
                        }
                    }
                })
                if (isPressedState) {
                    interpolator = TelegramThemeEasing
                    duration = (60 * durationPressMultiplier).toLong()
                    startDelay = 0
                } else {
                    interpolator = OvershootInterpolator(overshoot)
                    duration = (350 * durationReleaseMultiplier).toLong()
                    startDelay = releaseDelay
                }
                start()
            }
        }
    }

    fun isPressedProgress(): Float = pressedT

    fun getScale(diff: Float): Float {
        return ((1f - diff) + diff * (1f - pressedT)) * bounceScale
    }

    fun isPressed(): Boolean = isPressedState

    fun invalidate() {
        view?.invalidate()
        additionalInvalidate?.run()
    }
}
