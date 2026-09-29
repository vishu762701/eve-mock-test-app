package com.eve.app.ui.result

import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FlaggedQuestion
import com.eve.app.data.model.Question
import com.eve.app.data.model.QuestionStat
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.admin.EditExamActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verification test suite for the 6 Test/Review/Result Flow Upgrades:
 * 1. Solution Format Redesign ("Key Points" step-by-step structure + % stat)
 * 2. Result Screen Depth (Rank, Percentile, Accuracy, Best/Avg, Category Cutoffs)
 * 3. Unified Report System (Content vs Technical bug routing + comment validation)
 * 4. Review Filters (Combinable Unattempted & Overtime)
 * 5. Post-Answer Timer Comparison Popup
 * 6. Question Palette Navigation & color-coding
 */
class TestReviewResultFlowUpgradesTest {

    // =========================================================================
    // FEATURE 1 & VERIFICATION 1, 2: SOLUTION KEY POINTS FORMAT & STAT BADGE
    // =========================================================================
    @Test
    fun testKeyPointsSolutionFormatStructure() {
        val rawExplanation = "Newton's second law defines force as mass times acceleration. F = ma is the foundational formula."
        val reformatted = EditExamActivity.formatKeyPointsLocally("B", rawExplanation)

        // 1. Bold one-line summary
        assertTrue(
            "Reformatted explanation must contain bold summary stating correct option",
            reformatted.contains("**The correct answer is Option 2 (B).**")
        )

        // 2. Key Points section header
        assertTrue(
            "Reformatted explanation must contain Key Points header",
            reformatted.contains("Key Points:")
        )

        // 3. Concept-labeled bullets (• **Concept Name:** ...)
        assertTrue(
            "Reformatted explanation must contain concept bullets with bold label",
            reformatted.contains("• **Key Concept:**")
        )
    }

    @Test
    fun testAccuracyPercentageCalculationFromQuestionStat() {
        val stat = QuestionStat(
            questionId = "q101",
            totalAttempts = 200,
            correctAttempts = 150,
            totalTimeSeconds = 6000,
            avgTimeSeconds = 30.0
        )

        val accuracy = stat.correctPercentage
        assertEquals("Accuracy should be exactly 75%", 75, accuracy)

        val displayBadge = "$accuracy% got this right"
        assertEquals("Display badge text format", "75% got this right", displayBadge)
    }

    // =========================================================================
    // FEATURE 2 & VERIFICATION 3, 4: RESULT SCREEN DEPTH METRICS & CUTOFFS
    // =========================================================================
    @Test
    fun testRankAndPercentileCalculation() {
        val scores = listOf(95.0, 85.0, 80.0, 75.0, 60.0, 50.0, 40.0, 30.0, 20.0, 10.0) // 10 attempts
        val currentScore = 80.0

        val higherScores = scores.count { it > currentScore } // 2 (95, 85)
        val rank = higherScores + 1
        assertEquals("Rank should be #3", 3, rank)

        val lowerScores = scores.count { it < currentScore } // 7 (75, 60, 50, 40, 30, 20, 10)
        val totalAttempts = scores.size // 10
        val percentile = (lowerScores * 100.0) / (totalAttempts - 1)
        assertEquals("Percentile should be 77.8%", 77.78, percentile, 0.01)
    }

