/*
 * Derived from Telegram Android FiltersView.java, FilterTabsView.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.eve.app.R
import com.google.android.material.chip.ChipGroup

/**
 * Authentic Telegram-style chip and pill group.
 * Derived from Telegram Android FiltersView.java:697-760, FilterTabsView.java:176-178.
 * Extends ChipGroup for full framework and test compatibility.
 */
open class TelegramChipGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipGroupStyle
) : ChipGroup(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

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
        if (checked) {
            check(chipId)
        }
        return chip
    }

    fun selectChipByText(text: String) {
        for (i in 0 until childCount) {
            val chip = getChildAt(i) as? TextView ?: continue
            if (chip.text.toString().equals(text, ignoreCase = true)) {
                check(chip.id)
                break
            }
        }
    }

    fun findChipByText(text: String): TelegramChip? {
        for (i in 0 until childCount) {
            val chip = getChildAt(i) as? TelegramChip ?: continue
            if (chip.text.toString().equals(text, ignoreCase = true)) {
                return chip
            }
        }
        return null
    }

    fun getSelectedChipText(): String? {
        val id = checkedChipId
        if (id == View.NO_ID) return null
        return (findViewById<View>(id) as? TextView)?.text?.toString()
    }

    fun refreshPillAppearances() {
        for (i in 0 until childCount) {
            val chip = getChildAt(i) as? TelegramChip ?: continue
            chip.updateAppearance()
        }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (child.id == View.NO_ID) {
            child.id = View.generateViewId()
        }
        if (child is TelegramChip) {
            child.updateAppearance()
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        refreshPillAppearances()
    }
}
