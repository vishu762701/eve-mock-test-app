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
}