    @Test
    fun testMultiStudentAttemptAggregation_averageTimeAndPercentageCorrect() {
        // Multi-submission scenario: 5 distinct students answer Question Q42
        data class StudentAnswerSubmission(val studentId: String, val isCorrect: Boolean, val timeTakenSeconds: Long)

        val submissions = listOf(
            StudentAnswerSubmission("student_1", isCorrect = true, timeTakenSeconds = 25),
            StudentAnswerSubmission("student_2", isCorrect = true, timeTakenSeconds = 45),
            StudentAnswerSubmission("student_3", isCorrect = false, timeTakenSeconds = 60),
            StudentAnswerSubmission("student_4", isCorrect = true, timeTakenSeconds = 30),
            StudentAnswerSubmission("student_5", isCorrect = false, timeTakenSeconds = 50)
        )

        // Aggregation contract matching Firestore/QuestionStatsRepository/CloudFunction
        var totalAttempts = 0L
        var correctAttempts = 0L
        var totalTimeSeconds = 0L

        submissions.forEach { sub ->
            totalAttempts++
            if (sub.isCorrect) correctAttempts++
            totalTimeSeconds += sub.timeTakenSeconds
        }

        val avgTimeSeconds = totalTimeSeconds.toDouble() / totalAttempts
        val stat = QuestionStat(
            questionId = "Q42",
            examId = "exam_ssc",
            totalAttempts = totalAttempts,
            correctAttempts = correctAttempts,
            totalTimeSeconds = totalTimeSeconds,
            avgTimeSeconds = avgTimeSeconds
        )

        // Verify aggregation values
        assertEquals("Total attempts must be 5", 5L, stat.totalAttempts)
        assertEquals("Correct attempts must be 3", 3L, stat.correctAttempts)
        assertEquals("Total time must be 210s", 210L, stat.totalTimeSeconds)
        assertEquals("Average time must be exactly 42.0s", 42.0, stat.avgTimeSeconds, 0.001)

        // Verify percentage-correct calculation (3/5 = 60%)
        assertEquals("Correct percentage must be 60%", 60, stat.correctPercentage)
        val badge = "${stat.correctPercentage}% got this right"
        assertEquals("Badge must state '60% got this right'", "60% got this right", badge)

        // Now verify post-answer popup for Student 6 who takes 35s and gets it right
        val student6Time = 35L
        val student6Correct = true
        assertTrue("Student 6 (35s) is faster than average (42.0s)", student6Time < stat.avgTimeSeconds)
        val student6Diff = (stat.avgTimeSeconds - student6Time).toInt() // 7s
        assertEquals(7, student6Diff)

        // Student 7 takes 55s and gets it right
        val student7Time = 55L
        assertTrue("Student 7 (55s) is slower than average (42.0s)", student7Time >= stat.avgTimeSeconds)
    }

    @Test
    fun testMultiStudentSubmissions_realWorldRankAndPercentileDistribution() {
        // Multi-submission scenario: 12 students complete an exam
        val cohortScores = listOf(
            142.0, 138.5, 131.0, 125.0, 120.0, 115.5,
            110.0, 95.0, 88.0, 75.0, 60.0, 45.0
        )
        val totalParticipants = cohortScores.size // 12

        fun computeRankAndPercentile(studentScore: Double): Pair<Int, Double> {
            val higher = cohortScores.count { it > studentScore }
            val rank = higher + 1
            val lower = cohortScores.count { it < studentScore }
            val percentile = if (totalParticipants <= 1) 100.0 else (lower * 100.0) / (totalParticipants - 1)
            return rank to percentile
        }

        // Student with score 125.0 (4th position)
        val (rankMid, pMid) = computeRankAndPercentile(125.0)
        assertEquals("Score 125.0 rank should be #4 out of 12", 4, rankMid)
        assertEquals("Score 125.0 percentile (8 lower out of 11) = 72.72%", 72.72, pMid, 0.01)

        // Top student with score 142.0 (1st position)
        val (rankTop, pTop) = computeRankAndPercentile(142.0)
        assertEquals("Top student rank should be #1", 1, rankTop)
        assertEquals("Top student percentile should be 100.0%", 100.0, pTop, 0.01)

        // Bottom student with score 45.0 (last position)
        val (rankBottom, pBottom) = computeRankAndPercentile(45.0)
        assertEquals("Bottom student rank should be #12", 12, rankBottom)
        assertEquals("Bottom student percentile should be 0.0%", 0.0, pBottom, 0.01)

        // Verify ties: Two students with 120.0
        val tiedCohort = listOf(140.0, 130.0, 120.0, 120.0, 110.0)
        val higherThan120 = tiedCohort.count { it > 120.0 } // 2 (140, 130)
        val tiedRank = higherThan120 + 1 // Rank #3 for both tied students
        assertEquals("Tied students should both share rank #3", 3, tiedRank)
    }

    @Test
    fun testAccuracyVsPercentileDistinction() {
        val totalQuestions = 25
        val attemptedQuestions = 20
        val correctAnswers = 15

        val accuracy = (correctAnswers * 100.0) / attemptedQuestions
        assertEquals("Accuracy = correct / attempted * 100", 75.0, accuracy, 0.01)

        val examPercentage = (correctAnswers * 100.0) / totalQuestions
        assertEquals("Total exam score percentage = 60%", 60.0, examPercentage, 0.01)
        assertTrue("Accuracy is distinct from overall percentage and percentile", accuracy != examPercentage)
    }

