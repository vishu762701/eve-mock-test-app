package com.eve.app.ui.profile

/**
 * Exact numeric interpolation from Telegram AndroidUtilities.lerp/ilerp as used by
 * ProfileGooeyView.java:345 and :367. Rendering and device motion require separate tests.
 */
internal object ProfileGooeyMotionMath {
    fun imageAlphaUnclamped(progress: Float): Int =
        ((1f - (progress - 0.5f) / 0.5f) * 255f).toInt()

    /** Telegram lerp(0f, peak, 0f, 0.5f, progress): rises, then returns to zero. */
    fun liquidBridgeHeight(progress: Float, peak: Float): Float =
        if (progress < 0.5f) peak * (progress / 0.5f)
        else peak * ((1f - progress) / 0.5f)
}
