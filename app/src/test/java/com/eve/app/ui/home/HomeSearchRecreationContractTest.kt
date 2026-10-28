package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
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

    @Test fun telegramSourceSearchFadePreservesKeyboardAndCategoryRestoration() {
        val code = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
            .substringAfter("private fun startSearchAlphaTransition(")
            .substringBefore("private fun applyCurrentList()")
        assertTrue(code.contains("lp.width = 0") && code.contains("lp.weight = 1f"))
        assertTrue(code.contains("imm?.showSoftInput("))
        assertTrue(code.contains("imm?.hideSoftInputFromWindow("))
        assertTrue(code.contains("binding.etSearch.text?.clear()"))
        assertTrue("Telegram source clears keyboard focus during collapse",
            code.contains("binding.etSearch.clearFocus()"))
        assertTrue("Expand still restores keyboard focus for typing",
            code.contains("binding.etSearch.requestFocus()"))
        assertTrue(code.contains("binding.chipGroupCategory.visibility = View.GONE"))
        assertTrue(code.contains("binding.chipGroupCategory.visibility = View.VISIBLE"))
    }

    @Test fun searchBarUsesIntrinsicHeightWithinWrapContentHeader() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/layout/activity_main.xml"))
        val elements = document.getElementsByTagName("*")
        fun byId(id: String): Element = (0 until elements.length)
            .map { elements.item(it) as Element }
            .first { it.getAttribute("android:id") == "@+id/$id" }
        val parent = byId("topBarContainer")
        val search = byId("searchTopBar")
        assertEquals("wrap_content", parent.getAttribute("android:layout_height"))
        assertEquals("wrap_content", search.getAttribute("android:layout_height"))
        assertEquals("match_parent", search.getAttribute("android:layout_width"))
        assertEquals("gone", search.getAttribute("android:visibility"))
        assertEquals("@dimen/design_tab_height", search.getAttribute("android:minHeight"))
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
