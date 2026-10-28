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

    @Test fun bothTargetAndCompletedSheetsKeepTheOriginalOneSourceScrim() {
        val scrim = File("src/main/java/com/eve/app/ui/home/TelegramBottomSheetSourceScrim.kt").readText()
        val target = File("src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt").readText()
        val completed = File("src/main/java/com/eve/app/ui/common/CompletedExamBottomSheet.kt").readText()
        assertTrue(scrim.contains("internal object TelegramBottomSheetSourceBackdrop"))
        assertTrue(scrim.contains("TelegramBottomSheetSourceScrim().apply { alpha = 0 }"))
        assertTrue(scrim.contains("WindowManager.LayoutParams.FLAG_DIM_BEHIND"))
        assertTrue(scrim.contains("window.clearFlags("))
        assertTrue(scrim.contains("window.setDimAmount(0f)"))
        assertTrue(scrim.contains("ValueAnimator.ofInt(0, 51)"))
        assertTrue(scrim.contains("duration = 400L"))
        assertTrue(scrim.contains("startDelay = 20L"))
        assertTrue(scrim.contains("CubicBezierInterpolator.EASE_OUT_QUINT"))
        assertTrue(scrim.contains("!UiArcMotion.isMotionEnabled(context)"))
        assertTrue(scrim.contains("fade.cancel()"))
        assertTrue(target.contains("TelegramBottomSheetSourceBackdrop.install(dialog, activity)"))
        assertTrue(completed.contains("TelegramBottomSheetSourceBackdrop.install(dialog, activity)"))
        assertTrue("Selection state and onboarding persistence must remain",
            target.contains("putStringSet(KEY_TARGET_EXAMS, selected)") &&
            target.contains("putBoolean(KEY_ONBOARDING_DONE, true)"))
        assertTrue("Review and reattempt still invoke existing handlers",
            completed.contains("ResultDataHolder.setAnswers(att.answers)") &&
            completed.contains("onReattemptConfirmed()"))
    }
}
