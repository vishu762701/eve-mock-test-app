package com.eve.app.ui.home

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.designsystem.EveDesignSystem
import com.eve.app.designsystem.TelegramChip
import com.eve.app.util.Constants

/** Native Home presentation powered by EveDesignSystem foundation. */
object HomeAppearance {
    fun categories(values: List<String>, context: Context? = null): List<String> {
        val distinct = values.filter { it != Constants.CATEGORY_ALL }.distinct()
        val savedOrder = context?.let { CategoryOrderManager.getSavedOrder(it) } ?: emptyList()
        val ordered = if (savedOrder.isNotEmpty()) {
            val orderedList = savedOrder.filter { distinct.contains(it) }.toMutableList()
            for (cat in distinct) {
                if (!orderedList.contains(cat)) orderedList.add(cat)
            }
            orderedList
        } else {
            distinct.sortedWith(compareBy<String> { it != "Other" }.thenBy { it })
        }
        return listOf(Constants.CATEGORY_ALL) + ordered
    }

    fun tintNotificationBell(view: com.airbnb.lottie.LottieAnimationView) {
        EveDesignSystem.Components.tintNotificationBell(view)
    }

    fun clipBanner(view: View) {
        EveDesignSystem.Shapes.applyCardSquircle(view)
    }

    fun categoryChip(
        context: Context,
        category: String,
        selected: String,
        count: Int = 0,
        onSelect: () -> Unit
    ): TelegramChip =
        EveDesignSystem.Components.createCategoryChip(
            context = context,
            category = category,
            isSelected = category == selected,
            count = count,
            onSelect = onSelect
        )

    /** Reuse native chips; callbacks read the current authoritative selection. */
    fun renderCategoryChips(
        group: HomeCategoryChipGroup,
        categories: List<String>,
        selected: String,
        currentSelection: () -> String,
        onSelect: (String) -> Unit
    ) {
        renderCategoryChips(group, categories, selected, emptyMap(), null, currentSelection, onSelect)
    }

    fun renderCategoryChips(
        group: HomeCategoryChipGroup,
        categories: List<String>,
        selected: String,
        categoryCounts: Map<String, Int> = emptyMap(),
        onLongClick: ((String) -> Boolean)? = null,
        currentSelection: () -> String,
        onSelect: (String) -> Unit
    ) {
        fun refreshPalette() {
            for (index in 0 until group.childCount) {
                (group.getChildAt(index) as? TelegramChip)?.let {
                    EveDesignSystem.Components.refreshCategoryChipPalette(it)
                }
            }
        }
        if (group.childCount == categories.size) {
            val same = (0 until group.childCount).all { i ->
                val chip = group.getChildAt(i) as? TelegramChip
                chip?.text?.toString()?.startsWith(categories[i]) == true
            }
            if (same) {
                val selectedChip = (0 until group.childCount)
                    .mapNotNull { group.getChildAt(it) as? TelegramChip }
                    .firstOrNull { it.text.toString().startsWith(selected) }
                if (selectedChip != null && group.checkedChipId != selectedChip.id) {
                    group.check(selectedChip.id)
                }
                refreshPalette()
                return
            }
        }
        group.removeAllViews()
        categories.forEach { category ->
            val count = categoryCounts[category] ?: 0
            val chip = categoryChip(group.context, category, selected, count) {
                if (category != currentSelection()) {
                    onSelect(category)
                }
            }
            if (onLongClick != null) {
                chip.setOnLongClickListener { onLongClick(category) }
            }
            group.addView(chip)
        }
    }

    fun applyTopBarDepth(topBarContainer: View, searchTopBar: View? = null) {
        EveDesignSystem.Components.applyHomeTopBarDepth(topBarContainer, searchTopBar)
        val root = topBarContainer.rootView
        val rvExams = root?.findViewById<RecyclerView>(R.id.rvExams)
        val titleView = topBarContainer.findViewById<View>(R.id.tvActionBarTitle)
        if (rvExams != null && rvExams.getTag(R.id.rvExams) == null) {
            rvExams.setTag(R.id.rvExams, true)
            TelegramDialogsListHelper.install(rvExams, titleView)
        }
    }

    fun applyCard(card: com.google.android.material.card.MaterialCardView) {
        EveDesignSystem.Components.applyHomeCard(card)
    }
}
