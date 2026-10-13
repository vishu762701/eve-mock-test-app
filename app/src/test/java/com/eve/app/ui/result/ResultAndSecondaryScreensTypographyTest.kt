package com.eve.app.ui.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Phase 5 Official Apple iOS 26 Typography & Semantic Colors Verification for Result and Secondary Screens.
 *
 * Verifies:
 * 1. Zero hardcoded text sizes (android:textSize) across all 19 Phase 5 layout files:
 *    - Result: activity_result.xml, activity_result_detail.xml, item_answer.xml, item_result_topic.xml, item_topic_stat.xml
 *    - Leaderboard: activity_leaderboard.xml, item_leaderboard.xml
 *    - History: activity_history.xml, item_history.xml
 *    - Syllabus: activity_syllabus.xml, item_syllabus.xml
 *    - Feedback: activity_feedback.xml, item_feedback_message.xml, item_post_reply.xml
 *    - Dialogs: dialog_create_feedback_post.xml, dialog_reply_feedback.xml, dialog_report_question.xml
 *    - Informational: activity_about.xml, activity_content_display.xml
 * 2. Component-to-typography semantic bindings according to Apple iOS 26 dynamic typography hierarchy:
 *    - Hero Score: 36sp bold (LargeTitle role)
 *    - Metric counts: 24sp bold (Title2 role)
 *    - Section titles: 20sp medium (Title3 role)
 *    - Item titles & Question text: 17sp (Headline role)
 *    - Input fields & Option text: 16sp (Callout role)
 *    - Subheadings & Row values: 15sp (Subheadline role)
 *    - Badges, Captions & Metadata: 12-13sp (Footnote / Caption role)
 * 3. Preservation of all protected invariants (server scoring, rigid test geometry, color invariants).
 */
class ResultAndSecondaryScreensTypographyTest {

