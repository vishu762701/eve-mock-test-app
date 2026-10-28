package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Original Telegram draw semantics and window behavior, not whole sheet parity. */
class TelegramBottomSheetSourceScrimTest {
    @Test fun scrimUsesOfficialSourcePaintBoundsAndAlpha() {
        val source = File("src/main/java/com/eve/app/ui/home/TelegramBottomSheetSourceScrim.kt").readText()
        assertTrue(source.contains("Paint(Paint.ANTI_ALIAS_FLAG)"))
        assertTrue(source.contains("PorterDuffXfermode(PorterDuff.Mode.SRC)"))
        assertTrue(source.contains("color = Color.BLACK"))
        assertTrue(source.contains("val insets = Rect()"))
        assertTrue(source.contains("boundsWithInsets.left += maxOf(0, insets.left)"))
        assertTrue(source.contains("canvas.drawRect(boundsWithInsets, bgPaint)"))
        assertTrue(source.contains("override fun getAlpha(): Int = bgPaint.alpha"))
        assertTrue(source.contains("3de37c8cfb039452be7398909461da6288babf15"))
    }

    @Test fun targetSheetUsesOriginalQuint400msAlpha51InsteadOfWindowDim() {
        val source = File("src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt").readText()
        val patch = source.substringAfter("Source: BottomSheet.java:135-192")
            .substringBefore("dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED")
        assertTrue(patch.contains("TelegramBottomSheetSourceScrim()"))
        assertTrue(patch.contains("WindowManager.LayoutParams.FLAG_DIM_BEHIND"))
        assertTrue(patch.contains("window.clearFlags("))
        assertTrue(patch.contains("window.setDimAmount(0f)"))
        assertTrue(patch.contains("ValueAnimator.ofInt(0, 51)"))
        assertTrue(patch.contains("duration = 400L"))
        assertTrue(patch.contains("startDelay = 20L"))
        assertTrue(patch.contains("CubicBezierInterpolator.EASE_OUT_QUINT"))
        assertTrue(patch.contains("!UiArcMotion.isMotionEnabled(activity)"))
        assertTrue("Selection and secure persistence unchanged",
            source.contains("putStringSet(KEY_TARGET_EXAMS, selected)") &&
                source.contains("putBoolean(KEY_ONBOARDING_DONE, true)"))
    }
}
