package com.eve.app.ui.profile

/**
 * EVE full-window integration of Telegram ProfileGooeyView's camera mask target.
 * The targetY for circular notches comes from ProfileGooeyView.java:397-405.
 * Movement relative to EVE's separate scroll hierarchy is ADAPTED.
 */
internal object ProfileAvatarTrajectory {
    /**
     * EXACT numeric mapping for ProfileActivity.java:8735-8740 (my profile,
     * no open animation): avatarScale = lerp(intoSize, 96, diff) / 100,
     * intoSize = half circular notch width in dp or 24dp without a circle.
     * EVE scroll progress maps to original (1 - diff); parent layout remains ADAPTED.
     */
    fun sourceAvatarScale(progress: Float, circularNotchWidthPx: Float?, density: Float): Float {
        require(density > 0f && density.isFinite() && progress.isFinite())
        val intoSizeDp = circularNotchWidthPx?.takeIf { it.isFinite() && it > 0f }
            ?.let { 0.5f * it / density } ?: 24f
        val diff = 1f - progress.coerceIn(0f, 1f)
        return (intoSizeDp + (96f - intoSizeDp) * diff) / 100f
    }

    fun offsetToNotch(startCenterY: Float, cutoutCenterY: Float, progress: Float): Float =
        (cutoutCenterY - startCenterY) * progress
}
