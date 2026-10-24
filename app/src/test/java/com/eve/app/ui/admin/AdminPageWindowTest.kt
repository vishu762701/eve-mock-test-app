package com.eve.app.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPageWindowTest {
    @Test fun startsAndEndsAreBounded() {
        assertTrue(AdminPageWindow.pages(1, 0).isEmpty())
        assertEquals(listOf(1), AdminPageWindow.pages(1, 1).toList())
        assertEquals(listOf(1, 2, 3, 4, 5), AdminPageWindow.pages(1, 20).toList())
        assertEquals(listOf(8, 9, 10, 11, 12), AdminPageWindow.pages(10, 20).toList())
        assertEquals(listOf(16, 17, 18, 19, 20), AdminPageWindow.pages(20, 20).toList())
        assertEquals(listOf(1, 2, 3), AdminPageWindow.pages(-40, 3).toList())
        assertEquals(listOf(1, 2, 3), AdminPageWindow.pages(999, 3).toList())
    }
}
