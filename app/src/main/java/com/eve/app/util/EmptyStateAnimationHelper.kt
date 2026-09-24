package com.eve.app.util

import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R

/**
 * Task 2: Ensures the NO_FILES empty-state animation plays exactly ONCE
 * and stays frozen on its final frame.
 *
 * Rules:
 * - Plays exactly 1 time (repeatCount = 0).
 * - Stays frozen on its final frame (progress = 1.0f).
 * - Does NOT replay on scroll, rebind, onResume, focus change, rotation, or theme switch.
 * - Replays only when transitioning from "has data" to "empty", or freshly navigating.
 */
object EmptyStateAnimationHelper {

    /**
     * Plays the animation once if [hasPlayed] is false; otherwise freezes on the final frame.
     * @return true to indicate the animation has been played/frozen for this empty state.
     */
    fun showEmptyState(
        lottieView: LottieAnimationView,
        hasPlayed: Boolean
    ): Boolean {
        lottieView.repeatCount = 0
        lottieView.setAnimation(R.raw.no_files)
        return if (!hasPlayed) {
            lottieView.progress = 0f
            lottieView.playAnimation()
            true
        } else {
            if (lottieView.isAnimating) {
                lottieView.pauseAnimation()
            }
            lottieView.progress = 1.0f
            true
        }
    }
}
