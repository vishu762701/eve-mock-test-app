package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.designsystem.DesignLoading
import com.eve.app.designsystem.TelegramFlickerSkeletonView
import com.eve.app.theme.TelegramThemeEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TelegramFlickerLoadingNativeTest {

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun flickerSkeletonRendersInBothLightAndDarkModes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            for (dark in listOf(false, true)) {
                lateinit var owner: FrameLayout
                lateinit var skeleton: TelegramFlickerSkeletonView
                scenario.onActivity { activity ->
                    val config = Configuration(activity.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    }
                    val themedContext = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.Theme_Eve)
                    owner = FrameLayout(themedContext)

                    skeleton = TelegramFlickerSkeletonView(themedContext).apply {
                        rowType = TelegramFlickerSkeletonView.RowType.HOME_EXAM
                        itemsCount = 4
                    }
                    owner.addView(skeleton, FrameLayout.LayoutParams(-1, -1))
                    activity.setContentView(owner)
                }

                instrumentation.waitForIdleSync()
                val latch = CountDownLatch(1)
                var pixelCopyResult = -1
                lateinit var bitmap: Bitmap

                scenario.onActivity { activity ->
                    assertTrue("Hardware acceleration is active", owner.isHardwareAccelerated)
                    bitmap = Bitmap.createBitmap(owner.width.coerceAtLeast(100), owner.height.coerceAtLeast(100), Bitmap.Config.ARGB_8888)
                    owner.postOnAnimation {
                        val loc = IntArray(2).also { owner.getLocationInWindow(it) }
                        val rect = android.graphics.Rect(loc[0], loc[1], loc[0] + owner.width, loc[1] + owner.height)
                        android.view.PixelCopy.request(
                            activity.window,
                            rect,
                            bitmap,
                            { code ->
                                pixelCopyResult = code
                                latch.countDown()
                            },
                            android.os.Handler(android.os.Looper.getMainLooper())
                        )
                    }
                }

                assertTrue("PixelCopy completes within deadline", latch.await(6, TimeUnit.SECONDS))
                assertEquals(android.view.PixelCopy.SUCCESS, pixelCopyResult)
                assertNotNull(bitmap)
            }
        }
    }

    @Test
    fun flickerSkeletonTransitionsFromLoadingToContent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val container = FrameLayout(activity)
                val skeleton = TelegramFlickerSkeletonView(activity).apply {
                    visibility = View.VISIBLE
                }
                val content = View(activity).apply {
                    visibility = View.GONE
                }
                container.addView(skeleton)
                container.addView(content)
                activity.setContentView(container)

                assertEquals(View.VISIBLE, skeleton.visibility)
                assertEquals(View.GONE, content.visibility)

                // Transition using DesignLoading
                DesignLoading.showContent(skeleton, content)

                assertEquals(View.GONE, skeleton.visibility)
                assertEquals(View.VISIBLE, content.visibility)
            }
        }
    }

    @Test
    fun flickerSkeletonAccessibilityAndTouchContract() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val skeleton = TelegramFlickerSkeletonView(activity)
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, skeleton.importantForAccessibility)
                assertFalse("Skeleton view must not be clickable", skeleton.isClickable)
                assertFalse("Skeleton view must not be focusable", skeleton.isFocusable)
            }
        }
    }

    @Test
    fun flickerSkeletonAllThreeRowTypesMeasureCorrectly() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val skeleton = TelegramFlickerSkeletonView(activity)
                for (type in TelegramFlickerSkeletonView.RowType.values()) {
                    skeleton.rowType = type
                    assertEquals(type, skeleton.rowType)
                    skeleton.measure(
                        View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY)
                    )
                    skeleton.layout(0, 0, 720, 1200)
                    assertEquals(720, skeleton.measuredWidth)
                    assertEquals(1200, skeleton.measuredHeight)
                }
            }
        }
    }
}
