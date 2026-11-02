package com.eve.app.ui.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.text.Spanned
import android.text.style.ImageSpan
import com.eve.app.telegram.qr.QrCardCaption
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrCardCaptionTest {
    @Test fun fittingUsesSourceFontSizesAndRealGlyphSpan() {
        val context = RuntimeEnvironment.getApplication()
        val sizes = (4..32).map { count ->
            val layout = QrCardCaption(context, "w".repeat(count)).layout(298)
            val spans = (layout.text as Spanned).getSpans(0, 1, ImageSpan::class.java)
            assertEquals(1, spans.size)
            assertTrue(spans.single().drawable.intrinsicWidth > 0)
            assertEquals(" " + "W".repeat(count), layout.text.toString())
            assertEquals(layout.text.length, layout.getLineEnd(layout.lineCount - 1))
            layout.paint.textSize
        }.toSet()
        assertEquals(setOf(30f, 25f, 19f), sizes)
    }

    @Test fun longCaptionRendersInsideOriginalBandWithoutTouchingQr() {
        val caption = QrCardCaption(RuntimeEnvironment.getApplication(), "w".repeat(32))
        val image = Bitmap.createBitmap(300, 321, Bitmap.Config.ARGB_8888)
        try {
            caption.draw(Canvas(image), RectF(0f, 0f, 298f, 318.86f), 244.36f, Color.BLACK)
            var pixels = 0
            for (y in 0 until image.height) for (x in 0 until image.width) {
                if (Color.alpha(image.getPixel(x, y)) > 0) {
                    assertTrue("caption pixel $x,$y", y >= 244 && y < 319 && x in 0..297)
                    pixels++
                }
            }
            assertTrue(pixels > 100)
        } finally { image.recycle() }
    }

    @Test fun usernameCapitalizationIsStableUnderTurkishLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            val layout = QrCardCaption(RuntimeEnvironment.getApplication(), "identity_i").layout(298)
            assertEquals(" IDENTITY_I", layout.text.toString())
        } finally { Locale.setDefault(previous) }
    }
}
