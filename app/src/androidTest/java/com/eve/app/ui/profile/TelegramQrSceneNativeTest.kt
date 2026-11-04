package com.eve.app.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        val layoutReady = CountDownLatch(1)
        Log.i("EveNativeQrStage", "scene-start")
        val intent = Intent(ApplicationProvider.getApplicationContext(), UsernameCardActivity::class.java)
            .setData(Uri.parse(link))
        ActivityScenario.launch<UsernameCardActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val target = descendants(root).filterIsInstance<TelegramQrSceneView>().single()
                // The source-derived reveal/emoji canvas may animate continuously.
                // A measured scene, not an idle message queue, is required for QR assertions.
                val layoutListener = object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                        if (right > left && bottom > top) {
                            v.removeOnLayoutChangeListener(this)
                            layoutReady.countDown()
                        }
                    }
                }
                target.addOnLayoutChangeListener(layoutListener)
                if (target.width > 0 && target.height > 0) {
                    target.removeOnLayoutChangeListener(layoutListener)
                    layoutReady.countDown()
                }
                Log.i("EveNativeQrStage", "scene-layout-wait-start")
            }
            assertTrue("QR scene never laid out", layoutReady.await(30, TimeUnit.SECONDS))
            Log.i("EveNativeQrStage", "scene-layout-ready")
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val scene = descendants(root).filterIsInstance<TelegramQrSceneView>().single()
                assertTrue("Real laid out QR canvas", scene.width > 250 && scene.height > 400)
                val controls = descendants(root).filter { it.isClickable && it.contentDescription != null }
                    .map { it.contentDescription.toString() }
                for (action in listOf("Close QR screen", "Scan EVE QR using live camera",
                        "Share QR image", "Save QR image", "Change QR color", "Change QR emoji", "Copy EVE profile link")) {
                    assertTrue("Missing actual accessible QR action: " + action, action in controls)
                }
                for (palette in 0..2) {
                    Log.i("EveNativeQrStage", "scene-palette-$palette-before-export")
                    scene.setPalette(palette)
                    val image = scene.renderShareImage()
                    assertNotNull(image)
                    assertEquals(scene.width, image!!.width)
                    assertEquals(minOf(scene.height, (scene.width * 1.92f).toInt()), image.height)
                    assertEquals("Rounded source-mapped QR scene must remain decodable",
                        link, decode(image!!))
                    image.recycle()
                    Log.i("EveNativeQrStage", "scene-palette-$palette-decoded")
                }
                scene.setPalette(2)
                val selected = QrEmojiPattern.standard("🔥")
                scene.setEmojiChoices(listOf(selected))
                assertEquals(listOf(selected), scene.emojiChoices)
                Log.i("EveNativeQrStage", "scene-emoji-before-export")
                val animatedScene = scene.renderShareImage()
                assertNotNull(animatedScene)
                assertEquals(link, decode(animatedScene!!))
                animatedScene.recycle()
                Log.i("EveNativeQrStage", "scene-emoji-decoded")
                scene.setEmojiChoices(listOf(QrEmojiPattern.custom("fixture-fire", "Fire", "res:raw/tg_fire_on")))
                scene.pausePlayback()
            }
            Log.i("EveNativeQrStage", "scene-before-recreate")
            scenario.recreate()
            scenario.onActivity { activity ->
                val scene = descendants(activity.findViewById(android.R.id.content))
                    .filterIsInstance<TelegramQrSceneView>().single()
                assertEquals("QR palette must survive configuration recreation", 2, scene.selectedPalette)
                Log.i("EveNativeQrStage", "scene-after-recreate")
                assertEquals("Animated EVE choice/source must survive configuration recreation",
                    listOf(QrEmojiPattern.custom("fixture-fire", "Fire", "res:raw/tg_fire_on")), scene.emojiChoices)
            }
        }
    }
}
