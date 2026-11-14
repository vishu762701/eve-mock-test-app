/*
 * Derived from Telegram Android FiltersView.java, FilterTabsView.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.chip.ChipGroup

/**
 * Drop-in Telegram chip and pill group replacing Material ChipGroup app-wide.
 *
 * Supports both multi-row flow layout and single-line horizontal scroll layout.
 * Enforces single/multi selection and Telegram pill spacing (8dp).
 */
open class TelegramChipGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipGroupStyle
) : ChipGroup(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    init {
        chipSpacingHorizontal = dp(8f)
        chipSpacingVertical = dp(8f)
    }

    fun addChip(
        chipText: String,
        chipId: Int = View.generateViewId(),
        checked: Boolean = false
    ): TelegramChip {
        val chip = TelegramChip(context).apply {
            id = chipId
            text = chipText
            isChecked = checked
        }
        addView(chip)
        return chip
    }

    fun selectChipByText(text: String) {
        for (i in 0 until childCount) {
            val chip = getChildAt(i) as? android.widget.TextView ?: continue
            if (chip.text.toString().equals(text, ignoreCase = true)) {
                check(chip.id)
                break
            }
        }
    }
}
