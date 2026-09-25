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
     * Plays the search animation once from start to end, holding on the final frame (non-looping).
     * @return true to indicate the animation has played.
     */
    fun showEmptyState(
        lottieView: LottieAnimationView,
        hasPlayed: Boolean = false
    ): Boolean {
        lottieView.setBackgroundResource(android.R.color.transparent)
        lottieView.repeatCount = 0
        lottieView.setAnimation(R.raw.search)
        if (hasPlayed) {
            lottieView.progress = 1.0f
        } else {
            lottieView.progress = 0f
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
