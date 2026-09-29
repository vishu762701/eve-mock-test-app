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
            return mapColor(orig, isStroke = false)
        }
    }

    private val strokeColorCallback = object : LottieValueCallback<Int>() {
        override fun getValue(frameInfo: LottieFrameInfo<Int>?): Int {
            val orig = frameInfo?.startValue ?: return 0
            return mapColor(orig, isStroke = true)
        }
    }

    private fun mapColor(original: Int, isStroke: Boolean): Int {
        val rgb = original and 0x00FFFFFF
        val alpha = original and 0xFF000000.toInt()
        val mappedRgb = if (isStroke) {
            when (rgb) {
                0xC7EBF5, 0xC4EDF5 -> 0x6CB6FF // High-contrast cyan/blue stroke
                0xE6E6E6 -> 0xE2E8F0 // Clean light outline
                0xA6CCD6 -> 0x93C5FD
                0x0A2B4A -> 0xDCEBF5 // Light outline on dark bg
                0x0A4F80 -> 0x6CB6FF
                0x2B4559 -> 0xCBD5E1
                0xFFFFFF -> 0xF8FAFC // Bright white stroke
                0xEB0000 -> 0xFF6B6B // Vivid coral red X
                0x6EE3FF -> 0x6EE3FF
                else -> rgb
            }
        } else {
            when (rgb) {
                0xFFFFFF -> 0xF8FAFC // Retain crisp white paper/shapes on dark background
                0xE6E6E6 -> 0xE2E8F0 // Bright soft grey instead of near-black #1C1C1F
                0xC7EBF5, 0xC4EDF5 -> 0x38BDF8 // Radiant lens glass
                0xA6CCD6 -> 0x60A5FA
                0x0A2B4A -> 0xDCEBF5 // Outlines visible against dark canvas
                0x0A4F80 -> 0x6CB6FF // Accent blue
                0x2B4559 -> 0x94A3B8 // Soft slate
                0xEB0000 -> 0xFF6B6B // High-contrast red
                0x6EE3FF -> 0x6EE3FF
                else -> rgb
            }
        }
        return alpha or mappedRgb
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
        hasPlayed: Boolean = false
    ): Boolean {
        lottieView.setBackgroundResource(android.R.color.transparent)
        lottieView.repeatCount = 0

        val currentTag = lottieView.tag
        if (currentTag != R.raw.search) {
            lottieView.setAnimation(R.raw.search)
            lottieView.tag = R.raw.search
        }

        val isDark = ThemeSwitchAnimator.isDarkMode(lottieView.context)
        applyTheme(lottieView, isDark)

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
                    applyTheme(lottieView, isDark)
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
}
