package com.eve.app.util

import android.view.View

/**
 * Helper to smoothly cross-fade from shimmer skeleton placeholders to loaded content.
 */
object ShimmerHelper {
    const val DEFAULT_CROSS_FADE_DURATION = 250L

    fun crossFade(shimmerView: View?, contentView: View?, durationMs: Long = DEFAULT_CROSS_FADE_DURATION) {
        if (shimmerView == null || contentView == null) return
        if (contentView.visibility == View.VISIBLE && shimmerView.visibility == View.GONE) return

        contentView.alpha = 0f
        contentView.visibility = View.VISIBLE
        contentView.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .start()

        shimmerView.animate()
            .alpha(0f)
            .setDuration(durationMs)
            .withEndAction {
                shimmerView.visibility = View.GONE
                shimmerView.alpha = 1f
            }
            .start()
    }

    fun showContentImmediately(shimmerView: View, contentView: View) {
        shimmerView.animate().withEndAction(null).cancel()
        contentView.animate().withEndAction(null).cancel()
        shimmerView.visibility = View.GONE
        shimmerView.alpha = 1f
        contentView.visibility = View.VISIBLE
        contentView.alpha = 1f
    }

    fun showShimmer(shimmerView: View?, contentView: View?) {
        shimmerView?.alpha = 1f
        shimmerView?.visibility = View.VISIBLE
        contentView?.visibility = View.GONE
    }
}
