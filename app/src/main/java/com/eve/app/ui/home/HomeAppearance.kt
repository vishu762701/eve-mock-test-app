package com.eve.app.ui.home

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.Constants
import com.google.android.material.chip.Chip

/** Native Home presentation shared by the live screen and rendering verification. */
object HomeAppearance {
    fun categories(values: List<String>): List<String> = listOf(Constants.CATEGORY_ALL) +
        values.distinct().sortedWith(compareBy<String> { it != "Other" }.thenBy { it })

    fun clipBanner(view: View) {
        val radius = 16f * view.resources.displayMetrics.density
        com.eve.app.util.SquircleHelper.applySquircleOutline(view, radius)
    }

    fun categoryChip(context: Context, category: String, selected: String, onSelect: () -> Unit): Chip =
        Chip(context).apply {
            text = category
            isCheckable = true
            isChecked = category == selected
            val density = resources.displayMetrics.density
            // Categories are content-sized pills with a separate 48dp delegated touch target.
            chipStrokeWidth = 0f
            chipStrokeColor = ContextCompat.getColorStateList(context, R.color.eve_shape_border)
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.selector_recovery_chip_bg)
            setTextColor(ContextCompat.getColorStateList(context, R.color.selector_recovery_chip_text))
            isCheckedIconVisible = false
            setEnsureMinTouchTargetSize(false) // parent delegates a 48dp touch region
            textSize = 13f
            chipStartPadding = 12f * density
            chipEndPadding = 12f * density
            textStartPadding = 0f
            textEndPadding = 0f
            setPaddingRelative(paddingStart, Math.round(8f * density), paddingEnd, Math.round(8f * density))
            chipMinHeight = maxOf(40f * density, paint.fontMetrics.run { bottom - top } + 16f * density)
            chipCornerRadius = chipMinHeight / 2f
            minWidth = Math.round(64f * density)
            minHeight = Math.round(chipMinHeight)
            maxWidth = Math.round(maxOf(64, resources.configuration.screenWidthDp - 32) * density)
            ellipsize = android.text.TextUtils.TruncateAt.END
            contentDescription = category
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setOnClickListener { onSelect() }
        }
}
