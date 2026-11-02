package com.eve.app.ui.home

import android.app.Application
import android.view.ContextThemeWrapper
import com.eve.app.R
import com.google.android.material.chip.Chip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Execute the exact renderer and native Chip callbacks used by MainActivity. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HomeReusedFilterCallbackTest {
    private fun exercise(largeFont: Boolean = false) {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve)
        if (largeFont) context.applyOverrideConfiguration(android.content.res.Configuration().apply { fontScale = 2f })
        val group = HomeCategoryChipGroup(context).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        var categories = listOf("All", "Other", "SSC")
        var selected = "All"
        val events = mutableListOf<String>()
        fun render() = HomeAppearance.renderCategoryChips(group, categories, selected, { selected }) {
            events += it
            selected = it
        }
        fun chip(name: String) = (0 until group.childCount).map { group.getChildAt(it) as Chip }
            .single { it.text.toString() == name }
        fun click(name: String) {
            val previous = selected
            val count = events.size
            chip(name).performClick()
            render()
            assertEquals(name, selected)
            assertEquals(if (previous == name) count else count + 1, events.size)
            assertEquals(1, group.checkedChipIds.size)
            assertTrue(chip(name).isChecked)
        }
        render()
        val originalAll = chip("All")
        fun measure() {
            group.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
            group.layout(0,0,group.measuredWidth,group.measuredHeight)
        }
        measure()
        val widths = categories.associateWith { chip(it).measuredWidth }
        repeat(3) {
            click("Other"); click("Other"); click("All"); click("All"); click("SSC"); click("All")
        }
        assertSame("Refresh must reuse existing native chips", originalAll, chip("All"))
        measure()
        for (name in categories) assertEquals("Selection must not change chip width", widths[name], chip(name).measuredWidth)
        categories = listOf("SSC", "All", "Other")
        render()
        click("Other"); click("All")
        // Recreate the presentation while retaining the authoritative selection.
        group.removeAllViews()
        render()
        click("SSC")
        categories = listOf("All", "Other")
        selected = HomeCategorySelection.resolve(selected, categories, "All")
        render()
        assertTrue(chip("All").isChecked)
        click("All"); click("Other"); click("All")
    }
    @Test fun reusedProductionCallbacksFollowRepeatedAndReorderedSelections() = exercise()
    @Test @Config(qualifiers = "night")
    fun nightCallbacksKeepOneSelection() = exercise()
    @Test @Config(qualifiers = "hi-rIN-ldrtl")
    fun hindiRtlLargeFontCallbacksKeepOneSelection() = exercise(true)
}
