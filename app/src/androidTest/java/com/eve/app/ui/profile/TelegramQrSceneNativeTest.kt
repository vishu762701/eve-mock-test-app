package com.eve.app.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real native UI: public link still scans after rounded-cell/center-mark rendering. */
@RunWith(AndroidJUnit4::class)
class TelegramQrSceneNativeTest {
    private fun decode(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    private fun descendants(view: View): List<View> {
        val all = mutableListOf(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) all.addAll(descendants(view.getChildAt(i)))
        }
        return all
    }

    @Test fun sourceGeometrySceneIsScannableAndNativeControlsAreAccessible() {
        val link = "eve-mock-test://username/test_name"
        val intent = Intent(ApplicationProvider.getApplicationContext(), UsernameCardActivity::class.java)
            .setData(Uri.parse(link))
        ActivityScenario.launch<UsernameCardActivity>(intent).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val scene = descendants(root).filterIsInstance<TelegramQrSceneView>().single()
                assertTrue("Real laid out QR canvas", scene.width > 250 && scene.height > 400)
                val controls = descendants(root).filter { it.isClickable && it.contentDescription != null }
                    .map { it.contentDescription.toString() }
                for (action in listOf("Close QR screen", "Scan EVE QR using camera photo",
                        "Share QR image", "Save QR image", "Change QR color", "Copy EVE profile link")) {
                    assertTrue("Missing actual accessible QR action: " + action, action in controls)
                }
                for (palette in 0..2) {
                    scene.setPalette(palette)
                    val image = scene.renderShareImage()
                    assertNotNull(image)
                    assertEquals("Rounded source-mapped QR scene must remain decodable",
                        link, decode(image!!))
                    image.recycle()
                }
                scene.setPalette(2)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val scene = descendants(activity.findViewById(android.R.id.content))
                    .filterIsInstance<TelegramQrSceneView>().single()
                assertEquals("QR palette must survive configuration recreation", 2, scene.selectedPalette)
            }
        }
    }
}
