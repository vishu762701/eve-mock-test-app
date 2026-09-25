package com.eve.app.util

import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.eve.app.R

/**
 * Task E: Ensures the search_empty.json Lottie animation is loaded and played,
 * looping continuously while the empty state is visible, and stopped when content loads.
 */
object EmptyStateAnimationHelper {

    /**
     * Plays the search_empty animation looping continuously.
     * @return true to indicate the animation is playing.
     */
    fun showEmptyState(
        lottieView: LottieAnimationView,
        @Suppress("UNUSED_PARAMETER") hasPlayed: Boolean = false
    ): Boolean {
        lottieView.repeatCount = LottieDrawable.INFINITE
        lottieView.setAnimation(R.raw.search_empty)
        if (!lottieView.isAnimating) {
            lottieView.playAnimation()
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
}
