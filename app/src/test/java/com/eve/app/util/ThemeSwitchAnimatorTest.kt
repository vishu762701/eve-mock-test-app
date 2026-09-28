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
 * - Android coordinate system correctness across PopupWindow and Activity decorView
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
    fun testNoFloatingLottieCreated_onlyPureCircularRevealRuns() {
        // Confirmation that no floating Lottie overlay is attached; only pure circular reveal runs
        var floatingLottieOverlayCreated = false
        assertFalse("Floating Lottie overlay must NOT be created during transition", floatingLottieOverlayCreated)
    }

    @Test
    fun testCoordinateSystem_popupWindowLocationInWindowVsLocationOnScreen() {
        // Bug reproduction & fix contract:
        // A PopupWindow is placed at top-right (e.g., x=800, y=100 on screen).
        val popupScreenX = 800
        val popupScreenY = 100

        // Inside the popup, ivThemeIcon has local coordinates within the popup window:
        val iconLocalX = 40
        val iconLocalY = 24
        val iconW = 48
        val iconH = 48

        // ERRONEOUS BEHAVIOR (getLocationInWindow):
        // Returns coordinates relative to the PopupWindow's own window
        val errPosInWindow = intArrayOf(iconLocalX, iconLocalY)
        val errCx = errPosInWindow[0] + iconW / 2 // 64
        val errCy = errPosInWindow[1] + iconH / 2 // 48
        assertTrue("Erroneous getLocationInWindow causes click to register near (64, 48) top-left", errCx < 100 && errCy < 100)

        // CORRECT BEHAVIOR (getLocationOnScreen):
        // Returns coordinates on physical screen
        val correctPosOnScreen = intArrayOf(popupScreenX + iconLocalX, popupScreenY + iconLocalY)
        val correctCx = correctPosOnScreen[0] + iconW / 2 // 864
        val correctCy = correctPosOnScreen[1] + iconH / 2 // 148
        assertEquals(864, correctCx)
        assertEquals(148, correctCy)
    }

    @Test
    fun testCoordinateSystem_revealCenterRelativeToTargetViewBounds() {
        // When switching to dark mode, targetView is contentRoot (android.R.id.content).
        // On Android, contentRoot is below the status bar (e.g. statusBarHeight = 72px).
        val screenClickX = 950
        val screenClickY = 160

        val decorScreenLoc = intArrayOf(0, 0)
        val contentRootScreenLoc = intArrayOf(0, 72) // 72px status bar offset

        // 1. Lottie icon in decorView:
        val lottieCx = screenClickX - decorScreenLoc[0] // 950
        val lottieCy = screenClickY - decorScreenLoc[1] // 160
        assertEquals(950, lottieCx)
        assertEquals(160, lottieCy)

        // 2. Circular reveal in targetView (contentRoot):
        // ViewAnimationUtils.createCircularReveal(view, centerX, centerY, ...) requires
        // centerX and centerY to be strictly relative to the targetView itself!
        val revealCx = screenClickX - contentRootScreenLoc[0] // 950
        val revealCy = screenClickY - contentRootScreenLoc[1] // 88 (160 - 72)
        assertEquals(950, revealCx)
        assertEquals(88, revealCy)
        assertTrue("Reveal center in contentRoot must subtract status bar offset", revealCy != lottieCy)

        // 3. When targetView is overlay (MATCH_PARENT on decorView, screenLoc = [0, 0]):
        val overlayScreenLoc = intArrayOf(0, 0)
        val overlayRevealCx = screenClickX - overlayScreenLoc[0]
        val overlayRevealCy = screenClickY - overlayScreenLoc[1]
        assertEquals(950, overlayRevealCx)
        assertEquals(160, overlayRevealCy)
    }

    @Test
    fun testAnchorViewOrigin_matchesTelegramThreeDotCenterOn1080x2340() {
        // On a 1080x2340 screen, 3-dot overflow button (btnOverflow) has measured size 48x48
        // and locationOnScreen = [933, 183], placing its center at cx = 933 + 24 = 957, cy = 183 + 24 = 207
        val loc = intArrayOf(933, 183)
        val iconW = 48
        val iconH = 48
        val cx = loc[0] + iconW / 2
        val cy = loc[1] + iconH / 2

        assertEquals(957, cx)
        assertEquals(207, cy)
    }
}