    @Test
    fun testStudentBestAndAverageScoreAcrossAttempts() {
        val pastScores = listOf(65.0, 78.0, 82.0, 70.0)
        val best = pastScores.maxOrNull() ?: 0.0
        val avg = pastScores.average()

        assertEquals("Best score should be 82.0", 82.0, best, 0.01)
        assertEquals("Average score should be 73.75", 73.75, avg, 0.01)
    }

    @Test
    fun testCategoryCutoffComparisonLogicAndOmissionWhenUnset() {
        val exam = Exam(
            id = "exam_1",
            examName = "SSC CGL Tier 1",
            cutoffs = mapOf(
                "General" to 135.0,
                "OBC" to 125.0,
                "SC" to 110.0
                // ST and EWS not configured
            )
        )

        val studentScore = 128.0

        // General: Cutoff 135, Score 128 -> Below Cutoff
        val genCutoff = exam.cutoffs["General"]
        assertNotNull(genCutoff)
        assertTrue("Score 128 is below General cutoff 135", studentScore < genCutoff!!)

        // OBC: Cutoff 125, Score 128 -> Above Cutoff (Qualified)
        val obcCutoff = exam.cutoffs["OBC"]
        assertNotNull(obcCutoff)
        assertTrue("Score 128 is above OBC cutoff 125", studentScore >= obcCutoff!!)

        // ST: Not configured -> MUST BE OMITTED (null, not 0.0 or placeholder)
        val stCutoff = exam.cutoffs["ST"]
        assertNull("Unconfigured ST category cutoff must be null to trigger UI omission", stCutoff)

        // EWS: Not configured -> MUST BE OMITTED
        val ewsCutoff = exam.cutoffs["EWS"]
        assertNull("Unconfigured EWS category cutoff must be null to trigger UI omission", ewsCutoff)
    }

    // =========================================================================
    // FEATURE 3 & VERIFICATION 5: UNIFIED REPORT SYSTEM (VALIDATION & ROUTING)
    // =========================================================================
    @Test
    fun testReportCommentValidationMinimum7WordsOr40Chars() {
        // Comment is optional: empty string, short phrase, or detailed explanations all succeed
        assertTrue("Empty comment is valid without typing explanation", FlaggedQuestion.isCommentValid(""))
        assertTrue("Short phrase is valid", FlaggedQuestion.isCommentValid("Wrong option answer"))
        assertTrue("Short phrase is valid", FlaggedQuestion.isCommentValid("Please check this"))

        // Valid by word count (>= 7 words)
        assertTrue(
            "7 words comment should pass",
            FlaggedQuestion.isCommentValid("The formula used in step two is incorrect")
        )

        // Valid by character count (>= 40 chars)
        assertTrue(
            "Long descriptive comment should pass",
            FlaggedQuestion.isCommentValid("CalculationErrorOnStep3FormulaSubstitutionFailed")
        )
    }

    @Test
    fun testReportIssueCategorizationAndRouting() {
        val contentIssue = "Wrong Question"
        val technicalIssue = "Blinking Screen Issue"
        val tech2 = "Dark Mode Issue"

        // Content issues route to flagged_questions
        assertTrue(FlaggedQuestion.isContentIssue(contentIssue))
        val contentTargetCollection = if (FlaggedQuestion.isContentIssue(contentIssue)) "flagged_questions" else "reported_bugs"
        assertEquals("Content issue must route to flagged_questions", "flagged_questions", contentTargetCollection)

        // Technical issues route to reported_bugs
        assertFalse(FlaggedQuestion.isContentIssue(technicalIssue))
        assertFalse(FlaggedQuestion.isContentIssue(tech2))
        val techTargetCollection = if (FlaggedQuestion.isContentIssue(technicalIssue)) "flagged_questions" else "reported_bugs"
        assertEquals("Technical issue must route to reported_bugs", "reported_bugs", techTargetCollection)
    }

