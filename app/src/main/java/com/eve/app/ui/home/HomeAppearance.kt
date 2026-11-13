package com.eve.app.ui.home

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.designsystem.EveDesignSystem
import com.eve.app.util.Constants
import com.google.android.material.chip.Chip

/** Native Home presentation powered by EveDesignSystem foundation. */
object HomeAppearance {
    fun categories(values: List<String>): List<String> = listOf(Constants.CATEGORY_ALL) +
        values.filter { it != Constants.CATEGORY_ALL }.distinct()
            .sortedWith(compareBy<String> { it != "Other" }.thenBy { it })

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

    /** Reuse native chips; callbacks read the current authoritative selection. */
    fun renderCategoryChips(group: HomeCategoryChipGroup, categories: List<String>, selected: String,
                            currentSelection: () -> String, onSelect: (String) -> Unit) {
        fun refreshPalette() {
            for (index in 0 until group.childCount) {
                (group.getChildAt(index) as? Chip)?.let {
                    EveDesignSystem.Components.refreshCategoryChipPalette(it)
                }
            }
        }
        if (group.childCount == categories.size) {
            val same = (0 until group.childCount).all { i ->
                (group.getChildAt(i) as? Chip)?.text?.toString() == categories[i]
            }
            if (same) {
                val selectedChip = (0 until group.childCount)
                    .mapNotNull { group.getChildAt(it) as? Chip }
                    .firstOrNull { it.text.toString() == selected }
                if (selectedChip != null && group.checkedChipId != selectedChip.id) {
                    group.check(selectedChip.id)
                }
                refreshPalette()
                return
            }
        }
        group.removeAllViews()
        categories.forEach { category ->
            val chip = categoryChip(group.context, category, selected) {
                if (category != currentSelection()) {
                    onSelect(category)
                }
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
