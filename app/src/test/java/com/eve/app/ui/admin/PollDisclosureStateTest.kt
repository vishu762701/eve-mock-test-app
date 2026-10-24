package com.eve.app.ui.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PollDisclosureStateTest {
    @Test fun independentExpansionUsesStablePollIdsAndSurvivesRefresh() {
        val state = PollDisclosureState()
        assertTrue(state.toggle("poll-one"))
        assertTrue(state.toggle("poll-two"))
        assertTrue(state.isExpanded("poll-one"))
        assertTrue(state.isExpanded("poll-two"))
        state.retain(listOf("poll-two", "poll-one", "poll-three"))
        assertTrue(state.isExpanded("poll-one"))
        assertTrue(state.isExpanded("poll-two"))
        assertFalse(state.isExpanded("poll-three"))
    }

    @Test fun collapsingOnePollAndRemovingAnotherNeverTransfersExpansion() {
        val state = PollDisclosureState()
        state.toggle("alpha")
        state.toggle("beta")
        assertFalse(state.toggle("alpha"))
        assertFalse(state.isExpanded("alpha"))
        assertTrue(state.isExpanded("beta"))
        state.retain(listOf("alpha", "gamma"))
        assertFalse(state.isExpanded("beta"))
        assertFalse(state.isExpanded("gamma"))
        assertTrue(state.toggle("gamma"))
    }
}
