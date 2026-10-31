package com.eve.app.ui.profile

import android.widget.LinearLayout
import kotlin.math.roundToInt

/**
 * Numeric collapse stages from Telegram Components/ProfileActionsView.java:247-337.
 *
 * Telegram's currentHeight comes from ProfileActivity.java:11253-11270. EVE's
 * scroll-view origin is different, so mapping scrollY to currentHeight is
 * explicitly ADAPTED. The 74dp/20dp/54dp and two alpha stages are source-exact.
 */
internal object ProfileActionsCollapse {
    data class State(
        val fraction: Float,
        val backgroundAlpha: Float,
        val contentAlpha: Float,
        val touchEnabled: Boolean
    )

    fun atScroll(scrollY: Float, density: Float): State {
        require(density > 0f)
        val totalHeight = 74f * density
        val currentHeight = (totalHeight - scrollY).coerceIn(0f, totalHeight)
        // Source targetHeight is 74dp - 8dp top - 12dp bottom = 54dp.
        val visibleContentHeight = (currentHeight - 20f * density).coerceAtLeast(0f)
        val fraction = (visibleContentHeight / (54f * density)).coerceIn(0f, 1f)
        val backgroundAlpha = ((fraction - 0.2f) / 0.8f).coerceIn(0f, 1f)
        val contentAlpha = ((fraction - 0.4f) / 0.6f).coerceIn(0f, 1f)
        return State(fraction, backgroundAlpha, contentAlpha, currentHeight >= 8f * density)
    }

    fun applyTo(buttons: List<LinearLayout>, state: State) {
        val backgroundAlphaByte = (state.backgroundAlpha * 255f).roundToInt()
        for (button in buttons) {
            val drawable = button.background?.mutate()
            if (drawable != null && drawable.alpha != backgroundAlphaByte) {
                drawable.alpha = backgroundAlphaByte
            }
            button.isEnabled = state.touchEnabled
            for (index in 0 until button.childCount) {
                val child = button.getChildAt(index)
                child.alpha = state.contentAlpha
                child.scaleX = state.fraction
                child.scaleY = state.fraction
            }
        }
    }
}
