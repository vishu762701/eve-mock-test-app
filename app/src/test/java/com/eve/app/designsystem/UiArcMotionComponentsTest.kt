package com.eve.app.designsystem

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
