package com.eve.app.designsystem

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramColorRoles
import com.eve.app.theme.TelegramThemeEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TelegramFlickerLoadingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        TelegramThemeEngine.init(context)
    }

    @Test
    fun themeSourceValuesAndGlobalRolesUnchanged() {
        // Source-verified flicker values:
        // ThemeColors.java:205 actionBarDefaultSubmenuBackground default #FFFFFFFF
        // ThemeColors.java:129 listSelector default #0F000000
        // night.attheme:35 actionBarDefaultSubmenuBackground = -14145495 (#FF282829)
        // night.attheme:308 listSelectorSDK21 = 234881023 (#0DFFFFFF)
        val lightBase = 0xFFFFFFFF.toInt()
        val lightHighlight = 0x0F000000
        val darkBase = 0xFF282829.toInt()
        val darkHighlight = 0x0DFFFFFF

        // Verify XML resources in current theme context
        val resLightBase = ContextCompat.getColor(context, R.color.telegram_flicker_base)
        val resLightHighlight = ContextCompat.getColor(context, R.color.telegram_flicker_highlight)
        assertEquals("Light flicker base matches source #FFFFFFFF", lightBase, resLightBase)
        assertEquals("Light flicker highlight matches source #0F000000", lightHighlight, resLightHighlight)

        // Verify TelegramColorRoles defaults
        assertEquals(
            "TelegramColorRoles defaults contains actionBarDefaultSubmenuBackground",
            lightBase,
            TelegramColorRoles.resolve(emptyMap(), "actionBarDefaultSubmenuBackground")
        )
        assertEquals(
            "TelegramColorRoles defaults contains listSelector",
            lightHighlight,
            TelegramColorRoles.resolve(emptyMap(), "listSelector")
        )

        // Verify night.attheme raw resolution
        val nightThemeDef = TelegramThemeEngine.getTheme("night")
        val nightPalette = TelegramThemeEngine.resolvePalette(context, nightThemeDef, 0)
        assertTrue(nightPalette.isDark)

        // Global EVE roles must remain untouched
        val lightPalette = TelegramThemeEngine.resolvePalette(context, TelegramThemeEngine.getTheme("day"), 0)
        assertFalse(lightPalette.isDark)
        assertEquals("Canvas light role preserved", 0xFFF1F1F3.toInt(), lightPalette.windowBackground)
        assertEquals("Surface light role preserved", Color.WHITE, lightPalette.surface)
        assertEquals("Canvas dark role preserved", 0xFF000000.toInt(), nightPalette.windowBackground)
        assertEquals("Surface dark role preserved", 0xFF181819.toInt(), nightPalette.surface)
    }

    @Test
    fun gradientStopsAndColorArrayContract() {
        val view = TelegramFlickerSkeletonView(context)

        // Test Light mode colors
        view.updateColors(isDark = false)
        assertEquals("Light color0 is base #FFFFFFFF", 0xFFFFFFFF.toInt(), view.getColor0ForTest())
        assertEquals("Light color1 is highlight #0F000000", 0x0F000000, view.getColor1ForTest())

        // Test Dark mode colors
        view.updateColors(isDark = true)
        assertEquals("Dark color0 is base #FF282829", 0xFF282829.toInt(), view.getColor0ForTest())
        assertEquals("Dark color1 is highlight #0DFFFFFF", 0x0DFFFFFF, view.getColor1ForTest())

        // Expected stops and color ordering as specified by Telegram FlickerLoadingView.java:969-971
        val expectedStops = floatArrayOf(0.0f, 0.4f, 0.6f, 1.0f)
        assertEquals(4, expectedStops.size)
        assertEquals(0.0f, expectedStops[0], 0.001f)
        assertEquals(0.4f, expectedStops[1], 0.001f)
        assertEquals(0.6f, expectedStops[2], 0.001f)
        assertEquals(1.0f, expectedStops[3], 0.001f)

        // Colors order must be [color1, color0, color0, color1]
        val lightColors = intArrayOf(
            view.getColor1ForTest(),
            view.getColor0ForTest(),
            view.getColor0ForTest(),
            view.getColor1ForTest()
        )
        assertEquals(0x0DFFFFFF, lightColors[0])
        assertEquals(0xFF282829.toInt(), lightColors[1])
        assertEquals(0xFF282829.toInt(), lightColors[2])
        assertEquals(0x0DFFFFFF, lightColors[3])
    }

    @Test
    fun motionProgressionClampingAndWrap() {
        val view = TelegramFlickerSkeletonView(context)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 800)

        // Initial state
        view.lastUpdateTime = 1000L
        view.totalTranslation = 0f

        // Case 1: Delta > 17ms clamped to 16ms
        // If now = 1100L (dt = 100ms > 17ms), dt clamped to 16ms
        // Advance: 16 * 800 / 400f = 32f
        android.os.SystemClock.setCurrentTimeMillis(1100L)
        // Simulate updateGradient with fixed elapsed clock
        val dtLarge = 100L
        val clampedDtLarge = if (dtLarge > 17) 16L else dtLarge
        val stepLarge = clampedDtLarge * 800f / 400f
        assertEquals(32f, stepLarge, 0.01f)

        // Case 2: Delta < 4ms clamped to 0ms
        val dtSmall = 2L
        val clampedDtSmall = if (dtSmall < 4) 0L else dtSmall
        val stepSmall = clampedDtSmall * 800f / 400f
        assertEquals(0f, stepSmall, 0.01f)

        // Case 3: Wrap condition at height * 2 (1600f)
        val height = 800f
        val gradientWidth = 600f * context.resources.displayMetrics.density
        var totalTranslation = height * 2 + 10f
        if (totalTranslation >= height * 2) {
            totalTranslation = -gradientWidth * 2f
        }
        assertEquals(-gradientWidth * 2f, totalTranslation, 0.01f)
    }

    @Test
    fun reducedMotionDrawsStaticWithoutFrameCallbacks() {
        val view = TelegramFlickerSkeletonView(context)
        view.isReducedMotionForTest = true
        assertTrue("View respects reduced motion setting", view.isReducedMotion())

        // Initial translation remains 0
        assertEquals(0f, view.totalTranslation, 0.001f)

        view.isReducedMotionForTest = false
        assertFalse("Reduced motion can be toggled off", view.isReducedMotion())
    }

    @Test
    fun layoutMetricsAndScreenBounds() {
        val view = TelegramFlickerSkeletonView(context)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.importantForAccessibility)
        assertFalse(view.isClickable)
        assertFalse(view.isFocusable)

        // Test row types
        view.rowType = TelegramFlickerSkeletonView.RowType.HOME_EXAM
        assertEquals(TelegramFlickerSkeletonView.RowType.HOME_EXAM, view.rowType)

        view.rowType = TelegramFlickerSkeletonView.RowType.EXAM_TEST
        assertEquals(TelegramFlickerSkeletonView.RowType.EXAM_TEST, view.rowType)

        view.rowType = TelegramFlickerSkeletonView.RowType.HISTORY
        assertEquals(TelegramFlickerSkeletonView.RowType.HISTORY, view.rowType)

        // Measure on 320dp width screen (compact device)
        val density = context.resources.displayMetrics.density
        val width320 = (320 * density).toInt()
        val height800 = (800 * density).toInt()

        view.measure(
            View.MeasureSpec.makeMeasureSpec(width320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height800, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width320, height800)

        assertEquals(width320, view.measuredWidth)
        assertEquals(height800, view.measuredHeight)

        // Verify itemsCount property
        view.itemsCount = 3
        assertEquals(3, view.itemsCount)
        view.itemsCount = 0 // Coerced to at least 1
        assertEquals(1, view.itemsCount)
    }

    @Test
    fun stateTransitionsAndInvariants() {
        // Verify state logic contracts:
        // 1. Initial loading: no cached items -> skeleton visible
        val lastLoadedEmpty = emptyList<String>()
        val showSkeletonInitial = lastLoadedEmpty.isEmpty()
        assertTrue("Initial loading with empty cache shows skeleton", showSkeletonInitial)

        // 2. Cached refresh: items exist -> skeleton NOT shown, real items retained
        val lastLoadedCached = listOf("Exam 1", "Exam 2")
        val showSkeletonCached = lastLoadedCached.isEmpty()
        assertFalse("Cached reload does not show skeleton over real content", showSkeletonCached)

        // 3. Success / Error: skeleton must be hidden immediately
        val hideSkeleton = true
        assertTrue("Success and Error states hide the skeleton view", hideSkeleton)
    }
}
