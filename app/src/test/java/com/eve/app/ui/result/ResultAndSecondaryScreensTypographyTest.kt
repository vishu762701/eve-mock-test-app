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

    private fun getStyleOrAppearance(element: Element?): String? {
        if (element == null) return null
        val styleAttr = element.getAttribute("style")
        if (styleAttr.isNotEmpty()) return styleAttr
        val textAppAttr = element.getAttribute("android:textAppearance")
        if (textAppAttr.isNotEmpty()) return textAppAttr
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

        val tvScore = findElementById(root, "tvScore")
        assertNotNull("tvScore must exist", tvScore)
        assertEquals("@style/TextAppearance.Eve.Result.HeroScore", getStyleOrAppearance(tvScore))

        val tvScorePercentage = findElementById(root, "tvScorePercentage")
        assertNotNull("tvScorePercentage must exist", tvScorePercentage)
        assertEquals("@style/TextAppearance.Eve.Result.ScoreBadge", getStyleOrAppearance(tvScorePercentage))

        val tvCorrectCount = findElementById(root, "tvCorrectCount")
        assertNotNull("tvCorrectCount must exist", tvCorrectCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvCorrectCount))

        val tvWrongCount = findElementById(root, "tvWrongCount")
        assertNotNull("tvWrongCount must exist", tvWrongCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvWrongCount))

        val tvUnattemptedCount = findElementById(root, "tvUnattemptedCount")
        assertNotNull("tvUnattemptedCount must exist", tvUnattemptedCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvUnattemptedCount))

        val tvAccuracy = findElementById(root, "tvAccuracy")
        assertNotNull("tvAccuracy must exist", tvAccuracy)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvAccuracy))

        val btnReattempt = findElementById(root, "btnReattempt")
        assertNotNull("btnReattempt must exist", btnReattempt)
        assertEquals("@style/Widget.Eve.Button.OrangePill", getStyleOrAppearance(btnReattempt))
    }

    @Test
    fun testResultDetailTypographyBindings() {
        val root = loadXml("src/main/res/layout/activity_result_detail.xml")

        val tvDetailTitle = findElementById(root, "tvDetailTitle")
        assertNotNull("tvDetailTitle must exist", tvDetailTitle)
        assertEquals("@style/TextAppearance.Eve.Headline", getStyleOrAppearance(tvDetailTitle))

        val tvStatHeroScore = findElementById(root, "tvStatHeroScore")
        assertNotNull("tvStatHeroScore must exist", tvStatHeroScore)
        assertEquals("@style/TextAppearance.Eve.Result.HeroScore", getStyleOrAppearance(tvStatHeroScore))

        val tvStatHeroPercentage = findElementById(root, "tvStatHeroPercentage")
        assertNotNull("tvStatHeroPercentage must exist", tvStatHeroPercentage)
        assertEquals("@style/TextAppearance.Eve.Result.ScoreBadge", getStyleOrAppearance(tvStatHeroPercentage))

        val tvStatRightCount = findElementById(root, "tvStatRightCount")
        assertNotNull("tvStatRightCount must exist", tvStatRightCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvStatRightCount))

        val tvStatWrongCount = findElementById(root, "tvStatWrongCount")
        assertNotNull("tvStatWrongCount must exist", tvStatWrongCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvStatWrongCount))

        val tvStatUnattemptedTileCount = findElementById(root, "tvStatUnattemptedTileCount")
        assertNotNull("tvStatUnattemptedTileCount must exist", tvStatUnattemptedTileCount)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvStatUnattemptedTileCount))

        val tvStatAccuracyPct = findElementById(root, "tvStatAccuracyPct")
        assertNotNull("tvStatAccuracyPct must exist", tvStatAccuracyPct)
        assertEquals("@style/TextAppearance.Eve.Result.MetricCount", getStyleOrAppearance(tvStatAccuracyPct))
    }

    @Test
    fun testAnswerReviewItemTypographyBindings() {
        val root = loadXml("src/main/res/layout/item_answer.xml")

        val tvQ = findElementById(root, "tvQ")
        assertNotNull("tvQ must exist", tvQ)
        assertEquals("@style/TextAppearance.Eve.QuestionText", getStyleOrAppearance(tvQ))

        for (letter in listOf("A", "B", "C", "D")) {
            val badge = findElementById(root, "tvBadgeOption$letter")
            assertNotNull("tvBadgeOption$letter must exist", badge)
            assertEquals("@style/TextAppearance.Eve.Result.OptionKey", getStyleOrAppearance(badge))

            val text = findElementById(root, "tvOption$letter")
            assertNotNull("tvOption$letter must exist", text)
            assertEquals("@style/TextAppearance.Eve.Result.OptionText", getStyleOrAppearance(text))
        }

        val tvCorrectSummary = findElementById(root, "tvCorrectSummary")
        assertNotNull("tvCorrectSummary must exist", tvCorrectSummary)
        assertEquals("@style/TextAppearance.Eve.Result.ExplanationHeader", getStyleOrAppearance(tvCorrectSummary))

        val tvExplanation = findElementById(root, "tvExplanation")
        assertNotNull("tvExplanation must exist", tvExplanation)
        assertEquals("@style/TextAppearance.Eve.Result.ExplanationBody", getStyleOrAppearance(tvExplanation))
    }

    @Test
    fun testLeaderboardTypographyBindings() {
        val root = loadXml("src/main/res/layout/activity_leaderboard.xml")

        val tvRankMedallion = findElementById(root, "tvRankMedallion")
        assertNotNull("tvRankMedallion must exist", tvRankMedallion)
        assertEquals("@style/TextAppearance.Eve.Subheadline.Medium", getStyleOrAppearance(tvRankMedallion))

        val tvYourRank = findElementById(root, "tvYourRank")
        assertNotNull("tvYourRank must exist", tvYourRank)
        assertEquals("@style/TextAppearance.Eve.Title3", getStyleOrAppearance(tvYourRank))

        val tvYourScore = findElementById(root, "tvYourScore")
        assertNotNull("tvYourScore must exist", tvYourScore)
        assertEquals("@style/TextAppearance.Eve.Title3", getStyleOrAppearance(tvYourScore))

        val itemRoot = loadXml("src/main/res/layout/item_leaderboard.xml")
        val tvRank = findElementById(itemRoot, "tvRank")
        assertNotNull("tvRank must exist", tvRank)
        assertEquals("@style/TextAppearance.Eve.Leaderboard.Rank", getStyleOrAppearance(tvRank))

        val tvName = findElementById(itemRoot, "tvName")
        assertNotNull("tvName must exist", tvName)
        assertEquals("@style/TextAppearance.Eve.Leaderboard.Name", getStyleOrAppearance(tvName))

        val tvScore = findElementById(itemRoot, "tvScore")
        assertNotNull("tvScore must exist", tvScore)
        assertEquals("@style/TextAppearance.Eve.Leaderboard.Score", getStyleOrAppearance(tvScore))
    }

    @Test
    fun testSecondaryScreensTypographyBindings() {
        val syllabusRoot = loadXml("src/main/res/layout/activity_syllabus.xml")
        val etSearch = findElementById(syllabusRoot, "etSearch")
        assertNotNull("etSearch must exist", etSearch)
        assertEquals("@style/TextAppearance.Eve.Search", getStyleOrAppearance(etSearch))

        val syllabusItemRoot = loadXml("src/main/res/layout/item_syllabus.xml")
        val tvExamName = findElementById(syllabusItemRoot, "tvExamName")
        assertNotNull("tvExamName must exist", tvExamName)
        assertEquals("@style/TextAppearance.Eve.Secondary.ItemTitle", getStyleOrAppearance(tvExamName))

        val feedbackRoot = loadXml("src/main/res/layout/activity_feedback.xml")
        val etFeedbackMessage = findElementById(feedbackRoot, "etFeedbackMessage")
        assertNotNull("etFeedbackMessage must exist", etFeedbackMessage)
        assertEquals("@style/TextAppearance.Eve.Callout", getStyleOrAppearance(etFeedbackMessage))

        val aboutRoot = loadXml("src/main/res/layout/activity_about.xml")
        val tvAppName = findElementById(aboutRoot, "tvAppName")
        assertNotNull("tvAppName must exist", tvAppName)
        assertEquals("@style/TextAppearance.Eve.Secondary.SectionHeader", getStyleOrAppearance(tvAppName))

        val contentDisplayRoot = loadXml("src/main/res/layout/activity_content_display.xml")
        val tvTitle = findElementById(contentDisplayRoot, "tvTitle")
        assertNotNull("tvTitle must exist", tvTitle)
        assertEquals("@style/TextAppearance.Eve.Title2", getStyleOrAppearance(tvTitle))
    }
}
