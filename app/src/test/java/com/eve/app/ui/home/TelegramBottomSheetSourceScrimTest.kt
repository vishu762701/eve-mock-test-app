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
            completed.contains("ResultDataHolder.setAnswers(att.answers, ownerUid, att.examId)") &&
            completed.contains("onReattemptConfirmed()"))
    }

    @Test
    fun appWideSheetsUseTelegramBottomSheetBackdropAndTheme() {
        val activityLog = File("src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt").readText()
        val emojiStudio = File("src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt").readText()
        val result = File("src/main/java/com/eve/app/ui/result/ResultActivity.kt").readText()
        val usernameCard = File("src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt").readText()
        val paymentMethod = File("src/main/java/com/eve/app/ui/premium/PaymentMethodBottomSheet.kt").readText()

        assertTrue("ActivityLog must install backdrop", activityLog.contains("TelegramBottomSheetSourceBackdrop.install(dialog, this)"))
        assertTrue("ActivityLog must use theme", activityLog.contains("Theme_Eve_BottomSheetDialog"))

        assertTrue("EmojiStudio must install backdrop", emojiStudio.contains("TelegramBottomSheetSourceBackdrop.install(dialog, this)"))
        assertTrue("EmojiStudio must use theme", emojiStudio.contains("Theme_Eve_BottomSheetDialog"))

        assertTrue("Result must install backdrop", result.contains("TelegramBottomSheetSourceBackdrop.install(dialog, this)"))
        assertTrue("Result must use theme", result.contains("Theme_Eve_BottomSheetDialog"))

        assertTrue("UsernameCard must install backdrop", usernameCard.contains("TelegramBottomSheetSourceBackdrop.install(sheet, this)"))
        assertTrue("UsernameCard must use theme", usernameCard.contains("Theme_Eve_BottomSheetDialog"))

        assertTrue("PaymentMethod must use theme", paymentMethod.contains("Theme_Eve_BottomSheetDialog"))
        assertTrue("PaymentMethod must install backdrop", paymentMethod.contains("TelegramBottomSheetSourceBackdrop.install(dialog, ctx)"))
    }
}
