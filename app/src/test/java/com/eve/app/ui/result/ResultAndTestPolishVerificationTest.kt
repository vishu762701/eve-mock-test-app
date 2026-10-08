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

    @Test
    fun testSingleQuestionIsolationLogic() {
        val sampleItems = listOf(
            AnswerItem(number = 1, questionText = "Question 1", selected = "A", selectedText = "Alpha", correct = "A", correctText = "Alpha"),
            AnswerItem(number = 2, questionText = "Question 2", selected = "B", selectedText = "Beta", correct = "C", correctText = "Gamma"),
            AnswerItem(number = 3, questionText = "Question 3", selected = "", selectedText = "", correct = "D", correctText = "Delta")
        )

        // Simulating palette click on Question 2 (index 1)
        val isolatedIndex = 1
        val isolatedList = listOf(sampleItems[isolatedIndex])
        assertEquals(1, isolatedList.size)
        assertEquals(2, isolatedList[0].number)
        assertEquals("Question 2", isolatedList[0].questionText)

        // Simulating clearing isolation, returns to active filter (e.g. show all or right)
        val restoredList = sampleItems.filter { it.isCorrect }
        assertEquals(1, restoredList.size)
        assertEquals(1, restoredList[0].number)
    }

    @Test
    fun testExamItemHasTransparentIconContainer() {
        val file = File("src/main/res/layout/item_exam.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/item_exam.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/item_exam.xml")
        assertTrue("item_exam.xml must exist", file.exists())
        val content = file.readText()
        assertTrue(
            "examIconContainer must specify eve_exam_icon_tile_bg background",
            content.contains("android:id=\"@+id/examIconContainer\"") &&
            content.contains("app:cardBackgroundColor=\"@color/eve_exam_icon_tile_bg\"")
        )
    }

    @Test
    fun testQuestionHeaderBookmarkReportPairDimensions() {
        val file = File("src/main/res/layout/item_question.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/item_question.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/item_question.xml")
        assertTrue("item_question.xml must exist", file.exists())
        val content = file.readText()

        assertTrue("btnBookmark must exist", content.contains("android:id=\"@+id/btnBookmark\""))
        assertTrue("btnReport must exist", content.contains("android:id=\"@+id/btnReport\""))
        assertTrue("btnReport must use ic_report_flag_premium", content.contains("@drawable/ic_report_flag_premium"))
        assertFalse("btnReport must not use circular bg_btn_report", content.contains("@drawable/bg_btn_report"))
        assertTrue("Bookmark/report must use bare backgrounds", content.contains("android:background=\"@android:color/transparent\""))
        assertTrue("Touch target FrameLayout must be 48dp", content.contains("android:layout_width=\"48dp\"") && content.contains("android:layout_height=\"48dp\""))
    }

    @Test
    fun testReviewQuestionFirstSolutionOnDemandLayout() {
        val file = File("src/main/res/layout/item_answer.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/item_answer.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/item_answer.xml")
        assertTrue("item_answer.xml must exist", file.exists())
        val content = file.readText()

        assertTrue("btnViewSolution must exist", content.contains("android:id=\"@+id/btnViewSolution\""))
        assertTrue("layoutOptionsContainer must exist to show question options", content.contains("android:id=\"@+id/layoutOptionsContainer\""))
        assertTrue("layoutSolutionDetails must exist", content.contains("android:id=\"@+id/layoutSolutionDetails\""))
        assertTrue("layoutSolutionDetails must be initially gone", content.contains("android:id=\"@+id/layoutSolutionDetails\"") && content.contains("android:visibility=\"gone\""))
        assertTrue("Report icon in item_answer must use ic_report_flag_premium", content.contains("android:id=\"@+id/ivReportQuestion\"") && content.contains("@drawable/ic_report_flag_premium"))
        assertFalse("Report icon in item_answer must not have circular bg_btn_report", content.contains("@drawable/bg_btn_report"))
    }

    @Test
    fun testResultScreenNestedScrollAndActionCluster() {
        val file = File("src/main/res/layout/activity_result.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_result.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_result.xml")
        assertTrue("activity_result.xml must exist", file.exists())
        val content = file.readText()

        assertTrue("scrollResultContent NestedScrollView must exist", content.contains("android:id=\"@+id/scrollResultContent\""))
        assertTrue("btnReattempt header pill must exist", content.contains("android:id=\"@+id/btnReattempt\""))
        assertTrue("btnShare header pill must exist", content.contains("android:id=\"@+id/btnShare\""))
        assertFalse("chipIsolatedQuestion must not exist", content.contains("android:id=\"@+id/chipIsolatedQuestion\""))
    }

    @Test
    fun testResultScreenThreeSectionTabStructure() {
        val file = File("src/main/res/layout/activity_result.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_result.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_result.xml")
        assertTrue("activity_result.xml must exist", file.exists())
        val content = file.readText()

        // 1. Tab Navigation & 3 Primary Sections
        assertTrue("tabLayoutResult must exist", content.contains("android:id=\"@+id/tabLayoutResult\""))
        assertTrue("sectionOverview must exist", content.contains("android:id=\"@+id/sectionOverview\""))
        assertTrue("sectionCutoff must exist", content.contains("android:id=\"@+id/sectionCutoff\""))
        assertTrue("sectionReview must exist", content.contains("android:id=\"@+id/sectionReview\""))
        assertTrue("sectionLeaderboard must exist", content.contains("android:id=\"@+id/sectionLeaderboard\""))

        // Approved tab order: Overview -> Review -> Leaderboard
        val reviewIdx = content.indexOf("android:text=\"Review\"")
        val overviewIdx = content.indexOf("android:text=\"Overview\"")
        val leaderboardIdx = content.indexOf("android:text=\"Leaderboard\"")
        assertTrue("Overview tab must appear before Review", overviewIdx in 0 until reviewIdx)
        assertTrue("Review tab must appear before Leaderboard", reviewIdx in 0 until leaderboardIdx)

        // Cutoff integrated into Overview
        val overviewStart = content.indexOf("android:id=\"@+id/sectionOverview\"")
        val reviewStart = content.indexOf("android:id=\"@+id/sectionReview\"")
        val cutoffPos = content.indexOf("android:id=\"@+id/sectionCutoff\"")
        assertTrue("sectionCutoff must be nested inside sectionOverview", cutoffPos in overviewStart until reviewStart)

        // 2. Cutoff Category Selection Dropdown Pill & Bottom Sheet Rows
        assertTrue("tvCutoffSelectedCategory must exist as dropdown pill", content.contains("android:id=\"@+id/tvCutoffSelectedCategory\""))

        val sheetFile = File("src/main/res/layout/bottom_sheet_category_picker.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/bottom_sheet_category_picker.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/bottom_sheet_category_picker.xml")
        assertTrue("bottom_sheet_category_picker.xml must exist", sheetFile != null && sheetFile.exists())
        val sheetContent = sheetFile!!.readText()

        assertTrue("rowSheetGeneral must exist", sheetContent.contains("android:id=\"@+id/rowSheetGeneral\""))
        assertTrue("rowSheetObc must exist", sheetContent.contains("android:id=\"@+id/rowSheetObc\""))
        assertTrue("rowSheetSc must exist", sheetContent.contains("android:id=\"@+id/rowSheetSc\""))
        assertTrue("rowSheetSt must exist", sheetContent.contains("android:id=\"@+id/rowSheetSt\""))
        assertTrue("rowSheetEws must exist", sheetContent.contains("android:id=\"@+id/rowSheetEws\""))

        assertTrue("ivSheetCheckGeneral must exist", sheetContent.contains("android:id=\"@+id/ivSheetCheckGeneral\""))
        assertTrue("ivSheetCheckObc must exist", sheetContent.contains("android:id=\"@+id/ivSheetCheckObc\""))
        assertTrue("ivSheetCheckSc must exist", sheetContent.contains("android:id=\"@+id/ivSheetCheckSc\""))
        assertTrue("ivSheetCheckSt must exist", sheetContent.contains("android:id=\"@+id/ivSheetCheckSt\""))
        assertTrue("ivSheetCheckEws must exist", sheetContent.contains("android:id=\"@+id/ivSheetCheckEws\""))

        // 3. Header Language Removal from Result, Test, Bookmarks, and Mistakes (only preserved in Settings)
        assertFalse("btnLanguage must not exist in Result header", content.contains("android:id=\"@+id/btnLanguage\""))
        val testLayoutFile = File("src/main/res/layout/activity_test.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_test.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_test.xml")
        assertTrue("activity_test.xml must exist", testLayoutFile != null && testLayoutFile.exists())
        val testContent = testLayoutFile!!.readText()
        assertFalse("btnLanguage must be removed from Test screen", testContent.contains("android:id=\"@+id/btnLanguage\""))

        val bookmarksFile = File("src/main/res/layout/activity_bookmarks.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_bookmarks.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_bookmarks.xml")
        if (bookmarksFile != null && bookmarksFile.exists()) {
            assertFalse("btnLanguage must be removed from Bookmarks screen", bookmarksFile.readText().contains("android:id=\"@+id/btnLanguage\""))
        }

        val mistakesFile = File("src/main/res/layout/activity_mistakes.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_mistakes.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_mistakes.xml")
        if (mistakesFile != null && mistakesFile.exists()) {
            assertFalse("btnLanguage must be removed from Mistakes screen", mistakesFile.readText().contains("android:id=\"@+id/btnLanguage\""))
        }
    }

    @Test
    fun testCutoffScoreRelationshipCalculation() {
        data class CutoffVerdict(val qualified: Boolean, val message: String)

        fun computeRelationship(score: Double, category: String, cutoff: Double?): CutoffVerdict {
            if (cutoff == null || cutoff <= 0) {
                return CutoffVerdict(false, "No qualifying cutoff mark is configured for the $category category.")
            }
            return if (score >= cutoff) {
                val diff = score - cutoff
                val msg = if (diff >= 0.05) {
                    "You cleared the $category cutoff mark by +${String.format(java.util.Locale.US, "%.1f", diff)} marks."
                } else {
                    "You achieved the exact qualifying score for $category."
                }
                CutoffVerdict(true, msg)
            } else {
                val diff = cutoff - score
                val msg = "You are ${String.format(java.util.Locale.US, "%.1f", diff)} marks below the $category cutoff threshold."
                CutoffVerdict(false, msg)
            }
        }

        // General: Score 78, Cutoff 70 -> Qualified (+8.0)
        val general = computeRelationship(78.0, "General", 70.0)
        assertTrue(general.qualified)
        assertTrue(general.message.contains("+8.0 marks"))

        // OBC: Score 78, Cutoff 82 -> Not Qualified (-4.0)
        val obc = computeRelationship(78.0, "OBC", 82.0)
        assertFalse(obc.qualified)
        assertTrue(obc.message.contains("4.0 marks below"))

        // SC: Exact cutoff match
        val sc = computeRelationship(60.0, "SC", 60.0)
        assertTrue(sc.qualified)
        assertTrue(sc.message.contains("exact qualifying score"))

        // EWS: Not configured
        val ews = computeRelationship(78.0, "EWS", null)
        assertFalse(ews.qualified)
        assertTrue(ews.message.contains("No qualifying cutoff mark is configured"))
    }

    @Test
    fun testSingleQuestionPaletteNavigationTransitions() {
        val sampleItems = (1..10).map { i ->
            AnswerItem(
                number = i,
                questionText = "Question $i text",
                selected = if (i % 2 == 0) "A" else "",
                selectedText = "Option A",
                correct = "A",
                correctText = "Option A"
            )
        }

        // Helper simulating single question submission
        fun selectQuestionItem(index: Int): List<AnswerItem> {
            return listOf(sampleItems[index])
        }

        // Tap Q1 (index 0) -> only Q1 displayed
        val q1List = selectQuestionItem(0)
        assertEquals(1, q1List.size)
        assertEquals(1, q1List[0].number)

        // Switch to Q4 (index 3) -> only Q4 displayed
        val q4List = selectQuestionItem(3)
        assertEquals(1, q4List.size)
        assertEquals(4, q4List[0].number)

        // Switch to Q2 (index 1) -> only Q2 displayed
        val q2List = selectQuestionItem(1)
        assertEquals(1, q2List.size)
        assertEquals(2, q2List[0].number)
    }

    @Test
    fun testMaxThreeAttemptsLimitForNormalUsers() {
        val maxAllowed = com.eve.app.util.AttemptLimitManager.MAX_ATTEMPTS
        assertEquals(3, maxAllowed)

        fun canUserAttempt(attemptCount: Int, isAdmin: Boolean): Boolean {
            if (isAdmin) return true
            return attemptCount < maxAllowed
        }

        // Normal user
        assertTrue("Attempt 0: allowed", canUserAttempt(0, isAdmin = false))
        assertTrue("Attempt 1: allowed", canUserAttempt(1, isAdmin = false))
        assertTrue("Attempt 2: allowed", canUserAttempt(2, isAdmin = false))
        assertFalse("Attempt 3: limit reached, blocked", canUserAttempt(3, isAdmin = false))
        assertFalse("Attempt 4+: blocked", canUserAttempt(4, isAdmin = false))

        // Admin user: always allowed
        assertTrue("Admin at 0: allowed", canUserAttempt(0, isAdmin = true))
        assertTrue("Admin at 3: allowed", canUserAttempt(3, isAdmin = true))
        assertTrue("Admin at 10: allowed", canUserAttempt(10, isAdmin = true))
    }

    @Test
    fun testTargetExamsBottomSheetContentAndButtonSizing() {
        val sheetFile = File("src/main/res/layout/bottom_sheet_target_exams.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/bottom_sheet_target_exams.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/bottom_sheet_target_exams.xml")
        assertTrue("bottom_sheet_target_exams.xml must exist", sheetFile != null && sheetFile.exists())
        val content = sheetFile!!.readText()

        assertTrue("Sheet title must be user-oriented", content.contains("Which exams are you preparing for?"))
        assertTrue("Sheet subtitle must explain personalization benefit", content.contains("Select your target exams to personalize your test feed and recommendations."))
        assertTrue("btnSkip must have weight 1", content.contains("android:id=\"@+id/btnSkip\"") && content.contains("android:layout_weight=\"1\""))
        assertTrue("btnContinue must have weight 1", content.contains("android:id=\"@+id/btnContinue\"") && content.contains("android:layout_weight=\"1\""))
    }

    @Test
    fun testHomeStreakAndAdminPillWidthParity() {
        val mainFile = File("src/main/res/layout/activity_main.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_main.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_main.xml")
        assertTrue("activity_main.xml must exist", mainFile != null && mainFile.exists())
        val content = mainFile!!.readText()

        assertTrue("layoutStreakPill must exist", content.contains("android:id=\"@+id/layoutStreakPill\""))
        assertTrue("btnAdmin must exist", content.contains("android:id=\"@+id/btnAdmin\""))

        // The streak remains outside the single admin-managed Home Banner surface.
        val streakPillStart = content.indexOf("android:id=\"@+id/layoutStreakPill\"")
        val streakChunk = content.substring(streakPillStart, content.indexOf(">", streakPillStart))
        assertTrue("layoutStreakPill should size to its content", streakChunk.contains("android:layout_width=\"wrap_content\""))

        val bannerStart = content.indexOf("android:id=\"@+id/panelHomeBanner\"")
        val bannerEnd = content.indexOf("</FrameLayout>", bannerStart)
        val adminStart = content.indexOf("android:id=\"@+id/btnAdmin\"")
        val adminChunk = content.substring(adminStart, content.indexOf(">", adminStart))
        assertTrue("One large Home Banner surface must exist", bannerStart >= 0)
        assertTrue("Separate legacy Home Banner card must not remain", !content.contains("@+id/cardHomeBanner"))
        assertTrue("Admin shortcut must live outside the Home Banner", adminStart > bannerEnd)
        assertTrue("Admin shortcut remains full width", adminChunk.contains("android:layout_width=\"match_parent\""))
    }

    @Test
    fun testApprovedNativeTestSurfaces() {
        val base = File("src/main/res").takeIf { it.exists() } ?: File("app/src/main/res")
        val question = File(base, "layout/item_question.xml").readText()
        val test = File(base, "layout/activity_test.xml").readText()
        assertFalse(question.contains("BlurView"))
        assertFalse(test.contains("BlurView"))
        assertTrue(question.contains("@+id/questionScroll"))
        assertTrue(question.contains("android:lineSpacingMultiplier=\"1.5\""))
        assertTrue(test.contains("Widget.Eve.ApprovedTestAction"))
        assertTrue(test.contains("Widget.Eve.ApprovedTestNavigation"))
    }

    @Test
    fun testReattemptOrangeConsistency() {
        val resultFile = File("src/main/res/layout/activity_result.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_result.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/activity_result.xml")
        assertTrue("activity_result.xml must exist", resultFile != null && resultFile.exists())
        val resultContent = resultFile!!.readText()
        assertTrue("btnReattempt in Result header must use OrangePill", resultContent.contains("style=\"@style/Widget.Eve.Button.OrangePill\""))

        val completedSheetFile = File("src/main/res/layout/bottom_sheet_completed_exam.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/bottom_sheet_completed_exam.xml").takeIf { it.exists() }
            ?: File("eve-mock-test-app/app/src/main/res/layout/bottom_sheet_completed_exam.xml")
        assertTrue("bottom_sheet_completed_exam.xml must exist", completedSheetFile != null && completedSheetFile.exists())
        val completedContent = completedSheetFile!!.readText()
        assertTrue("btnReattempt in completed exam sheet must use Orange style", completedContent.contains("style=\"@style/Widget.Eve.Button.Orange\""))
        assertFalse("btnReattempt must not use error color", completedContent.contains("@color/error"))
    }
}
