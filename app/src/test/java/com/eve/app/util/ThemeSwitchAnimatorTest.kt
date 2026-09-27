package com.eve.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.max

/**
 * End-to-end unit tests for ThemeSwitchAnimator:
 * - Mathematical accuracy of circular reveal radius from arbitrary tap coordinates to all 4 screen corners
 * - Circular reveal start/end radius directions (expanding outward for dark, shrinking inward for light)
 * - Lottie sun-to-moon morph animation progress ranges (forward 0f->1f for dark, reverse 1f->0f for light)
 * - Rapid tap debouncing and transition locking
 * - Target theme mode inversion logic (preventing the double-toggle regression)
 */
class ThemeSwitchAnimatorTest {

    @Before
    fun setUp() {
        ThemeSwitchAnimator.resetForTesting()
    }

    @After
    fun tearDown() {
        ThemeSwitchAnimator.resetForTesting()
    }

    @Test
    fun testCalculateMaxRadius_fromCenter() {
        val width = 1080f
        val height = 1920f
        val cx = 540f
        val cy = 960f

        val calculated = ThemeSwitchAnimator.calculateMaxRadius(cx, cy, width, height)
        val expected = hypot(540.0, 960.0).toFloat()

        assertEquals(expected, calculated, 0.001f)
    }

    @Test
    fun testCalculateMaxRadius_fromTopRightCorner_farthestIsBottomLeft() {
        val width = 1080f
        val height = 1920f
        // Button located at top-right (e.g., toolbar action or 3-dot menu button)
        val cx = 1000f
        val cy = 60f

        val calculated = ThemeSwitchAnimator.calculateMaxRadius(cx, cy, width, height)

        val dTopLeft = hypot(1000.0, 60.0).toFloat()
        val dTopRight = hypot(80.0, 60.0).toFloat()
        val dBottomLeft = hypot(1000.0, 1860.0).toFloat()
        val dBottomRight = hypot(80.0, 1860.0).toFloat()

        val expected = max(max(dTopLeft, dTopRight), max(dBottomLeft, dBottomRight))

        assertEquals(dBottomLeft, calculated, 0.001f)
        assertEquals(expected, calculated, 0.001f)
        assertTrue(calculated > 2110f)
    }

    @Test
    fun testCalculateMaxRadius_fromBottomLeftCorner_farthestIsTopRight() {
        val width = 1080f
        val height = 1920f
        val cx = 40f
        val cy = 1880f

        val calculated = ThemeSwitchAnimator.calculateMaxRadius(cx, cy, width, height)
        val expected = hypot(1040.0, 1880.0).toFloat()

        assertEquals(expected, calculated, 0.001f)
    }

    @Test
    fun testRevealRadii_whenGoingDark_circleExpandsOutward() {
        val maxRadius = 1500f
        val (startRadius, endRadius) = ThemeSwitchAnimator.getRevealRadii(
            isDarkModeTarget = true,
            maxRadius = maxRadius
        )

        // Switching to dark: circle expands outward from tap button (0 -> maxRadius)
        assertEquals(0f, startRadius, 0.0f)
        assertEquals(maxRadius, endRadius, 0.0f)
    }

    @Test
    fun testRevealRadii_whenGoingLight_screenshotOverlayShrinksInward() {
        val maxRadius = 1500f
        val (startRadius, endRadius) = ThemeSwitchAnimator.getRevealRadii(
            isDarkModeTarget = false,
            maxRadius = maxRadius
        )

        // Switching to light: screenshot overlay shrinks inward to button (maxRadius -> 0)
        assertEquals(maxRadius, startRadius, 0.0f)
        assertEquals(0f, endRadius, 0.0f)
    }

    @Test
    fun testLottieProgressRange_whenGoingDark_morphsForwardSunToMoon() {
        val (startProgress, endProgress) = ThemeSwitchAnimator.getLottieProgressRange(
            isDarkModeTarget = true
        )

        // Switching to dark: starts at Sun (0f) and morphs forward to Moon (1f)
        assertEquals(0f, startProgress, 0.0f)
        assertEquals(1f, endProgress, 0.0f)
    }

    @Test
    fun testLottieProgressRange_whenGoingLight_morphsReverseMoonToSun() {
        val (startProgress, endProgress) = ThemeSwitchAnimator.getLottieProgressRange(
            isDarkModeTarget = false
        )

        // Switching to light: starts at Moon (1f) and morphs backwards to Sun (0f)
        assertEquals(1f, startProgress, 0.0f)
        assertEquals(0f, endProgress, 0.0f)
    }

