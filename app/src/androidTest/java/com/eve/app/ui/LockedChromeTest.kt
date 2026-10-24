package com.eve.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.PixelCopy
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.designsystem.DesignGlassLayout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Compositor evidence: live background blur must not soften or tint foreground children. */
class LockedChromeTest {
    @Test fun mediumGlassBlursLiveBackdropAndKeepsForegroundSharpInBothThemes() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            for (dark in listOf(false, true)) {
                lateinit var glass: DesignGlassLayout
                lateinit var backdrop: View
                var leftColor = 0
                var rightColor = 0
                var tint = 0
                var foreground = 0
                var density = 1f
                scenario.onActivity { activity ->
                    val config = android.content.res.Configuration(activity.resources.configuration).apply {
                        uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO
                    }
                    val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.Theme_Eve)
                    density = context.resources.displayMetrics.density
                    fun dp(value: Int) = (value * density + .5f).toInt()
                    leftColor = context.getColor(R.color.accent)
                    rightColor = context.getColor(R.color.surface)
                    tint = context.getColor(R.color.glass_tint)
                    foreground = context.getColor(R.color.system_green)
                    val owner = FrameLayout(activity)
                    owner.setBackgroundColor(context.getColor(R.color.bg_app))
                    backdrop = object : View(context) {
                        private val paint = Paint()
                        override fun onDraw(canvas: Canvas) {
                            paint.color = leftColor
                            canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), paint)
                            paint.color = rightColor
                            canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), paint)
                        }
                    }
                    fun position() = FrameLayout.LayoutParams(dp(160), dp(120)).apply { leftMargin = dp(16); topMargin = dp(16) }
                    owner.addView(backdrop, position())
                    glass = DesignGlassLayout(context).apply { backdropRoot = owner }
                    val sharp = View(context).apply { setBackgroundColor(foreground) }
                    glass.addView(sharp, FrameLayout.LayoutParams(dp(24), dp(24)).apply { leftMargin = dp(24); topMargin = dp(24) })
                    owner.addView(glass, position())
                    activity.setContentView(owner)
                }
                instrumentation.waitForIdleSync()
                fun capture(): Bitmap {
                    val ready = CountDownLatch(1)
                    var code = -1
                    lateinit var bitmap: Bitmap
                    scenario.onActivity { activity ->
                        assertTrue("Real backdrop blur requires a hardware window", glass.isHardwareAccelerated)
                        bitmap = Bitmap.createBitmap(glass.width, glass.height, Bitmap.Config.ARGB_8888)
                        // Allow source invalidation, pre-draw capture and the RenderNode frame to settle.
                        glass.postOnAnimation { glass.postOnAnimation { glass.postOnAnimation {
                            val location = IntArray(2)
                            glass.getLocationInWindow(location)
                            val rect = Rect(location[0], location[1], location[0] + glass.width, location[1] + glass.height)
                            PixelCopy.request(activity.window, rect, bitmap, { result -> code = result; ready.countDown() }, Handler(Looper.getMainLooper()))
                        } } }
                    }
                    assertTrue("Compositor capture completed", ready.await(8, TimeUnit.SECONDS))
                    assertEquals(PixelCopy.SUCCESS, code)
                    return bitmap
                }
                fun pixel(bitmap: Bitmap, x: Int, y: Int) = bitmap.getPixel((x * density).toInt(), (y * density).toInt())
                fun closeColor(expected: Int, actual: Int) {
                    for (channel in listOf<(Int) -> Int>(android.graphics.Color::red, android.graphics.Color::green, android.graphics.Color::blue))
                        assertTrue("Glass tint matches 55% rather than opaque/94% fallback: $expected / $actual", kotlin.math.abs(channel(expected) - channel(actual)) <= 10)
                }
                val first = capture()
                val expectedLeft = ColorUtils.compositeColors(tint, leftColor)
                val expectedRight = ColorUtils.compositeColors(tint, rightColor)
                closeColor(expectedLeft, pixel(first, 40, 80))
                closeColor(expectedRight, pixel(first, 120, 80))
                val center = android.graphics.Color.red(pixel(first, 80, 80))
                val lo = minOf(android.graphics.Color.red(expectedLeft), android.graphics.Color.red(expectedRight))
                val hi = maxOf(android.graphics.Color.red(expectedLeft), android.graphics.Color.red(expectedRight))
                assertTrue("A sharp source edge becomes a blurred transition", center > lo + 15 && center < hi - 15)
                assertEquals("Foreground stays sharp and untinted", foreground, pixel(first, 36, 36))
                first.recycle()
                scenario.onActivity {
                    leftColor = glass.context.getColor(R.color.system_blue)
                    backdrop.invalidate()
                }
                val changed = capture()
                closeColor(ColorUtils.compositeColors(tint, leftColor), pixel(changed, 40, 80))
                assertEquals(foreground, pixel(changed, 36, 36))
                changed.recycle()
            }
        }
    }
}
