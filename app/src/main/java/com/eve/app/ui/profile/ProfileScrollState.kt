package com.eve.app.ui.profile

import android.view.View

/** EVE NestedScrollView integration, source profile avatar collapse range: 100dp. */
internal object ProfileScrollState {
    /**
     * Source: DrKLO/Telegram@3de37c8, ProfileActivity.java:6053.
     * At complete collapse the gooey drawing host is GONE (not merely
     * non-animating); reopening or reverse scrolling must restore it.
     */
    fun applyGooeyVisibility(view: View, progress: Float) {
        require(progress.isFinite() && progress >= 0f)
        val target = if (progress >= 1f) View.GONE else View.VISIBLE
        if (view.visibility != target) view.visibility = target
    }

    fun progress(scrollY: Int, density: Float): Float {
        require(density > 0f)
        return (scrollY.toFloat() / (100f * density)).coerceIn(0f, 1f)
    }
}
