package com.eve.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.util.AvatarDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uploaded Telegram ThemeColors.java:161-174 / AvatarDrawable.java:571-577.
 * Exact gradient paint atom; EVE initials and name hash are ADAPTED. */
@RunWith(AndroidJUnit4::class)
class TelegramAvatarGradientNativeTest {
    @Test fun missingProfilePhotoRendersRealVerticalTelegramGradientAndRoundMask() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawable = AvatarDrawable.create(activity, "Ada Lovelace", "avatar-fixture")
                val size = 120
                val image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                try {
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(Canvas(image))
                    val top = image.getPixel(size / 2, size / 8)
                    val bottom = image.getPixel(size / 2, size * 7 / 8)
                    assertEquals("Avatar must be opaque inside circular bounds", 255, Color.alpha(top))
                    assertEquals(255, Color.alpha(bottom))
                    assertNotEquals("Solid accent is NOT Telegram's two-stop gradient", top, bottom)
                    assertEquals("Circular mask cannot paint outside avatar", Color.TRANSPARENT,
                        image.getPixel(0, 0))
                    val index = AvatarDrawable.getColorIndex("avatar-fixture")
                    assertTrue(index in AvatarDrawable.TELEGRAM_GRADIENTS.indices)
                } finally {
                    image.recycle()
                }
            }
        }
    }
}
