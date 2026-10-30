package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeSearchHistoryMission11Test {
    @Test fun effectiveSearchIsNormalizedWithoutChangingDevanagari() {
        assertEquals("हिंदी परीक्षा", HomeSearchHistory.normalize("  हिंदी    परीक्षा  "))
        assertEquals("", HomeSearchHistory.normalize("   "))
    }

    @Test fun searchIsNotPopulatedWithHomeContentWhenQueryEmpty() {
        val code = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val method = code.substringAfter("private fun applyCurrentList()")
            .substringBefore("private fun showPinPopupMenu(")
        val emptyBranch = method.substringBefore("if (isSearchActive && currentSearchQuery.isNotEmpty())")
        assertTrue(emptyBranch.contains("if (isSearchActive && currentSearchQuery.isEmpty())"))
        assertTrue(emptyBranch.contains("adapter.submit(emptyList())"))
        assertTrue(emptyBranch.contains("binding.rvExams.visibility = View.GONE"))
        assertTrue(emptyBranch.contains("return"))
    }

    @Test fun closingSearchRestoresAdminOnlyForTheAuthorizedAccount() {
        val activity = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val chrome = activity.substringAfter("private fun refreshSearchChrome()")
            .substringBefore("private fun setupSearch()")
        assertTrue("Search must hide the admin-only surface without changing authorization",
            chrome.contains("if (!isSearchActive && isAdminUser) View.VISIBLE else View.GONE"))
        assertTrue("Opening and closing must both refresh the chrome",
            activity.substringAfter("private fun openSearch()")
                .substringBefore("private fun closeSearch()").contains("refreshSearchChrome()"))
        assertTrue("Closing search must restore admin and banner presentation",
            activity.substringAfter("private fun closeSearch()")
                .substringBefore("private fun applyCurrentList()").contains("refreshSearchChrome()"))
    }

    @Test fun onlyExplicitActionsPersistAndClearIsUidScoped() {
        val activity = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val history = File("src/main/java/com/eve/app/ui/home/HomeSearchHistory.kt").readText()
        assertTrue(activity.contains("setOnEditorActionListener"))
        assertTrue(activity.contains("HomeSearchHistory.record(this, FirebaseAuth"))
        assertTrue(activity.contains("HomeSearchHistory.clear(this, uid)"))
        assertTrue(history.contains("if (uid.isBlank()) return"))
        assertTrue(history.contains("MAX_ITEMS = 8"))
        assertTrue(history.contains(".remove(\"recent_$" + "uid\")"))
        val watcher = activity.substringAfter("binding.etSearch.addTextChangedListener(")
            .substringBefore("private fun cancelSearchVisualAnimations")
        assertTrue(!watcher.contains("HomeSearchHistory.record("))
    }
}
