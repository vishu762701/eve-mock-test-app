package com.eve.app.ui.profile

/**
 * EVE full-window integration of Telegram ProfileGooeyView's camera mask target.
 * The targetY for circular notches comes from ProfileGooeyView.java:397-405.
 * Movement relative to EVE's separate scroll hierarchy is ADAPTED.
 */
internal object ProfileAvatarTrajectory {
    fun offsetToNotch(startCenterY: Float, cutoutCenterY: Float, progress: Float): Float =
        (cutoutCenterY - startCenterY) * progress
}
