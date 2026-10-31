package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileActionsCollapseTest {
    @Test fun fullExpansionUsesOriginalTelegramSourceAlphaStages() {
        val state = ProfileActionsCollapse.atScroll(scrollY = 0f, density = 2f)
        assertEquals(1f, state.fraction, 0.0001f)
        assertEquals(1f, state.backgroundAlpha, 0.0001f)
        assertEquals(1f, state.contentAlpha, 0.0001f)
        assertTrue(state.touchEnabled)
    }

    @Test fun midCollapseUsesIndependentBackgroundAndContentThresholds() {
        val state = ProfileActionsCollapse.atScroll(scrollY = 40f, density = 2f)
        val fraction = 34f / 54f
        assertEquals(fraction, state.fraction, 0.0001f)
        assertEquals((fraction - 0.2f) / 0.8f, state.backgroundAlpha, 0.0001f)
        assertEquals((fraction - 0.4f) / 0.6f, state.contentAlpha, 0.0001f)
        assertTrue(state.backgroundAlpha > state.contentAlpha)
    }

    @Test fun terminalCollapseDisablesTouchAndDoesNotReappearUnderOverscroll() {
        val state = ProfileActionsCollapse.atScroll(scrollY = 148f, density = 2f)
        assertEquals(0f, state.fraction, 0.0001f)
        assertEquals(0f, state.backgroundAlpha, 0.0001f)
        assertEquals(0f, state.contentAlpha, 0.0001f)
        assertFalse(state.touchEnabled)
        assertEquals(state, ProfileActionsCollapse.atScroll(scrollY = 300f, density = 2f))
    }

    @Test fun negativeOverscrollLeavesActionViewExpandedAndClickable() {
        assertEquals(
            ProfileActionsCollapse.atScroll(0f, 3f),
            ProfileActionsCollapse.atScroll(-50f, 3f)
        )
    }

    @Test fun profileNoLongerUsesSingleLinearAlphaForSourceActions() {
        val src = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val scroll = src.substringAfter("private fun setupScrollAndGooey()")
            .substringBefore("private fun setupActions()")
        assertTrue(scroll.contains("ProfileActionsCollapse.applyTo("))
        assertTrue(scroll.contains("ProfileActionsCollapse.atScroll(scrollY.toFloat(), density)"))
        // The same source-fidelity alpha contract must now run for live gestures
        // and restored scroll after layout, not only for an initial button-only post.
        assertTrue(scroll.contains("binding.profileScrollView.doOnLayout {"))
        assertTrue(scroll.contains("renderProfileScrollState(scrollY, density)"))
        assertTrue(scroll.contains("renderProfileScrollState(binding.profileScrollView.scrollY, density)"))
        assertTrue(scroll.contains("private fun renderProfileScrollState("))
        assertFalse(scroll.contains("binding.layoutActions.alpha = headerContentAlpha"))
    }
}