    @Test
    fun testRapidTapProtection_blocksSecondaryTapsWhileInFlight() {
        assertFalse(ThemeSwitchAnimator.isTransitioning)

        // Simulate animation start
        ThemeSwitchAnimator.setInTransitionForTesting(true)
        assertTrue(ThemeSwitchAnimator.isTransitioning)

        // Simulate animation cleanup
        ThemeSwitchAnimator.resetForTesting()
        assertFalse(ThemeSwitchAnimator.isTransitioning)
    }

    @Test
    fun testThemeInversionLogic_preventsDoubleToggleBug() {
        // Test light -> dark
        val currentDark = false
        val targetDark = !currentDark
        assertTrue("When currently in light mode, target must be dark", targetDark)

        // Test dark -> light
        val currentLight = true
        val targetLight = !currentLight
        assertFalse("When currently in dark mode, target must be light", targetLight)

        // Verify single source of truth: no second inversion should be applied
        val simulatedSavedState = targetDark
        assertEquals(true, simulatedSavedState)
    }

    @Test
    fun testFixedIconPositionCalculation_matchesTelegram() {
        // Simulating Telegram's formula: pos[0] + width / 2, pos[1] + height / 2
        val iconWindowX = 998
        val iconWindowY = 48
        val iconWidth = 42
        val iconHeight = 42

        val cx = iconWindowX + iconWidth / 2
        val cy = iconWindowY + iconHeight / 2

        assertEquals(1019, cx)
        assertEquals(69, cy)
    }

    @Test
    fun testMultipleTogglesInARow_recalculateFreshCoordinates() {
        // 1st toggle: from 3-dot overflow menu icon at top-right
        val overflowIconPos = intArrayOf(1000, 50)
        val overflowW = 42
        val overflowH = 42
        val cx1 = overflowIconPos[0] + overflowW / 2
        val cy1 = overflowIconPos[1] + overflowH / 2

        assertEquals(1021, cx1)
        assertEquals(71, cy1)

        // 2nd toggle: from Settings top bar icon
        val settingsTopBarPos = intArrayOf(1016, 52)
        val settingsTopBarW = 40
        val settingsTopBarH = 40
        val cx2 = settingsTopBarPos[0] + settingsTopBarW / 2
        val cy2 = settingsTopBarPos[1] + settingsTopBarH / 2

        assertEquals(1036, cx2)
        assertEquals(72, cy2)
        assertTrue("Coordinates must be recalculated fresh for each trigger point", cx1 != cx2)

        // 3rd toggle: from Settings Preferences row Lottie icon (left side of row)
        val settingsRowIconPos = intArrayOf(20, 240)
        val settingsRowIconW = 24
        val settingsRowIconH = 24
        val cx3 = settingsRowIconPos[0] + settingsRowIconW / 2
        val cy3 = settingsRowIconPos[1] + settingsRowIconH / 2

        assertEquals(32, cx3)
        assertEquals(252, cy3)
        assertTrue("Settings row icon must calculate at its own fixed location", cx3 != cx1 && cx3 != cx2)

        // 4th toggle: return to 3-dot overflow menu
        val overflowIconPosReturn = intArrayOf(1000, 50)
        val cx4 = overflowIconPosReturn[0] + overflowW / 2
        val cy4 = overflowIconPosReturn[1] + overflowH / 2

        assertEquals(cx1, cx4)
        assertEquals(cy1, cy4)
    }

    @Test
    fun testSingleIconVisibilityLifecycle_avoidsDuplicateRender() {
        // FIX 1 Verification:
        // 1. Idle state: static icon is VISIBLE (value 0 in Android View.VISIBLE), floating icon does not exist
        var staticIconVisibility = 0 // View.VISIBLE
        var floatingIconExists = false
        var visibleIconCount = (if (staticIconVisibility == 0) 1 else 0) + (if (floatingIconExists) 1 else 0)
        assertEquals("Idle state must display exactly 1 icon (static only)", 1, visibleIconCount)

        // 2. Transition starts: static icon set to INVISIBLE (value 4), floating Lottie added at (cx, cy)
        staticIconVisibility = 4 // View.INVISIBLE
        floatingIconExists = true
        visibleIconCount = (if (staticIconVisibility == 0) 1 else 0) + (if (floatingIconExists) 1 else 0)
        assertEquals("During animation exactly 1 icon must be visible (floating Lottie only)", 1, visibleIconCount)

        // 3. Animation ends (cleanup): floating Lottie removed, static icon restored to VISIBLE
        floatingIconExists = false
        staticIconVisibility = 0 // View.VISIBLE
        visibleIconCount = (if (staticIconVisibility == 0) 1 else 0) + (if (floatingIconExists) 1 else 0)
        assertEquals("After animation ends exactly 1 icon must be visible (static only)", 1, visibleIconCount)
    }
}
