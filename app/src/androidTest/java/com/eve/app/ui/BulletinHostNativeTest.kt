package com.eve.app.ui

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
                val downTime = SystemClock.uptimeMillis()
                fun send(action: Int, x: Float) {
                    val event = android.view.MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                    try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
                }
                send(android.view.MotionEvent.ACTION_DOWN, startX)
                for (step in 1..5) {
                    SystemClock.sleep(20)
                    send(android.view.MotionEvent.ACTION_MOVE, startX + direction * bounds[2] * 0.45f * step / 5f)
                }
                send(android.view.MotionEvent.ACTION_UP, startX + direction * bounds[2] * 0.45f)
                instrumentation.waitForIdleSync()
                SystemClock.sleep(300)
                scenario.onActivity { activity -> assertNull(activity.findViewById<View>(R.id.cardBulletin)) }
            }
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
                scenario.onActivity { activity ->
                    TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.NIGHT, activity)
                    val card = activity.findViewById<MaterialCardView>(R.id.cardBulletin)
                    assertEquals(0xF5181818.toInt(), card.cardBackgroundColor.defaultColor)
                }
                instrumentation.waitForIdleSync()
                captureWindow("bulletin-night")
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
                assertEquals("पहला परीक्षण संदेश", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
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
            SystemClock.sleep(300)
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertEquals(1, actions)
                assertEquals(1, bulletinCount(content))
                assertEquals("Second", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
            }
        }
    }
}
