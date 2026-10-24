package com.eve.app.ui.result

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultSectionTest {
    @Test fun tabsUseApprovedOrderAndSafeFallback() {
        assertEquals(ResultSection.OVERVIEW, ResultSection.fromTab(0))
        assertEquals(ResultSection.REVIEW, ResultSection.fromTab(1))
        assertEquals(ResultSection.LEADERBOARD, ResultSection.fromTab(2))
        assertEquals(ResultSection.OVERVIEW, ResultSection.fromTab(-1))
        assertEquals(ResultSection.OVERVIEW, ResultSection.fromTab(3))
    }
}
