package com.eve.app.ui

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eve.app.R
import com.eve.app.util.AppBulletin
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.navigation.EveBottomPillBar
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the production overlay in a real attached activity, not an XML-only fixture. */
@RunWith(AndroidJUnit4::class)
class BulletinHostNativeTest {
    private fun awaitCondition(scenario: ActivityScenario<RepairVerificationActivity>, description: String,
                               predicate: (RepairVerificationActivity) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 4000L
        var met = false
        while (!met && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { met = predicate(it) }
            if (!met) SystemClock.sleep(25)
        }
        assertTrue(description, met)
    }
    private fun bulletinCount(view: View): Int {
        val own = if (view.id == R.id.cardBulletin) 1 else 0
        return own + if (view is ViewGroup) (0 until view.childCount).sumOf { bulletinCount(view.getChildAt(it)) } else 0
    }
    private fun captureWindow(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val resolver = instrumentation.targetContext.contentResolver
                val filename = "$name.png"
                resolver.delete(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "${android.provider.MediaStore.Images.Media.DISPLAY_NAME} = ?", arrayOf(filename))
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-student-ui")
                }
                val uri = requireNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                requireNotNull(resolver.openOutputStream(uri)).use {
                    check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                }
            }
        } finally { screenshot.recycle() }
    }

    private fun verifyCompositedContrast(scenario: ActivityScenario<RepairVerificationActivity>) {
        val point = IntArray(2)
        var foreground = 0
        scenario.onActivity { activity ->
            val card = activity.findViewById<View>(R.id.cardBulletin)
            card.getLocationOnScreen(point)
            point[0] += (4 * activity.resources.displayMetrics.density).toInt()
            point[1] += card.height / 2
            foreground = activity.findViewById<TextView>(R.id.tvBulletinMessage).currentTextColor
        }
        val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            assertTrue(point[0] in 0 until screenshot.width && point[1] in 0 until screenshot.height)
            val background = screenshot.getPixel(point[0], point[1])
            val ratio = androidx.core.graphics.ColorUtils.calculateContrast(foreground, background)
            assertTrue("Composited bulletin text contrast is $ratio", ratio >= 4.5)
        } finally { screenshot.recycle() }
    }

    @Test fun recreationRemovesTransientBulletinWithoutRunningAction() {
        var actions = 0
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
                AppBulletin.make(activity, "Undo test", 30000L, actionText = "Undo", onAction = { actions++ }).show()
            }
            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertNull(activity.findViewById<View>(R.id.cardBulletin))
                assertEquals(0, actions)
            }
        }
    }

    @Test fun disabledAnimationsRemoveAnAttachedBulletinImmediately() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val key = android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
        val previous = android.provider.Settings.Global.getFloat(resolver, key, 1f)
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
        }
        try {
            shell("settings put global animator_duration_scale 0")
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                lateinit var bulletin: AppBulletin
                scenario.onActivity { activity ->
                    activity.setTheme(R.style.Theme_Eve)
                    activity.setContentView(FrameLayout(activity))
                    bulletin = AppBulletin.make(activity, "Reduced motion", 30000L)
                    bulletin.show()
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(250)
                scenario.onActivity { activity ->
                    assertTrue("Animator scale should be disabled", !com.eve.app.designsystem.DesignMotion.enabled(activity))
                    val card = activity.findViewById<View>(R.id.cardBulletin)
                    assertTrue(card.isLaidOut)
                    assertEquals(0f, card.translationY, 0f)
                    bulletin.dismiss()
                    assertNull(activity.findViewById<View>(R.id.cardBulletin))
                }
            }
        } finally { shell("settings put global animator_duration_scale $previous") }
    }

    @Test fun swipesDismissInBothDirections() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
            }
            for (direction in listOf(-1, 1)) {
                scenario.onActivity { activity -> AppBulletin.make(activity, "Swipe test", 30000L).show() }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(350)
                val bounds = IntArray(4)
                scenario.onActivity { activity ->
                    val card = activity.findViewById<View>(R.id.cardBulletin)
                    val position = IntArray(2)
                    card.getLocationOnScreen(position)
                    bounds[0] = position[0]; bounds[1] = position[1]
                    bounds[2] = card.width; bounds[3] = card.height
                }
                val startX = bounds[0] + bounds[2] / 2f
                val y = bounds[1] + bounds[3] / 2f
                fun swipe(fraction: Float) {
                    val downTime = SystemClock.uptimeMillis()
                    fun send(action: Int, x: Float) {
                        val event = android.view.MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                        try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
                    }
                    send(android.view.MotionEvent.ACTION_DOWN, startX)
                    for (step in 1..5) {
                        SystemClock.sleep(20)
                        send(android.view.MotionEvent.ACTION_MOVE, startX + direction * bounds[2] * fraction * step / 5f)
                    }
                    send(android.view.MotionEvent.ACTION_UP, startX + direction * bounds[2] * fraction)
                }
                swipe(0.2f)
                instrumentation.waitForIdleSync()
                awaitCondition(scenario, "Short swipe must return to its original position") { activity ->
                    activity.findViewById<View>(R.id.cardBulletin)?.let { kotlin.math.abs(it.translationX) < 1f } == true
                }
                scenario.onActivity { activity ->
                    val card = activity.findViewById<View>(R.id.cardBulletin)
                    assertNotNull(card)
                    assertEquals(0f, card.translationX, 1f)
                }
                swipe(0.45f)
                instrumentation.waitForIdleSync()
                awaitCondition(scenario, "Long swipe must dismiss the bulletin") { activity ->
                    activity.findViewById<View>(R.id.cardBulletin) == null
                }
                scenario.onActivity { activity -> assertNull(activity.findViewById<View>(R.id.cardBulletin)) }
            }
        }
    }

    @Test fun touchPausesOnlyTheRemainingTime() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
                AppBulletin.make(activity, "Pause test", 2500L).show()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(1000)
            val position = IntArray(2)
            val size = IntArray(2)
            scenario.onActivity { activity ->
                val card = activity.findViewById<View>(R.id.cardBulletin)
                card.getLocationOnScreen(position)
                size[0] = card.width; size[1] = card.height
            }
            val x = position[0] + size[0] / 2f
            val y = position[1] + size[1] / 2f
            val downTime = SystemClock.uptimeMillis()
            val down = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
            try { instrumentation.sendPointerSync(down) } finally { down.recycle() }
            SystemClock.sleep(1800)
            scenario.onActivity { activity -> assertNotNull(activity.findViewById<View>(R.id.cardBulletin)) }
            val up = android.view.MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_UP, x, y, 0)
            try { instrumentation.sendPointerSync(up) } finally { up.recycle() }
            SystemClock.sleep(2300)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity -> assertNull(activity.findViewById<View>(R.id.cardBulletin)) }
        }
    }

    @Suppress("DEPRECATION")
    @Test fun hindiWrapsInNarrowRtlWindowAtLargeFontScale() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val message = "यह परीक्षा की महत्वपूर्ण सूचना है। कृपया आगे बढ़ने से पहले सभी निर्देश ध्यान से पढ़ें।"
        var original: android.content.res.Configuration? = null
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    activity.setTheme(R.style.Theme_Eve)
                    val resources = activity.resources
                    original = android.content.res.Configuration(resources.configuration)
                    val scaled = android.content.res.Configuration(resources.configuration).apply { fontScale = 2f }
                    resources.updateConfiguration(scaled, resources.displayMetrics)
                    activity.window.setLayout((320 * resources.displayMetrics.density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
                    activity.setContentView(FrameLayout(activity))
                    val content = activity.findViewById<ViewGroup>(android.R.id.content)
                    content.layoutDirection = View.LAYOUT_DIRECTION_RTL
                    AppBulletin.make(activity, message, 30000L).show()
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(350)
                scenario.onActivity { activity ->
                    val card = activity.findViewById<View>(R.id.cardBulletin)
                    val text = activity.findViewById<TextView>(R.id.tvBulletinMessage)
                    assertEquals(message, text.text.toString())
                    assertEquals(View.LAYOUT_DIRECTION_RTL, text.layoutDirection)
                    assertTrue("Large text must wrap", text.lineCount > 1)
                    assertTrue("Large font scale must affect the text", text.textSize > 20 * activity.resources.displayMetrics.density)
                    assertTrue("Text must fit its measured height", text.height >= requireNotNull(text.layout).height)
                    assertTrue("Card must fit the narrow window", card.width <= (320 * activity.resources.displayMetrics.density).toInt())
                }
                captureWindow("bulletin-rtl-large")
            } finally {
                scenario.onActivity { activity ->
                    original?.let { activity.resources.updateConfiguration(it, activity.resources.displayMetrics) }
                }
            }
        }
    }

    @Test fun bulletinClearsImeWhenEmulatorProvidesOne() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                val container = FrameLayout(activity)
                val field = android.widget.EditText(activity)
                container.addView(field, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                activity.setContentView(container)
                field.requestFocus()
                field.post {
                    val input = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    input.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
                AppBulletin.make(activity, "Keyboard clearance", 30000L).show()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(1200)
            var visible = false
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val insets = ViewCompat.getRootWindowInsets(content)
                visible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
                if (visible) {
                    val card = activity.findViewById<View>(R.id.cardBulletin)
                    val cardPosition = IntArray(2)
                    val windowPosition = IntArray(2)
                    card.getLocationOnScreen(cardPosition)
                    activity.window.decorView.rootView.getLocationOnScreen(windowPosition)
                    val bottom = windowPosition[1] + activity.window.decorView.rootView.height
                    val keyboardTop = bottom - requireNotNull(insets).getInsets(WindowInsetsCompat.Type.ime()).bottom
                    assertTrue("Bulletin must clear the keyboard", cardPosition[1] + card.height <= keyboardTop)
                } else {
                    android.util.Log.w("EveBulletin", "MANUAL/UNKNOWN: emulator did not show an IME")
                }
            }
            if (visible) captureWindow("bulletin-ime")
        }
    }

    @Test fun veryLongMessageRemainsScrollableInsideVisibleViewport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val message = "परीक्षा की सूचना पढ़ें। ".repeat(100)
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
                AppBulletin.make(activity, message, 30000L).show()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(500)
            scenario.onActivity { activity ->
                val card = activity.findViewById<View>(R.id.cardBulletin)
                val scroll = activity.findViewById<android.widget.ScrollView>(R.id.bulletinMessageScroll)
                val text = activity.findViewById<TextView>(R.id.tvBulletinMessage)
                assertEquals(message, text.text.toString())
                assertTrue("Long message must be vertically scrollable", scroll.canScrollVertically(1))
                val position = IntArray(2)
                card.getLocationOnScreen(position)
                assertTrue("Card must remain within the window", position[1] >= 0)
            }
            captureWindow("bulletin-long-scroll")
        }
    }

    @Test fun visibleBulletinClearsAttachedFloatingPill() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val density = activity.resources.displayMetrics.density
                val pill = EveBottomPillBar(activity).apply { tag = "eve_bottom_pill_bar" }
                val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (56 * density).toInt()).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                    bottomMargin = (8 * density).toInt()
                }
                content.addView(pill, params)
                AppBulletin.make(activity, "परीक्षण सूचना", 30000L).show()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(350)
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val card = content.findViewById<View>(R.id.cardBulletin)
                val pill = requireNotNull(content.findViewWithTag<EveBottomPillBar>("eve_bottom_pill_bar"))
                val cardPosition = IntArray(2)
                val pillPosition = IntArray(2)
                card.getLocationOnScreen(cardPosition)
                pill.getLocationOnScreen(pillPosition)
                assertTrue("Bulletin must be above the pill", cardPosition[1] + card.height <= pillPosition[1])
            }
            captureWindow("bulletin-pill")
        }
    }

    @Test fun visibleBulletinRebindsDayAndNightRoles() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    activity.setTheme(R.style.Theme_Eve)
                    activity.setContentView(FrameLayout(activity))
                    TelegramThemeEngine.beginPreview()
                    TelegramThemeEngine.resetPreviewToDefaults(activity)
                    TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.DAY, activity)
                    AppBulletin.make(activity, "दृश्य संदेश", 30000L).show()
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(350)
                scenario.onActivity { activity ->
                    val card = activity.findViewById<MaterialCardView>(R.id.cardBulletin)
                    assertEquals(0xEA272F38.toInt(), card.cardBackgroundColor.defaultColor)
                }
                captureWindow("bulletin-day")
                verifyCompositedContrast(scenario)
                scenario.onActivity { activity ->
                    TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.NIGHT, activity)
                    val card = activity.findViewById<MaterialCardView>(R.id.cardBulletin)
                    assertEquals(0xF5181818.toInt(), card.cardBackgroundColor.defaultColor)
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(100)
                captureWindow("bulletin-night")
                verifyCompositedContrast(scenario)
                var customFile: java.io.File? = null
                try {
                    scenario.onActivity { activity ->
                        val file = java.io.File(activity.filesDir, "custom_${System.nanoTime().toString().replace("-", "")}.attheme")
                        customFile = file
                        file.writeText("windowBackgroundGray=-1\nundo_background=#AA123456\n")
                        val theme = requireNotNull(TelegramThemeEngine.registerCustomTheme(activity, file))
                        TelegramThemeEngine.setPreviewDayTheme(theme.id, 0, activity)
                        TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.DAY, activity)
                        val card = activity.findViewById<MaterialCardView>(R.id.cardBulletin)
                        assertEquals(0xAA123456.toInt(), card.cardBackgroundColor.defaultColor)
                    }
                    instrumentation.waitForIdleSync()
                    SystemClock.sleep(100)
                    captureWindow("bulletin-custom")
                    verifyCompositedContrast(scenario)
                } finally { customFile?.delete() }
            } finally {
                scenario.onActivity { TelegramThemeEngine.cancelPreview(it) }
            }
        }
    }
    @Test fun actionableBurstKeepsOneVisibleAndInvokesOnlyClickedAction() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var actions = 0
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
                AppBulletin.make(activity, "पहला परीक्षण संदेश", 10000L, actionText = "Undo", onAction = { actions++ }).show()
                AppBulletin.make(activity, "Second", 10000L, actionText = "Undo", onAction = { actions++ }).show()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(350)
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val card = content.findViewById<View>(R.id.cardBulletin)
                assertNotNull(card)
                assertTrue(card.isShown && card.width > 0 && card.height > 0)
                assertEquals(1, bulletinCount(content))
                val messageView = content.findViewById<TextView>(R.id.tvBulletinMessage)
                assertEquals("पहला परीक्षण संदेश", messageView.text.toString())
                assertEquals("पहला परीक्षण संदेश", messageView.createAccessibilityNodeInfo().text.toString())
                assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,
                    content.findViewById<View>(R.id.bulletinContainer).accessibilityLiveRegion)
                val action = content.findViewById<View>(R.id.btnBulletinAction)
                assertTrue(action.createAccessibilityNodeInfo().isClickable)
                val minTouch = 48 * activity.resources.displayMetrics.density
                assertTrue(action.width >= minTouch && action.height >= minTouch)
            }
            // UiAutomation captures the attached window, including actual compositing.
            captureWindow("bulletin-attached")
            val buttonLocation = IntArray(2)
            val buttonSize = IntArray(2)
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val button = content.findViewById<View>(R.id.btnBulletinAction)
                button.getLocationOnScreen(buttonLocation)
                buttonSize[0] = button.width; buttonSize[1] = button.height
            }
            val x = buttonLocation[0] + buttonSize[0] / 2f
            val y = buttonLocation[1] + buttonSize[1] / 2f
            val downTime = SystemClock.uptimeMillis()
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                val event = android.view.MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
                if (action == android.view.MotionEvent.ACTION_DOWN) SystemClock.sleep(30)
            }
            instrumentation.waitForIdleSync()
            awaitCondition(scenario, "Queued bulletin must appear") { activity ->
                activity.findViewById<TextView>(R.id.tvBulletinMessage)?.text?.toString() == "Second"
            }
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertEquals(1, actions)
                assertEquals(1, bulletinCount(content))
                assertEquals("Second", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
            }
        }
    }
}
