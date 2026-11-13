package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.eve.app.R
import com.eve.app.designsystem.DesignLoading
import com.eve.app.designsystem.TelegramFlickerSkeletonView
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
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            for (dark in listOf(false, true)) {
                val latch = CountDownLatch(1)
                var pixelCopyResult = -1
                var capturedBitmap: Bitmap? = null

                scenario.onActivity { activity ->
                    val config = Configuration(activity.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    }
                    val themedContext = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.Theme_Eve)
                    val owner = FrameLayout(themedContext)

                    val skeleton = TelegramFlickerSkeletonView(themedContext).apply {
                        rowType = TelegramFlickerSkeletonView.RowType.HOME_EXAM
                        itemsCount = 4
                    }
                    owner.addView(skeleton, FrameLayout.LayoutParams(-1, -1))
                    activity.setContentView(owner)

                    owner.post {
                        try {
                            assertTrue("Hardware acceleration is active", owner.isHardwareAccelerated)
                            val w = owner.width.coerceAtLeast(100)
                            val h = owner.height.coerceAtLeast(100)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            capturedBitmap = bmp
                            val loc = IntArray(2).also { owner.getLocationInWindow(it) }
                            val rect = android.graphics.Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
                            android.view.PixelCopy.request(
                                activity.window,
                                rect,
                                bmp,
                                { code ->
                                    pixelCopyResult = code
                                    latch.countDown()
                                },
                                android.os.Handler(android.os.Looper.getMainLooper())
                            )
                        } catch (_: Throwable) {
                            latch.countDown()
                        }
                    }
                }

                assertTrue("PixelCopy completes within deadline", latch.await(6, TimeUnit.SECONDS))
                assertEquals(android.view.PixelCopy.SUCCESS, pixelCopyResult)
                assertNotNull(capturedBitmap)
            }
        }
    }

    @Test
    fun flickerSkeletonTransitionsFromLoadingToContent() {
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

