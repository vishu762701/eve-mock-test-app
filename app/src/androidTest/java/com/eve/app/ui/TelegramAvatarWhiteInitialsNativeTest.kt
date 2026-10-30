package com.eve.app.ui

import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.util.AvatarDrawable
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Upstream DrKLO/Telegram@3de37c8: ThemeColors.java:155, AvatarDrawable.java:566.
 * EVE's stable user-id hashing is ADAPTED; initials foreground is EXACT white. */
@RunWith(AndroidJUnit4::class)
class TelegramAvatarWhiteInitialsNativeTest {
    @Test fun originalWhiteInitialsRenderOverEveryGradientFamily() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                for (index in AvatarDrawable.TELEGRAM_GRADIENTS.indices) {
                    val drawable = AvatarDrawable(activity, "E", index)
                    val size = (96f * activity.resources.displayMetrics.density).toInt()
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        size, size, android.graphics.Bitmap.Config.ARGB_8888)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(android.graphics.Canvas(bitmap))
                    try {
                        // The original gradient pairs contain no pure-white endpoint;
                        // white near the centered glyph therefore tests real ink,
                        // not a constant field or a mocked resource value.
                        var whitePixels = 0
                        for (y in size / 4 until size * 3 / 4) {
                            for (x in size / 4 until size * 3 / 4) {
                                val pixel = bitmap.getPixel(x, y)
                                if (Color.alpha(pixel) > 245 &&
                                    Color.red(pixel) > 248 &&
                                    Color.green(pixel) > 248 &&
                                    Color.blue(pixel) > 248) whitePixels++
                            }
                        }
                        assertTrue("Avatar initials must render genuine white ink for color $index",
                            whitePixels > 2)
                        assertTrue("Two-stop vertical gradient must retain distinct ends",
                            bitmap.getPixel(size / 2, size / 12) !=
                                bitmap.getPixel(size / 2, size * 11 / 12))
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }
}
