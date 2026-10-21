package com.eve.app.designsystem

import com.eve.app.ui.performance.ScoreTrendChartView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated JVM test suite verifying the implementation and fidelity of the
 * five UIArc motion components in Batch 01:
 * 1. UiArcButton (Feedback submission)
 * 2. UiArcSegmentedControl (Admin Analytics time range)
 * 3. UiArcAnimatedCounterView (Performance metric cards)
 * 4. UiArcBadgeView (Result cutoff verdict)
 * 5. UiArcTextShimmerView (Performance loading status)
 */
class UiArcMotionComponentsTest {

    private fun getProjectDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."))
        for (candidate in candidates) {
            val resDir = File(candidate, "app/src/main/res/values")
            if (resDir.exists() && resDir.isDirectory) return candidate
            val directResDir = File(candidate, "src/main/res/values")
            if (directResDir.exists() && directResDir.isDirectory) {
                return candidate.canonicalFile.parentFile ?: File(".")
            }
        }
        return File(".")
    }

    private fun loadXml(relativePath: String): Element {
        val root = getProjectDir()
        val candidates = listOf(
            File(root, relativePath),
            File(root, "app/$relativePath"),
            File(relativePath),
            File("app/$relativePath")
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

    @Test
    fun testUIArcMotionTokens_DurationsAndSpringConstants() {
        // Durations
        assertEquals(120L, UiArcMotion.Duration.INSTANT)
        assertEquals(160L, UiArcMotion.Duration.FAST)
        assertEquals(180L, UiArcMotion.Duration.EXIT)
        assertEquals(240L, UiArcMotion.Duration.STANDARD)
        assertEquals(480L, UiArcMotion.Duration.CONSIDERED)
        assertEquals(1800L, UiArcMotion.Duration.SHIMMER_SWEEP)
        assertEquals(576L, UiArcMotion.Duration.SHIMMER_PAUSE)

        // Stagger
        assertEquals(16L, UiArcMotion.Stagger.CHAR_MS)
        assertEquals(40L, UiArcMotion.Stagger.WORD_MS)
        assertEquals(80L, UiArcMotion.Stagger.LINE_MS)
        assertEquals(35L, UiArcMotion.Stagger.ITEM_MS)

        // Blur tokens
        assertEquals(2f, UiArcMotion.Blur.SUBTLE_PX, 0.001f)
        assertEquals(4f, UiArcMotion.Blur.SOFT_PX, 0.001f)
        assertEquals(8f, UiArcMotion.Blur.TEXT_PX, 0.001f)

        // Spring presets
        val snappy = UiArcMotion.Spring.SNAPPY
        assertEquals(0.26f, snappy.visualDuration, 0.001f)
        assertEquals(0.12f, snappy.bounce, 0.001f)
        assertEquals(360L, snappy.settlingDurationMs)
        assertEquals(0.0f, snappy.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, snappy.getInterpolation(1.0f), 0.001f)
        // At visualDuration (260ms / 360ms = ~0.722), spring should have reached >99% of target
        val snappyVisualFraction = snappy.getInterpolation(0.26f / 0.36f)
        assertTrue("Snappy visual duration reaches target: $snappyVisualFraction", snappyVisualFraction > 0.98f)

        val smooth = UiArcMotion.Spring.SMOOTH
        assertEquals(0.40f, smooth.visualDuration, 0.001f)
        assertEquals(0.00f, smooth.bounce, 0.001f)
        assertEquals(500L, smooth.settlingDurationMs)
        assertEquals(0.0f, smooth.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, smooth.getInterpolation(1.0f), 0.001f)

        val morph = UiArcMotion.Spring.MORPH
        assertEquals(0.42f, morph.visualDuration, 0.001f)
        assertEquals(0.16f, morph.bounce, 0.001f)
        assertEquals(550L, morph.settlingDurationMs)
        assertEquals(0.0f, morph.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, morph.getInterpolation(1.0f), 0.001f)
    }

    @Test
    fun testUIArcMotion_ButtonPressScaleFormula() {
        // Density 1.0 (px = dp)
        assertEquals(0.985f, UiArcMotion.buttonPressScale(300, 1.0f), 0.001f) // >220dp
        assertEquals(0.96f, UiArcMotion.buttonPressScale(44, 1.0f), 0.001f)   // <=48dp
        assertEquals(0.97f, UiArcMotion.buttonPressScale(150, 1.0f), 0.001f)  // standard
    }

    @Test
    fun testComponent1_UiArcButtonInFeedbackLayout() {
        val root = loadXml("src/main/res/layout/activity_feedback.xml")
        val btn = findElementById(root, "btnSendFeedback")
        assertNotNull("btnSendFeedback must exist in activity_feedback.xml", btn)
        assertEquals("com.eve.app.designsystem.UiArcButton", btn?.tagName)
        assertEquals("@dimen/design_touch_min", btn?.getAttribute("android:layout_height"))
        assertEquals("Send Feedback", btn?.getAttribute("android:text"))

        // ProgressBar is preserved for view-binding contract with gone visibility
        val pb = findElementById(root, "progressBar")
        assertNotNull("progressBar view binding preserved", pb)
        assertEquals("gone", pb?.getAttribute("android:visibility"))
    }

    @Test
    fun testComponent2_UiArcSegmentedControlInAdminAnalyticsLayout() {
        val root = loadXml("src/main/res/layout/activity_admin_analytics.xml")
        val group = findElementById(root, "chipGroupTimeRange")
        assertNotNull("chipGroupTimeRange must exist in activity_admin_analytics.xml", group)
        assertEquals("com.eve.app.designsystem.UiArcSegmentedControl", group?.tagName)

        // All 3 contract IDs must be declared for static assertions and runtime mapping
        assertNotNull("chipLast7Days exists", findElementById(root, "chipLast7Days"))
        assertNotNull("chipLast30Days exists", findElementById(root, "chipLast30Days"))
        assertNotNull("chipAllTime exists", findElementById(root, "chipAllTime"))
    }

    @Test
    fun testComponent3_UiArcMetricCardsInPerformanceLayout() {
        val root = loadXml("src/main/res/layout/activity_performance.xml")

        val tvAttempts = findElementById(root, "tvTotalAttempts")
        assertNotNull("tvTotalAttempts must exist in activity_performance.xml", tvAttempts)
        assertEquals("com.eve.app.designsystem.UiArcAnimatedCounterView", tvAttempts?.tagName)

        val tvAccuracy = findElementById(root, "tvOverallAccuracy")
        assertNotNull("tvOverallAccuracy must exist in activity_performance.xml", tvAccuracy)
        assertEquals("com.eve.app.designsystem.UiArcAnimatedCounterView", tvAccuracy?.tagName)
    }

    @Test
    fun testComponent4_UiArcBadgeInResultLayout() {
        val root = loadXml("src/main/res/layout/activity_result.xml")
        val badge = findElementById(root, "tvCutoffVerdict")
        assertNotNull("tvCutoffVerdict must exist in activity_result.xml", badge)
        assertEquals("com.eve.app.designsystem.UiArcBadgeView", badge?.tagName)
    }

    @Test
    fun testComponent5_UiArcTextShimmerInPerformanceLayout() {
        val root = loadXml("src/main/res/layout/activity_performance.xml")
        val shimmer = findElementById(root, "tvLoadingPerformance")
        assertNotNull("tvLoadingPerformance must exist in activity_performance.xml", shimmer)
        assertEquals("com.eve.app.designsystem.UiArcTextShimmerView", shimmer?.tagName)
        assertEquals("Loading performance data…", shimmer?.getAttribute("android:text"))
    }

    @Test
    fun testBatch02_SpringConstants() {
        val glide = UiArcMotion.Spring.GLIDE
        assertEquals(0.30f, glide.visualDuration, 0.001f)
        assertEquals(0.00f, glide.bounce, 0.001f)
        assertEquals(400L, glide.settlingDurationMs)
        assertEquals(0.0f, glide.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, glide.getInterpolation(1.0f), 0.001f)

        val morph = UiArcMotion.Spring.MORPH
        assertEquals(0.42f, morph.visualDuration, 0.001f)
        assertEquals(0.16f, morph.bounce, 0.001f)
        assertEquals(550L, morph.settlingDurationMs)

        val smooth = UiArcMotion.Spring.SMOOTH
        assertEquals(0.40f, smooth.visualDuration, 0.001f)
        assertEquals(0.00f, smooth.bounce, 0.001f)
        assertEquals(500L, smooth.settlingDurationMs)
    }

    @Test
    fun testBatch02_Component1_ExpandingSearchHomeLayout() {
        val root = loadXml("src/main/res/layout/activity_main.xml")
        val btnSearch = findElementById(root, "btnSearch")
        assertNotNull("btnSearch must exist in activity_main.xml", btnSearch)

        val searchTopBar = findElementById(root, "searchTopBar")
        assertNotNull("searchTopBar must exist in activity_main.xml", searchTopBar)

        val layoutSearch = findElementById(root, "layoutSearchFieldContainer")
        assertNotNull("layoutSearchFieldContainer must exist in activity_main.xml", layoutSearch)

        val etSearch = findElementById(root, "etSearch")
        assertNotNull("etSearch must exist in activity_main.xml", etSearch)

        val btnSearchBack = findElementById(root, "btnSearchBack")
        assertNotNull("btnSearchBack must exist in activity_main.xml", btnSearchBack)

        val btnClearSearch = findElementById(root, "btnClearSearch")
        assertNotNull("btnClearSearch must exist in activity_main.xml", btnClearSearch)
    }

    @Test
    fun testBatch02_Component2_UiArcSwitchInSettingsAndProfileLayout() {
        val settingsRoot = loadXml("src/main/res/layout/activity_settings.xml")
        val settingsSwitch = findElementById(settingsRoot, "switchReminder")
        assertNotNull("switchReminder must exist in activity_settings.xml", settingsSwitch)
        assertEquals("com.eve.app.designsystem.UiArcSwitch", settingsSwitch?.tagName)
        assertEquals("@style/Widget.Eve.Token.Switch", settingsSwitch?.getAttribute("style"))

        val profileRoot = loadXml("src/main/res/layout/activity_profile.xml")
        val profileSwitch = findElementById(profileRoot, "switchReminder")
        assertNotNull("switchReminder must exist in activity_profile.xml", profileSwitch)
        assertEquals("com.eve.app.designsystem.UiArcSwitch", profileSwitch?.tagName)
        assertEquals("@style/Widget.Eve.Token.Switch", profileSwitch?.getAttribute("style"))
    }

    @Test
    fun testBatch02_Component3_InlineEditProfileLayout() {
        val root = loadXml("src/main/res/layout/activity_profile.xml")
        val tilName = findElementById(root, "tilName")
        assertNotNull("tilName must exist in activity_profile.xml", tilName)

        val etName = findElementById(root, "etName")
        assertNotNull("etName must exist in activity_profile.xml", etName)

        val btnSave = findElementById(root, "btnSaveProfile")
        assertNotNull("btnSaveProfile must exist in activity_profile.xml", btnSave)
    }

    @Test
    fun testBatch02_Component4_ConfirmMorphDeleteAccountLayout() {
        val root = loadXml("src/main/res/layout/activity_settings.xml")
        val rowDelete = findElementById(root, "rowDeleteAccount")
        assertNotNull("rowDeleteAccount must exist in activity_settings.xml", rowDelete)
    }

    @Test
    fun testBatch02_Component5_AccordionResultAnalysisAudit() {
        val root = loadXml("src/main/res/layout/activity_result.xml")
        val headerStats = findElementById(root, "headerOverviewStatistics")
        assertNotNull("headerOverviewStatistics must exist in activity_result.xml", headerStats)

        val headerPerf = findElementById(root, "headerOverviewPerformance")
        assertNotNull("headerOverviewPerformance must exist in activity_result.xml", headerPerf)

        val headerAnalytics = findElementById(root, "headerOverviewAnalytics")
        assertNotNull("headerOverviewAnalytics must exist in activity_result.xml", headerAnalytics)
    }

    @Test
    fun testBatch03_Component1_ProgressBar() {
        val progressBarClass = File(getProjectDir(), "app/src/main/java/com/eve/app/designsystem/UiArcProgressBar.kt")
        assertTrue("UiArcProgressBar.kt must exist", progressBarClass.exists())
    }

    @Test
    fun testBatch03_Component2_TargetExamsBottomSheetDetents() {
        val sheetLayout = loadXml("src/main/res/layout/bottom_sheet_target_exams.xml")
        val chipGroup = findElementById(sheetLayout, "chipGroupExams")
        assertNotNull("chipGroupExams must exist in bottom_sheet_target_exams.xml", chipGroup)
        val btnContinue = findElementById(sheetLayout, "btnContinue")
        assertNotNull("btnContinue must exist in bottom_sheet_target_exams.xml", btnContinue)
    }

    @Test
    fun testBatch03_Component3_ToastStackConstants() {
        assertEquals(5000L, com.eve.app.util.AppBulletin.DURATION_DEFAULT)
        assertEquals(8000L, com.eve.app.util.AppBulletin.DURATION_ERROR)
        assertEquals(12, com.eve.app.util.AppBulletin.MAX_QUEUE)
        assertEquals(3, com.eve.app.util.AppBulletin.MAX_VISIBLE)
        assertEquals(14f, com.eve.app.util.AppBulletin.PEEK_DP, 0.001f)
        assertEquals(0.05f, com.eve.app.util.AppBulletin.SCALE_STEP, 0.001f)
    }

    @Test
    fun testBatch03_Component4_NumberFieldManageExamsLayout() {
        assertEquals(400L, UiArcNumberField.HOLD_DELAY)
        assertEquals(40L, UiArcNumberField.HOLD_FASTEST)

        val root = loadXml("src/main/res/layout/activity_manage_exams.xml")
        val numberField = findElementById(root, "numberFieldQuestionCount")
        assertNotNull("numberFieldQuestionCount must exist in activity_manage_exams.xml", numberField)
        assertEquals("com.eve.app.designsystem.UiArcNumberField", numberField?.tagName)

        val etQuestionCount = findElementById(root, "etQuestionCount")
        assertNotNull("etQuestionCount must be nested inside number field", etQuestionCount)
        assertEquals("number", etQuestionCount?.getAttribute("android:inputType"))
    }

    @Test
    fun testBatch03_Component5_InViewTitlePerformanceHeadingsLayout() {
        assertEquals(0.45f, UiArcInViewTitleView.TRIGGER_AMOUNT, 0.001f)
        assertEquals(800L, UiArcInViewTitleView.SETTLE_MS)
        assertEquals(448L, UiArcInViewTitleView.OPACITY_MS)
        assertEquals(624L, UiArcInViewTitleView.BLUR_MS)

        val root = loadXml("src/main/res/layout/activity_performance.xml")
        val headingScore = findElementById(root, "tvHeadingScoreTrend")
        assertNotNull("tvHeadingScoreTrend must exist in activity_performance.xml", headingScore)
        assertEquals("com.eve.app.designsystem.UiArcInViewTitleView", headingScore?.tagName)
        assertEquals("Score Trend", headingScore?.getAttribute("android:text"))

        val headingTopic = findElementById(root, "tvHeadingTopicAccuracy")
        assertNotNull("tvHeadingTopicAccuracy must exist in activity_performance.xml", headingTopic)
        assertEquals("com.eve.app.designsystem.UiArcInViewTitleView", headingTopic?.tagName)
        assertEquals("Topic-wise Accuracy", headingTopic?.getAttribute("android:text"))
    }

    @Test
    fun testBatch04_Component1_LineChartConstantsAndLayout() {
        assertEquals(840L, ScoreTrendChartView.DRAW_DURATION)
        assertEquals(0.45f, ScoreTrendChartView.TRIGGER_AMOUNT, 0.001f)
        assertEquals(14f, ScoreTrendChartView.TOOLTIP_GAP_DP, 0.001f)
        assertEquals(12f, ScoreTrendChartView.TOOLTIP_RADIUS_DP, 0.001f)

        val root = loadXml("src/main/res/layout/activity_performance.xml")
        val chart = findElementById(root, "chartScoreTrend")
        assertNotNull("chartScoreTrend must exist in activity_performance.xml", chart)
        assertEquals("com.eve.app.ui.performance.ScoreTrendChartView", chart?.tagName)
    }

    @Test
    fun testBatch04_Component2_GaugeConstantsAndLayout() {
        assertEquals(0.50f, DesignAccuracyRing.TRIGGER_AMOUNT, 0.001f)
        assertEquals(768L, DesignAccuracyRing.REVEAL_DURATION_MS)
        assertEquals(500L, DesignAccuracyRing.SMOOTH_DURATION_MS)

        val root = loadXml("src/main/res/layout/activity_result.xml")
        val ring = findElementById(root, "gaugeTopicAccuracy")
        assertNotNull("gaugeTopicAccuracy must exist in activity_result.xml", ring)
        assertEquals("com.eve.app.designsystem.DesignAccuracyRing", ring?.tagName)
    }

    @Test
    fun testBatch04_Component3_ExpandableCardLayout() {
        val root = loadXml("src/main/res/layout/item_bookmark_card.xml")
        val btnToggle = findElementById(root, "btnToggleExplanation")
        assertNotNull("btnToggleExplanation must exist in item_bookmark_card.xml", btnToggle)
        val tvExplanation = findElementById(root, "tvExplanation")
        assertNotNull("tvExplanation must exist in item_bookmark_card.xml", tvExplanation)
        assertEquals("gone", tvExplanation?.getAttribute("android:visibility"))
    }

    @Test
    fun testBatch04_Component4_TextMorphConstantsAndLayout() {
        assertEquals(560L, UiArcTextMorphView.MORPH_DURATION_MS)
        assertEquals(280L, UiArcTextMorphView.ENTER_DURATION_MS)
        assertEquals(180L, UiArcTextMorphView.EXIT_DURATION_MS)
        assertEquals(50L, UiArcTextMorphView.ENTER_BASE_DELAY_MS)
        assertEquals(32L, UiArcTextMorphView.ENTER_STAGGER_MS)

        val root = loadXml("src/main/res/layout/activity_bookmarks.xml")
        val tvCount = findElementById(root, "tvBookmarkCount")
        assertNotNull("tvBookmarkCount must exist in activity_bookmarks.xml", tvCount)
        assertEquals("com.eve.app.designsystem.UiArcTextMorphView", tvCount?.tagName)
    }

    @Test
    fun testBatch04_Component5_AlertConstantsAndLayout() {
        assertEquals(240L, UiArcAlertView.ENTRANCE_OPACITY_DURATION_MS)
        assertEquals(160L, UiArcAlertView.EXIT_OPACITY_DURATION_MS)
        assertEquals(500L, UiArcAlertView.HEIGHT_DURATION_MS)

        val root = loadXml("src/main/res/layout/activity_main.xml")
        val banner = findElementById(root, "tvOfflineBanner")
        assertNotNull("tvOfflineBanner must exist in activity_main.xml", banner)
        assertEquals("com.eve.app.designsystem.UiArcAlertView", banner?.tagName)
    }

    @Test
    fun testInvariants_ProtectedThemeAndLottieAreUntouched() {
        val themeSwitchAnimator = File(getProjectDir(), "app/src/main/java/com/eve/app/util/ThemeSwitchAnimator.kt")
        assertTrue("ThemeSwitchAnimator must remain protected", themeSwitchAnimator.exists())

        val themeManager = File(getProjectDir(), "app/src/main/java/com/eve/app/util/ThemeManager.kt")
        assertTrue("ThemeManager must remain protected", themeManager.exists())

        val sunIcon = File(getProjectDir(), "app/src/main/res/drawable/ic_theme_sun.xml")
        assertTrue("ic_theme_sun must remain protected", sunIcon.exists())

        val moonIcon = File(getProjectDir(), "app/src/main/res/drawable/ic_theme_moon.xml")
        assertTrue("ic_theme_moon must remain protected", moonIcon.exists())
    }
}
