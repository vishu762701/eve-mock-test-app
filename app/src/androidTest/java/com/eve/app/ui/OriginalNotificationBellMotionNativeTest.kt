package com.eve.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.eve.app.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A real frame-difference test of the historical EVE bell composition, not a mock Lottie. */
@RunWith(AndroidJUnit4::class)
class OriginalNotificationBellMotionNativeTest {
    @Test fun originalAssetRendersVisibleMotionBetweenHistoricalKeyframes() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val composition = LottieCompositionFactory.fromRawResSync(
                    activity, R.raw.notification_bell).value
                assertNotNull("Historical notification_bell.json must parse", composition)
                val bell = LottieAnimationView(activity)
                bell.setComposition(composition!!)
                activity.setContentView(bell)
                val size = (96f * activity.resources.displayMetrics.density).toInt()
                bell.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY))
                bell.layout(0, 0, size, size)
                fun frame(progress: Float): Bitmap {
                    bell.progress = progress
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    bell.draw(Canvas(bitmap))
                    return bitmap
                }
                val early = frame(0.10f)
                val later = frame(0.35f)
                var opaque = 0
                var changed = 0
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val a = early.getPixel(x, y)
                        val b = later.getPixel(x, y)
                        if (Color.alpha(a) > 25 || Color.alpha(b) > 25) opaque++
                        if (a != b) changed++
                    }
                }
                assertTrue("Animated bell needs actual visible pixels", opaque > 20)
                assertTrue("Historically animated ring must change between frames", changed > 3)
                early.recycle()
                later.recycle()
            }
        }
    }
}
