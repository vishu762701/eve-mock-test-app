package com.eve.app.ui.home

import android.content.Context
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.Constants
import com.google.android.material.chip.Chip

/** Native Home presentation shared by the live screen and rendering verification. */
object HomeAppearance {
    fun categories(values: List<String>): List<String> = listOf(Constants.CATEGORY_ALL) +
        values.distinct().sortedWith(compareBy<String> { it != "Other" }.thenBy { it })

    fun clipBanner(view: View) {
        val radius = 18f * view.resources.displayMetrics.density
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        view.clipToOutline = true
    }

    fun categoryChip(context: Context, category: String, selected: String, onSelect: () -> Unit): Chip =
        Chip(context).apply {
            text = category
            isCheckable = true
            isChecked = category == selected
            chipCornerRadius = 50f * resources.displayMetrics.density
            chipStrokeWidth = resources.displayMetrics.density
            chipStrokeColor = ContextCompat.getColorStateList(context, R.color.selector_category_chip_stroke)
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.selector_category_chip_bg)
            setTextColor(ContextCompat.getColorStateList(context, R.color.selector_category_chip_text))
            isCheckedIconVisible = false
            setOnClickListener { onSelect() }
        }
}
