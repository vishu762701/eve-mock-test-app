package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class HomeCategorySelectionTest {
    private val all = "All"
    private val categories = listOf(all, "Other", "History")

    @Test fun repeatSelectedAllTenTimesNeverClearsSelection() {
        var selected = all
        repeat(10) {
            selected = HomeCategorySelection.select(selected, all, categories)
            assertEquals(all, selected)
        }
    }

    @Test fun otherAndAllSwitchWithoutEmptyIntermediateState() {
        var selected = all
        repeat(10) {
            selected = HomeCategorySelection.select(selected, "Other", categories)
            assertEquals("Other", selected)
            selected = HomeCategorySelection.select(selected, all, categories)
            assertEquals(all, selected)
        }
    }

    @Test fun disappearedCategoryFallsBackToAllAndStaysThereOnReappearance() {
        val removed = listOf(all, "History")
        val selected = HomeCategorySelection.resolve("Other", removed, all)
        assertEquals(all, selected)
        assertEquals(all, HomeCategorySelection.resolve(selected, categories, all))
    }

    @Test fun unknownCategoryTapDoesNotChangeSelectedFilter() {
        assertEquals("Other", HomeCategorySelection.select("Other", "Unknown", categories))
    }

    @Test fun viewModelPersistsStateAndRenderChecksSingleChipAtomically() {
        val viewModel = File("src/main/java/com/eve/app/ui/home/HomeViewModel.kt").readText()
        val activity = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val appearance = File("src/main/java/com/eve/app/ui/home/HomeAppearance.kt").readText()
        assert(viewModel.contains("SavedStateHandle"))
        assert(viewModel.contains("savedStateHandle[SELECTED_CATEGORY_STATE_KEY] = category"))
        assert(viewModel.contains("publishExamState(exams)"))
        assert(activity.contains("group.check(selectedChip.id)"))
        assert(appearance.contains("values.filter { it != Constants.CATEGORY_ALL }"))
    }
}
