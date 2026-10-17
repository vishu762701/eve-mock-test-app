package com.eve.app.ui.home

import android.content.Context
import android.view.View
import com.eve.app.designsystem.EveDesignSystem
import com.eve.app.util.Constants
import com.google.android.material.chip.Chip

/** Native Home presentation powered by EveDesignSystem foundation. */
object HomeAppearance {
    fun categories(values: List<String>): List<String> = listOf(Constants.CATEGORY_ALL) +
        values.distinct().sortedWith(compareBy<String> { it != "Other" }.thenBy { it })

    fun tintNotificationBell(view: com.airbnb.lottie.LottieAnimationView) {
        EveDesignSystem.Components.tintNotificationBell(view)
    }

    fun clipBanner(view: View) {
        EveDesignSystem.Shapes.applyCardSquircle(view)
    }

    fun categoryChip(context: Context, category: String, selected: String, onSelect: () -> Unit): Chip =
        EveDesignSystem.Components.createCategoryChip(
            context = context,
            category = category,
            isSelected = category == selected,
            onSelect = onSelect
        )

    fun applyTopBarDepth(topBarContainer: View, searchTopBar: View? = null) {
        val context = topBarContainer.context
        val hasBlur = com.eve.app.util.EveLiquidGlass.isCrossWindowBlurSupported(context)
        val topBarBg = androidx.core.content.ContextCompat.getColor(
            context,
            if (hasBlur) com.eve.app.R.color.eve_home_blur_tint else com.eve.app.R.color.eve_home_solid_tint
        )
        topBarContainer.setBackgroundColor(topBarBg)
        searchTopBar?.setBackgroundColor(topBarBg)
    }
}
