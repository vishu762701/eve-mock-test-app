package com.eve.app.ui.profile

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Device-executed proof of the adapted ARGB bitmap bridge to Telegram's kernel. */
@RunWith(AndroidJUnit4::class)
class ProfileStackBlurNativeTest {
    @Test fun softwareBitmapUsesSameOpaqueTriangularBlur() {
        val bitmap = Bitmap.createBitmap(3, 1, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setPixels(
                intArrayOf(0xff000000.toInt(), 0xff808080.toInt(), 0xffffffff.toInt()),
                0, 3, 0, 0, 3, 1
            )
            ProfileStackBlur.blurBitmap(bitmap, 1)
            val pixels = IntArray(3)
            bitmap.getPixels(pixels, 0, 3, 0, 0, 3, 1)
            assertArrayEquals(
                intArrayOf(0xff202020.toInt(), 0xff7f7f7f.toInt(), 0xffdfdfdf.toInt()),
                pixels
            )
        } finally {
            bitmap.recycle()
        }
    }
}
