package com.eve.app.util

import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieFrameInfo
import com.airbnb.lottie.value.LottieValueCallback
import com.eve.app.R

/**
 * Ensures the SEARCH Lottie animation (res/raw/search.json) is loaded
 * on a transparent background, plays exactly ONCE, and stays frozen on its final frame.
 * Supports theme-aware dynamic color mapping in dark mode without per-frame allocations.
 */
object EmptyStateAnimationHelper {

    private val keyPathAll = KeyPath("**")

    private val fillColorCallback = object : LottieValueCallback<Int>() {
        override fun getValue(frameInfo: LottieFrameInfo<Int>?): Int {
            val orig = frameInfo?.startValue ?: return 0
            return com.eve.app.designsystem.DesignLottieTokens.emptyStateColor(orig, isStroke = false)
        }
    }

    private val strokeColorCallback = object : LottieValueCallback<Int>() {
        override fun getValue(frameInfo: LottieFrameInfo<Int>?): Int {
            val orig = frameInfo?.startValue ?: return 0
            return com.eve.app.designsystem.DesignLottieTokens.emptyStateColor(orig, isStroke = true)
        }
    }


    fun applyTheme(lottieView: LottieAnimationView, isDark: Boolean = ThemeSwitchAnimator.isDarkMode(lottieView.context)) {
        if (isDark) {
            lottieView.addValueCallback(keyPathAll, LottieProperty.COLOR, fillColorCallback)
            lottieView.addValueCallback(keyPathAll, LottieProperty.STROKE_COLOR, strokeColorCallback)
        } else {
            lottieView.addValueCallback(keyPathAll, LottieProperty.COLOR, null as LottieValueCallback<Int>?)
            lottieView.addValueCallback(keyPathAll, LottieProperty.STROKE_COLOR, null as LottieValueCallback<Int>?)
        }
    }

    /**
     * Plays the SEARCH animation once from start to end, holding on the final frame (non-looping).
     * If hasPlayed is true (e.g. from savedInstanceState or recreation), holds on final frame immediately.
     * @return true to indicate the animation has played or is playing.
     */
    fun showEmptyState(
        lottieView: LottieAnimationView,
        hasPlayed: Boolean = false,
        rawResId: Int = R.raw.search,
        onAnimationEnd: (() -> Unit)? = null
    ): Boolean {
        lottieView.setBackgroundResource(android.R.color.transparent)
        lottieView.repeatCount = 0

        // If already actively playing target animation, let it finish cleanly without interruption
        if (lottieView.isAnimating && lottieView.tag == rawResId) {
            return true
        }

        val currentTag = lottieView.tag
        if (currentTag != rawResId) {
            lottieView.cancelAnimation()
            lottieView.setAnimation(rawResId)
            lottieView.tag = rawResId
        }

        val isDark = ThemeSwitchAnimator.isDarkMode(lottieView.context)
        applyTheme(lottieView, isDark)

        if (hasPlayed) {
            // Already played to completion in this screen session; hold on final frame
            if (lottieView.composition != null) {
                lottieView.pauseAnimation()
                lottieView.progress = 1.0f
            } else {
                lottieView.addLottieOnCompositionLoadedListener {
                    applyTheme(lottieView, isDark)
                    lottieView.repeatCount = 0
                    lottieView.pauseAnimation()
                    lottieView.progress = 1.0f
                }
            }
            return true
        }

        lottieView.removeAllAnimatorListeners()
        lottieView.addAnimatorListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                lottieView.pauseAnimation()
                lottieView.progress = 1.0f
                onAnimationEnd?.invoke()
            }
        })

        if (lottieView.composition != null) {
            lottieView.repeatCount = 0
            lottieView.progress = 0f
            lottieView.playAnimation()
        } else {
            lottieView.addLottieOnCompositionLoadedListener {
                applyTheme(lottieView, isDark)
                lottieView.repeatCount = 0
                lottieView.progress = 0f
                lottieView.playAnimation()
            }
        }

        return true
    }

    /**
     * Stops the empty state animation when content loads.
     */
    fun stopEmptyState(lottieView: LottieAnimationView) {
        if (lottieView.isAnimating) {
            lottieView.cancelAnimation()
        }
    }

    /**
     * Shows the 404 ERROR animation (res/raw/error_404.json).
     * Cancels any running animation, clears dark-mode color value callbacks,
     * sets R.raw.error_404, sets lottieView.tag = R.raw.error_404, and plays.
     */
    fun showErrorState(lottieView: LottieAnimationView) {
        lottieView.cancelAnimation()
        lottieView.removeAllAnimatorListeners()
        lottieView.addValueCallback(keyPathAll, LottieProperty.COLOR, null as LottieValueCallback<Int>?)
        lottieView.addValueCallback(keyPathAll, LottieProperty.STROKE_COLOR, null as LottieValueCallback<Int>?)
        lottieView.setAnimation(R.raw.error_404)
        lottieView.tag = R.raw.error_404
        lottieView.repeatCount = 0
        lottieView.playAnimation()
    }
}