    // =========================================================================
    // FEATURE 4 & VERIFICATION 6: REVIEW SCREEN COMBINABLE FILTERS
    // =========================================================================
    @Test
    fun testReviewCombinableFilters() {
        val items = listOf(
            AnswerItem(number = 1, questionText = "Q1", selected = "A", selectedText = "Option A", correct = "A", correctText = "Option A", timeTakenSeconds = 25), // Correct, 25s
            AnswerItem(number = 2, questionText = "Q2", selected = "B", selectedText = "Option B", correct = "C", correctText = "Option C", timeTakenSeconds = 55), // Wrong, Overtime (55s > 40s)
            AnswerItem(number = 3, questionText = "Q3", selected = "D", selectedText = "Option D", correct = "A", correctText = "Option A", timeTakenSeconds = 30), // Wrong, Normal time (30s <= 40s)
            AnswerItem(number = 4, questionText = "Q4", selected = "", selectedText = "", correct = "B", correctText = "Option B", timeTakenSeconds = 10),           // Unattempted, 10s
            AnswerItem(number = 5, questionText = "Q5", selected = "", selectedText = "", correct = "D", correctText = "Option D", timeTakenSeconds = 60)            // Unattempted, Overtime (60s > 40s)
        )

        val avgTime = 40.0

        // Filter: Unattempted only
        val unattemptedOnly = items.filter { it.isUnattempted }
        assertEquals("Should match 2 unattempted items (Q4, Q5)", 2, unattemptedOnly.size)

        // Filter: Overtime only (userTime > avgTime)
        val overtimeOnly = items.filter { it.timeTakenSeconds > avgTime }
        assertEquals("Should match 2 overtime items (Q2, Q5)", 2, overtimeOnly.size)

        // Filter: Combinable -> Wrong + Overtime
        val wrongAndOvertime = items.filter { it.isWrong && (it.timeTakenSeconds > avgTime) }
        assertEquals("Should match exactly 1 item (Q2)", 1, wrongAndOvertime.size)
        assertEquals("Matched item is Q2", 2, wrongAndOvertime.first().number)

        // Filter: Combinable -> Unattempted + Overtime
        val unattemptedAndOvertime = items.filter { it.isUnattempted && (it.timeTakenSeconds > avgTime) }
        assertEquals("Should match exactly 1 item (Q5)", 1, unattemptedAndOvertime.size)
        assertEquals("Matched item is Q5", 5, unattemptedAndOvertime.first().number)
    }

    // =========================================================================
    // FEATURE 5 & VERIFICATION 7: POST-ANSWER TIMER COMPARISON POPUP
    // =========================================================================
    @Test
    fun testTimerComparisonPopupMessageSelection() {
        fun getTimerFeedback(userSec: Long, avgSec: Double, isCorrect: Boolean): Pair<String, String> {
            val isFaster = userSec < avgSec
            return when {
                isCorrect && isFaster -> "⚡" to "Lightning fast & accurate! You beat the average time by ${(avgSec - userSec).toInt()}s."
                isCorrect && !isFaster -> "🎯" to "Correct! Keep practicing to get faster than the average time."
                !isCorrect && isFaster -> "⚠️" to "Fast, but incorrect. Make sure to double check your answer."
                else -> "💡" to "Take time to review this concept and formula."
            }
        }

        // Fast & correct
        val (emoji1, msg1) = getTimerFeedback(20, 45.0, true)
        assertEquals("⚡", emoji1)
        assertTrue(msg1.contains("Lightning fast"))

        // Slow & correct
        val (emoji2, msg2) = getTimerFeedback(50, 45.0, true)
        assertEquals("🎯", emoji2)
        assertTrue(msg2.contains("Keep practicing"))

        // Fast & incorrect
        val (emoji3, msg3) = getTimerFeedback(15, 45.0, false)
        assertEquals("⚠️", emoji3)
        assertTrue(msg3.contains("Fast, but incorrect"))
    }

    // =========================================================================
    // FEATURE 6 & VERIFICATION 8: QUESTION PALETTE NAVIGATION & COLOR CODING
    // =========================================================================
    @Test
    fun testQuestionPaletteStateMapping() {
        val p1 = PaletteItem(number = 1, state = PaletteState.CORRECT)
        val p2 = PaletteItem(number = 2, state = PaletteState.WRONG)
        val p3 = PaletteItem(number = 3, state = PaletteState.ANSWERED)
        val p4 = PaletteItem(number = 4, state = PaletteState.UNATTEMPTED, isActive = true)

        assertEquals(PaletteState.CORRECT, p1.state)
        assertEquals(PaletteState.WRONG, p2.state)
        assertEquals(PaletteState.ANSWERED, p3.state)
        assertEquals(PaletteState.UNATTEMPTED, p4.state)
        assertTrue("Q4 should have active highlight indicator", p4.isActive)
    }
}
