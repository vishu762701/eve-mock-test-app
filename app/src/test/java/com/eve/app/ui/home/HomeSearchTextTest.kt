package com.eve.app.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class HomeSearchTextTest {
    @Test fun findsCategoryOrExamTitleRegardlessOfCase() {
        assertTrue(HomeSearchText.matches("history", "Rajasthan History", "GK"))
        assertTrue(HomeSearchText.matches("GK", "Teacher Test", "GK"))
        assertFalse(HomeSearchText.matches("geography", "History", "Polity"))
    }

    @Test fun supportsDevanagariAndPublishedFeedbackFields() {
        assertTrue(HomeSearchText.matches("राजस्थान", "राजस्थान इतिहास", "History"))
        assertTrue(HomeSearchText.matches("new schedule", "Announcement", "Our new schedule is published"))
        assertFalse(HomeSearchText.matches("  ", "Anything", "General"))
    }

    @Test fun independentOfDeviceTurkishCaseLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertTrue(HomeSearchText.matches("india", "INDIA POLITY", "GK"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun searchReadsUnfilteredRowsButLeavesHomeCategoryListUntouched() {
        val vm = File("src/main/java/com/eve/app/ui/home/HomeViewModel.kt").readText()
        val activity = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val model = File("src/main/java/com/eve/app/ui/home/HomeUiModels.kt").readText()
        assertTrue("Search data must originate before category filtering",
            vm.contains("mainExams.forEach { exam ->") && vm.contains("HomeUiData(categories, effectiveSelected, items, searchItems)"))
        assertTrue("Search must read global items instead of filtered Home presentation",
            activity.contains("lastSearchItems = data.searchItems") &&
                activity.contains("val filtered = lastSearchItems.filter"))
        assertTrue("Keep selected category for normal Home list",
            model.contains("val searchItems: List<HomeListItem> = items") &&
                activity.contains("adapter.submit(lastLoadedItems)"))
    }
}