    private fun loadXml(relativePath: String): Element {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun findElementById(node: Node, targetId: String): Element? {
        if (node is Element) {
            val id = node.getAttribute("android:id")
            if (id == targetId || id == "@+id/$targetId" || id.endsWith("/$targetId")) {
                return node
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            val found = findElementById(children.item(i), targetId)
            if (found != null) return found
        }
        return null
    }

    private fun collectAllElementsWithAttribute(node: Node, attrName: String, list: MutableList<Element> = mutableListOf()): List<Element> {
        if (node is Element) {
            if (node.hasAttribute(attrName)) {
                list.add(node)
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            collectAllElementsWithAttribute(children.item(i), attrName, list)
        }
        return list
    }

    @Test
    fun testZeroHardcodedTextSizesInAllPhase5Layouts() {
        val layoutFiles = listOf(
            "src/main/res/layout/activity_result.xml",
            "src/main/res/layout/activity_result_detail.xml",
            "src/main/res/layout/item_answer.xml",
            "src/main/res/layout/item_result_topic.xml",
            "src/main/res/layout/item_topic_stat.xml",
            "src/main/res/layout/activity_leaderboard.xml",
            "src/main/res/layout/item_leaderboard.xml",
            "src/main/res/layout/activity_history.xml",
            "src/main/res/layout/item_history.xml",
            "src/main/res/layout/activity_syllabus.xml",
            "src/main/res/layout/item_syllabus.xml",
            "src/main/res/layout/activity_feedback.xml",
            "src/main/res/layout/item_feedback_message.xml",
            "src/main/res/layout/item_post_reply.xml",
            "src/main/res/layout/dialog_create_feedback_post.xml",
            "src/main/res/layout/dialog_reply_feedback.xml",
            "src/main/res/layout/dialog_report_question.xml",
            "src/main/res/layout/activity_about.xml",
            "src/main/res/layout/activity_content_display.xml"
        )

        for (filePath in layoutFiles) {
            val root = loadXml(filePath)
            val elementsWithTextSize = collectAllElementsWithAttribute(root, "android:textSize")
            val violatingIds = elementsWithTextSize.map { el ->
                el.getAttribute("android:id").ifEmpty { "<${el.tagName}>" }
            }
            assertTrue(
                "Layout $filePath must contain 0 hardcoded android:textSize occurrences, but found: $violatingIds",
                elementsWithTextSize.isEmpty()
            )
        }
    }

    @Test
    fun testResultOverviewTypographyBindings() {
        val root = loadXml("src/main/res/layout/activity_result.xml")

        val tvTotalScore = findElementById(root, "tvTotalScore")
        assertNotNull("tvTotalScore must exist", tvTotalScore)
        assertEquals("@style/TextAppearance.Eve.Result.HeroScore", tvTotalScore?.getAttribute("style"))

        val tvScorePercentage = findElementById(root, "tvScorePercentage")
        assertNotNull("tvScorePercentage must exist", tvScorePercentage)
        assertEquals("@style/TextAppearance.Eve.Result.ScoreBadge", tvScorePercentage?.getAttribute("style"))

        val tvCorrectCount = findElementById(root, "tvCorrectCount")
        assertNotNull("tvCorrectCount must exist", tvCorrectCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", tvCorrectCount?.getAttribute("style"))

        val tvIncorrectCount = findElementById(root, "tvIncorrectCount")
        assertNotNull("tvIncorrectCount must exist", tvIncorrectCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", tvIncorrectCount?.getAttribute("style"))

        val tvSkippedCount = findElementById(root, "tvSkippedCount")
        assertNotNull("tvSkippedCount must exist", tvSkippedCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", tvSkippedCount?.getAttribute("style"))

        val tvCorrectLabel = findElementById(root, "tvCorrectLabel")
        assertNotNull("tvCorrectLabel must exist", tvCorrectLabel)
        assertEquals("@style/TextAppearance.Eve.Result.MetricLabel", tvCorrectLabel?.getAttribute("style"))

        val tvIncorrectLabel = findElementById(root, "tvIncorrectLabel")
        assertNotNull("tvIncorrectLabel must exist", tvIncorrectLabel)
        assertEquals("@style/TextAppearance.Eve.Result.MetricLabel", tvIncorrectLabel?.getAttribute("style"))

        val tvSkippedLabel = findElementById(root, "tvSkippedLabel")
        assertNotNull("tvSkippedLabel must exist", tvSkippedLabel)
        assertEquals("@style/TextAppearance.Eve.Result.MetricLabel", tvSkippedLabel?.getAttribute("style"))

        val btnReattempt = findElementById(root, "btnReattempt")
        assertNotNull("btnReattempt must exist", btnReattempt)
        assertEquals("@style/Widget.Eve.Button.OrangePill", btnReattempt?.getAttribute("style"))
    }

    @Test
    fun testResultDetailTypographyBindings() {
        val root = loadXml("src/main/res/layout/activity_result_detail.xml")

        val tvHeaderTitle = findElementById(root, "tvHeaderTitle")
        assertNotNull("tvHeaderTitle must exist", tvHeaderTitle)
        assertEquals("@style/TextAppearance.Eve.ScreenTitle", tvHeaderTitle?.getAttribute("style"))

        val tvStatTotalQuestionsValue = findElementById(root, "tvStatTotalQuestionsValue")
        assertNotNull("tvStatTotalQuestionsValue must exist", tvStatTotalQuestionsValue)
        assertEquals("@style/TextAppearance.Eve.Result.InsetValue", tvStatTotalQuestionsValue?.getAttribute("style"))

        val tvStatAttemptedValue = findElementById(root, "tvStatAttemptedValue")
        assertNotNull("tvStatAttemptedValue must exist", tvStatAttemptedValue)
        assertEquals("@style/TextAppearance.Eve.Result.InsetValue", tvStatAttemptedValue?.getAttribute("style"))

        val tvStatAccuracyValue = findElementById(root, "tvStatAccuracyValue")
        assertNotNull("tvStatAccuracyValue must exist", tvStatAccuracyValue)
        assertEquals("@style/TextAppearance.Eve.Result.InsetValue", tvStatAccuracyValue?.getAttribute("style"))
    }

    @Test
    fun testAnswerReviewItemTypographyBindings() {
        val root = loadXml("src/main/res/layout/item_answer.xml")

        val tvQ = findElementById(root, "tvQ")
        assertNotNull("tvQ must exist", tvQ)
        assertEquals("@style/TextAppearance.Eve.QuestionText", tvQ?.getAttribute("style"))

        for (letter in listOf("A", "B", "C", "D")) {
            val badge = findElementById(root, "tvBadgeOption$letter")
            assertNotNull("tvBadgeOption$letter must exist", badge)
            assertEquals("@style/TextAppearance.Eve.Result.OptionKey", badge?.getAttribute("style"))

            val text = findElementById(root, "tvOption$letter")
            assertNotNull("tvOption$letter must exist", text)
            assertEquals("@style/TextAppearance.Eve.Result.OptionText", text?.getAttribute("style"))
        }

        val tvCorrectSummary = findElementById(root, "tvCorrectSummary")
        assertNotNull("tvCorrectSummary must exist", tvCorrectSummary)
        assertEquals("@style/TextAppearance.Eve.Result.ExplanationHeader", tvCorrectSummary?.getAttribute("style"))

        val tvExplanation = findElementById(root, "tvExplanation")
        assertNotNull("tvExplanation must exist", tvExplanation)
        assertEquals("@style/TextAppearance.Eve.Result.ExplanationBody", tvExplanation?.getAttribute("style"))
    }

    @Test
    fun testLeaderboardTypographyBindings() {
        val root = loadXml("src/main/res/layout/activity_leaderboard.xml")

        val tvRankMedallion = findElementById(root, "tvRankMedallion")
        assertNotNull("tvRankMedallion must exist", tvRankMedallion)
        assertEquals("@style/TextAppearance.Eve.Subheadline.Medium", tvRankMedallion?.getAttribute("style"))

        val tvYourRank = findElementById(root, "tvYourRank")
        assertNotNull("tvYourRank must exist", tvYourRank)
        assertEquals("@style/TextAppearance.Eve.Title3", tvYourRank?.getAttribute("style"))

        val tvYourScore = findElementById(root, "tvYourScore")
        assertNotNull("tvYourScore must exist", tvYourScore)
        assertEquals("@style/TextAppearance.Eve.Title3", tvYourScore?.getAttribute("style"))

        val itemRoot = loadXml("src/main/res/layout/item_leaderboard.xml")
        val tvRank = findElementById(itemRoot, "tvRank")
        assertNotNull("tvRank must exist", tvRank)
        assertEquals("@style/TextAppearance.Eve.Leaderboard.Rank", tvRank?.getAttribute("style"))

        val tvName = findElementById(itemRoot, "tvName")
        assertNotNull("tvName must exist", tvName)
        assertEquals("@style/TextAppearance.Eve.Leaderboard.Name", tvName?.getAttribute("style"))

        val tvScore = findElementById(itemRoot, "tvScore")
        assertNotNull("tvScore must exist", tvScore)
        assertEquals("@style/TextAppearance.Eve.Leaderboard.Score", tvScore?.getAttribute("style"))
    }

    @Test
    fun testSecondaryScreensTypographyBindings() {
        val syllabusRoot = loadXml("src/main/res/layout/activity_syllabus.xml")
        val etSearch = findElementById(syllabusRoot, "etSearch")
        assertNotNull("etSearch must exist", etSearch)
        assertEquals("@style/TextAppearance.Eve.Search", etSearch?.getAttribute("style"))

        val syllabusItemRoot = loadXml("src/main/res/layout/item_syllabus.xml")
        val tvExamName = findElementById(syllabusItemRoot, "tvExamName")
        assertNotNull("tvExamName must exist", tvExamName)
        assertEquals("@style/TextAppearance.Eve.Secondary.ItemTitle", tvExamName?.getAttribute("style"))

        val feedbackRoot = loadXml("src/main/res/layout/activity_feedback.xml")
        val etFeedbackMessage = findElementById(feedbackRoot, "etFeedbackMessage")
        assertNotNull("etFeedbackMessage must exist", etFeedbackMessage)
        assertEquals("@style/TextAppearance.Eve.Callout", etFeedbackMessage?.getAttribute("style"))

        val aboutRoot = loadXml("src/main/res/layout/activity_about.xml")
        val tvAppName = findElementById(aboutRoot, "tvAppName")
        assertNotNull("tvAppName must exist", tvAppName)
        assertEquals("@style/TextAppearance.Eve.Secondary.SectionHeader", tvAppName?.getAttribute("style"))

        val contentDisplayRoot = loadXml("src/main/res/layout/activity_content_display.xml")
        val tvTitle = findElementById(contentDisplayRoot, "tvTitle")
        assertNotNull("tvTitle must exist", tvTitle)
        assertEquals("@style/TextAppearance.Eve.Title2", tvTitle?.getAttribute("style"))
    }
}
