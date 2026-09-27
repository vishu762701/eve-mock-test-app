package com.eve.app.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R

/**
 * Ensures the NO_FILES (open folder with question mark) Lottie animation is loaded
 * directly on a transparent background, plays exactly ONCE, and stays frozen on its final frame.
 */
object EmptyStateAnimationHelper {

    /**
     * Plays the NO_FILES animation once from start to end, holding on the final frame (non-looping).
     * If hasPlayed is true (e.g. from savedInstanceState or recreation), holds on final frame immediately.
     * @return true to indicate the animation has played or is playing.
     */
    fun showEmptyState(
        lottieView: LottieAnimationView,
        hasPlayed: Boolean = false
    ): Boolean {
        lottieView.setBackgroundResource(android.R.color.transparent)
        lottieView.repeatCount = 0

        val currentTag = lottieView.tag
        if (currentTag != R.raw.no_files) {
            lottieView.setAnimation(R.raw.no_files)
            lottieView.tag = R.raw.no_files
        }

        if (hasPlayed) {
            // Already played in this screen session; freeze at final frame without replaying
            if (lottieView.isAnimating) {
                lottieView.cancelAnimation()
            }
            if (lottieView.composition != null) {
                lottieView.pauseAnimation()
                lottieView.progress = 1.0f
            } else {
                lottieView.addLottieOnCompositionLoadedListener {
                    lottieView.repeatCount = 0
                    lottieView.pauseAnimation()
                    lottieView.progress = 1.0f
                }
            }
            return true
        }

        if (lottieView.isAnimating) {
            // Already actively playing in this open session; let it finish cleanly
            return true
        }

        if (lottieView.composition != null) {
            lottieView.repeatCount = 0
            lottieView.progress = 0f
            lottieView.playAnimation()
        } else {
            lottieView.addLottieOnCompositionLoadedListener {
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
}
