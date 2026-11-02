package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.PixelCopy
import android.view.View
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.ui.RepairVerificationActivity
import com.eve.app.R
import com.eve.app.telegram.qr.QrScannerControl
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real HWUI production compositor test with synthetic pixels, no camera permission or private content.
 * This does not establish actual CameraX TextureView/crop fidelity or Telegram reference parity.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class QrCameraGlassNativeTest {
    private class Stripes(context: Context) : View(context) {
        private val paint = Paint()
        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            paint.color = Color.WHITE
            for (x in 0 until width step 8) canvas.drawRect(x.toFloat(), 0f, x + 4f, height.toFloat(), paint)
        }
    }

    @Test fun hardwareCameraLayerBlursBelowActualGlassAndDisposesOnDetach() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var root: QrScanMorphLayout
            lateinit var control: View
            val frame = CountDownLatch(1)
            scenario.onActivity { activity ->
                root = QrScanMorphLayout(activity)
                val preview = PreviewView(activity).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    addView(Stripes(activity), FrameLayout.LayoutParams(-1, -1))
                }
                root.addView(preview, FrameLayout.LayoutParams(-1, -1))
                val density = activity.resources.displayMetrics.density
                val side = (72 * density).toInt()
                control = QrScannerControl(activity,
                    requireNotNull(activity.getDrawable(R.drawable.eve_qr_flashlight)),
                    true, 56, root.scannerGlass(28 * density))
                root.addView(control, FrameLayout.LayoutParams(side, side, Gravity.CENTER))
                activity.setContentView(root)
                root.setAlreadyOpened()
                root.setPreviewStreaming(true)
                root.postOnAnimation { root.invalidate(); root.postOnAnimation { frame.countDown() } }
            }
            assertTrue("Hardware frame scheduled", frame.await(5, TimeUnit.SECONDS))
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val copied = CountDownLatch(1)
            var result = -1
            lateinit var pixels: Bitmap
            scenario.onActivity { activity ->
                assertTrue("Actual hardware window", root.isHardwareAccelerated)
                val xy = IntArray(2)
                control.getLocationInWindow(xy)
                // Lower half avoids the source top sheen while staying away from the rim.
                val x = xy[0] + control.width / 2
                val y = xy[1] + control.height * 3 / 4
                pixels = Bitmap.createBitmap(16, 4, Bitmap.Config.ARGB_8888)
                PixelCopy.request(activity.window, Rect(x - 8, y - 2, x + 8, y + 2), pixels,
                    { code -> result = code; copied.countDown() }, Handler(Looper.getMainLooper()))
            }
            assertTrue("PixelCopy completed", copied.await(5, TimeUnit.SECONDS))
            assertEquals(PixelCopy.SUCCESS, result)
            try {
                // Unblurred 4px stripes contain black and white. The original 32dp
                // DECAL camera node averages both under the source white glass fill.
                val values = (0 until 16).map { Color.red(pixels.getPixel(it, 2)) }
                assertTrue("Blur removed the black/white stripes: $values", values.max() - values.min() < 24)
                assertTrue("Real camera content contributes: $values", values.all { it in 80..230 })
            } finally { pixels.recycle() }
            scenario.onActivity { activity ->
                root.setPreviewStreaming(false)
                root.dispose()
                root.dispose()
                activity.setContentView(FrameLayout(activity))
            }
        }
    }
}
