package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** MainActivity is Firebase-bound, so this local test enforces saved-state wiring.
 * Real rotation/IME pixels must be exercised in the final native two-nav-mode suite.
 */
class HomeSearchRecreationContractTest {
    @Test fun searchPresentationIsRestoredAfterListenerSetupAndBeforeFirstHomeRender() {
        val source = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val setupCall = source.indexOf("        setupSearch()")
        val restoreCall = source.indexOf("        restoreSearchPresentation(savedInstanceState)")
        val firstRender = source.indexOf("            render(viewModel.state.value)")
        assertTrue(setupCall >= 0 && restoreCall > setupCall)
        assertTrue("Search must restore before first Home render", firstRender > restoreCall)
    }

    @Test fun activeQueryIsSavedFromRealEditTextAndRestoredWithoutMorphOrImeRestart() {
        val source = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val restore = source.substringAfter("private fun restoreSearchPresentation(savedInstanceState: Bundle?)")
            .substringBefore("private fun setupSearch()")
        val save = source.substringAfter("override fun onSaveInstanceState(outState: Bundle)")
            .substringBefore("private fun loadProfilePhoto(")
        assertTrue(save.contains("outState.putBoolean(\"home_search_active\", isSearchActive)"))
        assertTrue(save.contains("outState.putString(\"home_search_query\", binding.etSearch.text?.toString().orEmpty())"))
        assertTrue(restore.contains("savedInstanceState?.getBoolean(\"home_search_active\") != true"))
        assertTrue(restore.contains("binding.etSearch.setText(query)"))
        assertTrue(restore.contains("currentSearchQuery = query.trim()"))
        assertTrue(restore.contains("searchDebounceJob?.cancel()"))
        assertTrue(restore.contains("binding.normalTopBar.visibility = View.GONE"))
        assertTrue(restore.contains("binding.searchTopBar.visibility = View.VISIBLE"))
        assertTrue(restore.contains("binding.chipGroupCategory.visibility = View.GONE"))
        assertTrue(!restore.contains("showSoftInput("))
        assertTrue(!restore.contains("searchMorphAnimator?.start()"))
    }
}
