package com.eve.app.ui.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionBulkImportOriginTest {
    private val origin = QuestionBulkImportOrigin("exam-A", "uid-A", "a@example.com")

    @Test fun requestNeedsOriginalExamAndAccount() {
        assertTrue(origin.isCurrent("exam-A", "uid-A", "A@example.com"))
        assertFalse(origin.isCurrent("exam-B", "uid-A", "a@example.com"))
        assertFalse(origin.isCurrent("exam-A", "uid-B", "a@example.com"))
        assertFalse(origin.isCurrent("exam-A", "uid-A", "b@example.com"))
        assertFalse(origin.isCurrent(null, "uid-A", "a@example.com"))
    }

    @Test fun incompleteIdentityCannotAuthorizeResult() {
        assertFalse(QuestionBulkImportOrigin("", "uid-A", "a@example.com")
            .isCurrent("", "uid-A", "a@example.com"))
        assertFalse(QuestionBulkImportOrigin("exam-A", "", "a@example.com")
            .isCurrent("exam-A", "", "a@example.com"))
    }
}
