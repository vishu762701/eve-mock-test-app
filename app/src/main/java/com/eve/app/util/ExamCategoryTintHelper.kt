package com.eve.app.util

import androidx.annotation.ColorRes
import com.eve.app.R
import kotlin.math.abs

/**
 * Task B: Curated, harmonious category tint palette.
 * Maps exam categories to 6 distinct, palette-derived harmonious tints
 * to give the home exam list scanned visual variety in both light and dark mode.
 */
object ExamCategoryTintHelper {

    data class TintColors(
        @ColorRes val bgRes: Int,
        @ColorRes val fgRes: Int
    )

    private val PALETTE = listOf(
        TintColors(R.color.eve_category_tint_lilac_bg, R.color.eve_category_tint_lilac_fg),
        TintColors(R.color.eve_category_tint_green_bg, R.color.eve_category_tint_green_fg),
        TintColors(R.color.eve_category_tint_amber_bg, R.color.eve_category_tint_amber_fg),
        TintColors(R.color.eve_category_tint_rose_bg, R.color.eve_category_tint_rose_fg),
        TintColors(R.color.eve_category_tint_blue_bg, R.color.eve_category_tint_blue_fg),
        TintColors(R.color.eve_category_tint_teal_bg, R.color.eve_category_tint_teal_fg)
    )

    fun getTintForCategory(category: String?): TintColors {
        if (category.isNullOrBlank()) {
            return PALETTE[0]
        }
        val clean = category.trim().lowercase()
        // Semantic keyword overrides if relevant
        return when {
            clean.contains("railway") || clean.contains("rrb") -> PALETTE[4] // Blue
            clean.contains("ssc") || clean.contains("cgl") -> PALETTE[0] // Lilac
            clean.contains("defence") || clean.contains("nda") || clean.contains("cds") -> PALETTE[1] // Green
            clean.contains("bank") || clean.contains("ibps") || clean.contains("sbi") -> PALETTE[2] // Amber
            clean.contains("teaching") || clean.contains("ctet") || clean.contains("state") -> PALETTE[3] // Rose
            clean.contains("upsc") || clean.contains("civil") -> PALETTE[5] // Teal
            else -> {
                val index = abs(clean.hashCode()) % PALETTE.size
                PALETTE[index]
            }
        }
    }
}
