package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test fun delegatedTouchesMustStayWithOneOwnerUntilUpOrCancel() {
        val src = File("src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt").readText()
        assertTrue("DOWN must select exactly one hit region",
            src.contains("delegates.firstOrNull { (bounds, _) -> bounds.contains(x, y) }"))
        assertTrue("MOVE must keep the active owner",
            src.contains("gestureOwner?.let { forward(it, event) } ?: false"))
        assertTrue("UP and CANCEL must reset gesture ownership",
            src.contains("MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL") &&
                src.contains("gestureOwner = null"))
        assertTrue("Never fan an event out to all chip delegates",
            !src.contains("delegates.any"))
    }

    @Test fun viewModelPersistsStateAndRenderChecksSingleChipAtomically() {
        val viewModel = File("src/main/java/com/eve/app/ui/home/HomeViewModel.kt").readText()
        val activity = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val appearance = File("src/main/java/com/eve/app/ui/home/HomeAppearance.kt").readText()
        assertTrue(viewModel.contains("SavedStateHandle"))
        assertTrue(viewModel.contains("savedStateHandle[SELECTED_CATEGORY_STATE_KEY] = category"))
        assertTrue(viewModel.contains("publishExamState(exams)"))
        assertTrue(activity.contains("group.check(selectedChip.id)"))
        assertTrue(appearance.contains("values.filter { it != Constants.CATEGORY_ALL }"))
    }
}
