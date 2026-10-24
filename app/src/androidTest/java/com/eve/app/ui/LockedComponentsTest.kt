package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.designsystem.DesignCardView
import com.eve.app.designsystem.DesignInputLayout
import com.eve.app.designsystem.DesignSegmentedTabs
import com.eve.app.designsystem.DesignSwitch
import com.eve.app.designsystem.ShadowHost
import com.google.android.material.textfield.TextInputEditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Native layout/drawing/state contracts for shared components, independent of app data. */
@RunWith(AndroidJUnit4::class)
class LockedComponentsTest {
    @Test fun inflaterAndExpandedTargetsKeepBindingsAndClicks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val clicks = java.util.concurrent.atomic.AtomicInteger(0)
        lateinit var owner: FrameLayout
        lateinit var target: View
        androidx.test.core.app.ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                owner = FrameLayout(activity)
                activity.setContentView(owner)
                val row = activity.layoutInflater.inflate(R.layout.item_exam, owner, false)
                assertTrue("Existing XML cards receive the shared host implementation", row is DesignCardView)
                com.eve.app.databinding.ItemExamBinding.bind(row)
                owner.addView(row)
                assertFalse(owner.clipChildren)
                owner.removeView(row)
                val density = activity.resources.displayMetrics.density
                target = View(activity).apply { setOnClickListener { clicks.incrementAndGet() } }
                owner.addView(target, FrameLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                    leftMargin = (56 * density).toInt(); topMargin = (56 * density).toInt()
                })
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                com.eve.app.designsystem.TouchTargets.ensure(target)
                val x = target.left - 2 * target.resources.displayMetrics.density
                val y = target.top + target.height / 2f
                val now = android.os.SystemClock.uptimeMillis()
                for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                    val event = android.view.MotionEvent.obtain(now, now, action, x, y, 0)
                    owner.dispatchTouchEvent(event)
                    event.recycle()
                }
            }
            instrumentation.waitForIdleSync()
            assertEquals("Expanded region preserves the existing listener and emits one click", 1, clicks.get())
            scenario.onActivity {
                owner.removeView(target)
                assertNull("Detached targets release their parent delegate", owner.touchDelegate)
                assertNull(owner.getTag(R.id.design_touch_group))
            }
        }
    }

    @Test fun switchAndInputRetainNativeStateAtLockedGeometryInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    fontScale = 1f
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                val resources = context.resources
                val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(context)
                assertEquals("Modal overlay must retain Mono rather than reset the theme",
                    context.getColor(R.color.accent), com.eve.app.designsystem.DesignTokens.color(
                        sheet.context, com.google.android.material.R.attr.colorPrimary))
                val modalFont = android.util.TypedValue()
                assertTrue(sheet.context.theme.resolveAttribute(android.R.attr.fontFamily, modalFont, true))
                assertEquals("Native Telegram text must remain in modal overlays",
                    "sans-serif", modalFont.string?.toString())
                sheet.dismiss()
                val toggle = DesignSwitch(context)
                toggle.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                toggle.layout(0, 0, toggle.measuredWidth, toggle.measuredHeight)
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_switch_width), toggle.width)
                assertTrue(toggle.height >= resources.getDimensionPixelSize(R.dimen.design_touch_min))
                var callbacks = 0
                toggle.setOnCheckedChangeListener { _, _ -> callbacks++ }
                toggle.performClick()
                assertTrue(toggle.isChecked)
                assertEquals(1, callbacks)
                val bitmap = Bitmap.createBitmap(toggle.width, toggle.height, Bitmap.Config.ARGB_8888)
                toggle.draw(Canvas(bitmap))
                val painted = Rect(toggle.width, toggle.height, 0, 0)
                for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                    if (Color.alpha(bitmap.getPixel(x, y)) != 0) {
                        painted.left = minOf(painted.left, x); painted.top = minOf(painted.top, y)
                        painted.right = maxOf(painted.right, x + 1); painted.bottom = maxOf(painted.bottom, y + 1)
                    }
                }
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_switch_width), painted.width())
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_switch_height), painted.height())
                bitmap.recycle()

                val input = DesignInputLayout(context)
                input.hint = "Password"
                val field = TextInputEditText(context).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                input.addView(field, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                assertEquals("Password", field.hint.toString())
                assertFalse(field.includeFontPadding)
                val sharedBox = com.google.android.material.textfield.TextInputLayout::class.java
                    .getDeclaredField("boxBackground").apply { isAccessible = true }.get(input)
                assertSame("Material installs the one shared box as the field background", sharedBox, field.background)
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_input_height), field.layoutParams.height)
                input.error = "Required"
                assertEquals("Required", input.error.toString())
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_focus_stroke), input.boxStrokeWidth)
                val strip = DesignSegmentedTabs(context)
                strip.addTab(strip.newTab().setText("First")); strip.addTab(strip.newTab().setText("Second"))
                strip.measure(View.MeasureSpec.makeMeasureSpec((320 * resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
                strip.layout(0, 0, strip.measuredWidth, strip.measuredHeight)
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_tab_height), strip.height)
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_tab_segment_height), strip.getChildAt(0).height)
                strip.getTabAt(1)!!.select()
                assertEquals(1, strip.selectedTabPosition)
                val owner = FrameLayout(context)
                val host = FrameLayout(context)
                val card = DesignCardView(context)
                owner.addView(host); host.addView(card)
                ShadowHost.releaseClipping(card)
                // The shadow host may unclip its immediate holder, never unrelated ancestors.
                assertTrue(owner.clipChildren); assertTrue(owner.clipToPadding)
                assertFalse(host.clipChildren); assertFalse(host.clipToPadding)
                ShadowHost.reserveTabStrip(strip, host)
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_tab_height), host.paddingTop)
            }
        }
    }
}
