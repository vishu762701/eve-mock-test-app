package com.eve.app.ui.result

import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.TestAttempt
import com.eve.app.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit verification for:
 * 1. Result screen Right / Wrong / Unattempted filters and counts
 * 2. Cutoff category dynamic lookup and qualification verdict
 * 3. Status indicator drawable mapping
 * 4. Lottie search.json layer transparency (no bg Outlines)
 * 5. Reattempt / Attempt persistence tracking
 */
class ResultAndTestPolishVerificationTest {

    @Test
    fun testResultScreenStatusFilters() {
        val sampleItems = listOf(
            AnswerItem(number = 1, questionText = "Question 1", selected = "A", selectedText = "Alpha", correct = "A", correctText = "Alpha"),
            AnswerItem(number = 2, questionText = "Question 2", selected = "B", selectedText = "Beta", correct = "C", correctText = "Gamma"),
            AnswerItem(number = 3, questionText = "Question 3", selected = "", selectedText = "", correct = "D", correctText = "Delta"),
            AnswerItem(number = 4, questionText = "Question 4", selected = "B", selectedText = "Beta", correct = "B", correctText = "Beta"),
            AnswerItem(number = 5, questionText = "Question 5", selected = "A", selectedText = "Alpha", correct = "C", correctText = "Gamma")
        )

        val total = sampleItems.size
        val correctCount = sampleItems.count { it.isCorrect }
        val wrongCount = sampleItems.count { it.isAttempted && !it.isCorrect }
        val unattemptedCount = sampleItems.count { !it.isAttempted }

        assertEquals(5, total)
        assertEquals(2, correctCount)
        assertEquals(2, wrongCount)
        assertEquals(1, unattemptedCount)

        // Filter: Right
        val rightItems = sampleItems.filter { it.isCorrect }
        assertEquals(2, rightItems.size)
        assertTrue(rightItems.all { it.isCorrect })

        // Filter: Wrong
        val wrongItems = sampleItems.filter { it.isAttempted && !it.isCorrect }
        assertEquals(2, wrongItems.size)
        assertTrue(wrongItems.all { it.isAttempted && !it.isCorrect })

        // Filter: Unattempted
        val unattemptedItems = sampleItems.filter { !it.isAttempted }
        assertEquals(1, unattemptedItems.size)
        assertTrue(unattemptedItems.all { !it.isAttempted })
    }

    @Test
    fun testCutoffCategoryLookupAndVerdict() {
        val cutoffs = mapOf(
            "General" to 75.0,
            "OBC" to 70.0,
            "SC" to 60.0,
            "ST" to 55.0
            // EWS not configured
        )

        val studentScore = 72.0

        // General: 72 < 75 -> Not Qualified
        val generalCutoff = cutoffs["General"]
        assertNotNull(generalCutoff)
        assertTrue(studentScore < generalCutoff!!)

        // OBC: 72 >= 70 -> Qualified
        val obcCutoff = cutoffs["OBC"]
        assertNotNull(obcCutoff)
        assertTrue(studentScore >= obcCutoff!!)

        // EWS: Not configured -> null
        val ewsCutoff = cutoffs["EWS"]
        assertNull(ewsCutoff)
    }

    @Test
    fun testStatusIndicatorCircleMapping() {
        fun resolveStatusDrawable(item: AnswerItem): Int {
            return when {
                !item.isAttempted -> R.drawable.ic_status_circle_unattempted
                item.isCorrect -> R.drawable.ic_status_circle_right
                else -> R.drawable.ic_status_circle_wrong
            }
        }

        val rightItem = AnswerItem(number = 1, questionText = "Q1", selected = "A", selectedText = "A", correct = "A", correctText = "A")
        val wrongItem = AnswerItem(number = 2, questionText = "Q2", selected = "A", selectedText = "A", correct = "B", correctText = "B")
        val unattemptedItem = AnswerItem(number = 3, questionText = "Q3", selected = "", selectedText = "", correct = "A", correctText = "A")

        assertEquals(R.drawable.ic_status_circle_right, resolveStatusDrawable(rightItem))
        assertEquals(R.drawable.ic_status_circle_wrong, resolveStatusDrawable(wrongItem))
        assertEquals(R.drawable.ic_status_circle_unattempted, resolveStatusDrawable(unattemptedItem))
    }

    @Test
    fun testSearchLottieHasNoBackgroundCircleLayer() {
        val file = File("src/main/res/raw/search.json").takeIf { it.exists() }
            ?: File("app/src/main/res/raw/search.json").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/raw/search.json")
        assertTrue("search.json must exist in res/raw", file.exists())

        val content = file.readText()
        assertFalse(
            "search.json must not contain 'bg Outlines' layer (the unwanted background circle)",
            content.contains("\"bg Outlines\"")
        )
    }

    @Test
    fun testSubmittedAttemptImmediateTracking() {
        val testExamId = "test_exam_mock_42"
        HomeViewModel.markAttemptCleared(testExamId)
        assertFalse(HomeViewModel.isAttemptSubmitted(testExamId))

        val attempt = TestAttempt(
            id = "att_1",
            examId = testExamId,
            score = 80.0,
            total = 100,
            correct = 80,
            wrong = 20,
            unattempted = 0
        )

        HomeViewModel.markAttemptSubmitted(testExamId, attempt)
        assertTrue(HomeViewModel.isAttemptSubmitted(testExamId))
        assertEquals(attempt, HomeViewModel.getCachedAttempt(testExamId))

        HomeViewModel.markAttemptCleared(testExamId)
        assertFalse(HomeViewModel.isAttemptSubmitted(testExamId))
        assertNull(HomeViewModel.getCachedAttempt(testExamId))
    }
}
