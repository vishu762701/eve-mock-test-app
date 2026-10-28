package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Source identities are immutable: both the Git blob and SHA-256 must match. */
class TelegramDialogSourceAssetsTest {
    private data class Asset(val density: String, val size: Int, val sha256: String, val gitSha1: String)
    private val assets = listOf(
        Asset("mdpi", 1161, "6557266c5b8caf3e2a293dfb3083494a2db77d00f7a829387356dbd261fef99f", "53fa939aa586826cb9edcacd5624aca5e1eb12ea"),
        Asset("hdpi", 1903, "947bab8642f394213ebd617bd27e0187ce2b9d81eda9199b1bcde2a0c68b6f4e", "17d032ef07a97dede8c2edb5f0fa7dbdeae9b94b"),
        Asset("xhdpi", 2803, "b17e6b374f698950f81507a409891aec5385e20c549922762516f18f883952f2", "a1ccada709280d953a0ae7175d06efcbb1fb91fa"),
        Asset("xxhdpi", 4590, "9d78c7ec81e0938056a37177cef0899e338d817a23e7e4db4f820ec0d78a4505", "5275c8cd0ff41411cbf5621d56333395a7e871c8")
    )
    private fun hex(bytes: ByteArray) = bytes.joinToString("") {
        (it.toInt() and 255).toString(16).padStart(2, '0')
    }

    @Test fun dialogBackgroundImagesAreUnchangedOfficialTelegramSource() {
        for (asset in assets) {
            val file = File("src/main/res/drawable-${asset.density}/telegram_dialog_popup_fixed_alert4.9.png")
            assertTrue("Missing original Telegram dialog image: ${file.path}", file.isFile)
            val bytes = file.readBytes()
            assertEquals("Original byte count ${asset.density}", asset.size, bytes.size)
            assertEquals("Original SHA-256 ${asset.density}", asset.sha256,
                hex(MessageDigest.getInstance("SHA-256").digest(bytes)))
            val gitHash = MessageDigest.getInstance("SHA-1")
            gitHash.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
            gitHash.update(bytes)
            assertEquals("Original Git blob SHA-1 ${asset.density}", asset.gitSha1, hex(gitHash.digest()))
        }
    }

    @Test fun approvedDarkAlertDialogVariantPreservesSourceColorsAndDestructiveAction() {
        val helper = File("src/main/java/com/eve/app/ui/home/TelegramDialogSourceBackground.kt").readText()
        assertTrue(helper.contains("val surface = if (palette.isDark) 0xFF262626.toInt() else palette.dialogSurface"))
        assertTrue(helper.contains("android.R.id.message"))
        assertTrue(helper.contains("androidx.appcompat.R.id.alertTitle"))
        assertTrue(helper.contains("Color.WHITE"))
        assertTrue(helper.contains("dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)"))
        assertTrue("Do not override EVE's destructive positive button styling",
            !helper.contains("BUTTON_POSITIVE"))
    }

    @Test fun dialogTypographyMatchesOriginalSizesAndMediumFaceWithAccessibleScaling() {
        val source = File("src/main/java/com/eve/app/ui/home/TelegramDialogSourceBackground.kt").readText()
        val main = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        assertTrue("Original title uses 20-unit typography", source.contains("TypedValue.COMPLEX_UNIT_SP, 20f"))
        assertTrue("Original message and actions use 16-unit typography",
            source.contains("TypedValue.COMPLEX_UNIT_SP, 16f"))
        assertTrue("Original AndroidUtilities.bold maps to Telegram medium face",
            source.contains("FontManager.telegramMediumTypeface(dialog.context)"))
        assertTrue("Preserve the registered destructive confirmation behavior",
            main.contains("adminDeleteFlow.removeConfirmed(") &&
            main.contains(".showDestructive().also { dialog ->"))
        assertTrue("Don't shrink 48dp accessible buttons to Telegram's 40dp visual size",
            !source.contains("minimumHeight") && !source.contains("layoutParams.height"))
    }

    @Test fun homeAdminFeedbackDialogKeepsOriginalDestructiveActionsAndSourceRole() {
        val main = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val block = main.substringAfter("private fun handleFeedbackPostLongClick")
            .substringBefore("private var isReturningFromStopped")
        val helper = File("src/main/java/com/eve/app/ui/home/TelegramDialogSourceBackground.kt").readText()
        assertTrue(block.contains("adminRepo.isAdmin(email)"))
        assertTrue(block.contains("adminDeleteFlow.removeConfirmed"))
        assertTrue(block.contains(".showDestructive().also { dialog ->"))
        assertTrue(block.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(helper.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(helper.contains("palette.dialogSurface"))
        assertTrue(helper.contains("PorterDuff.Mode.MULTIPLY"))
        assertTrue(helper.contains("dialog.window?.setBackgroundDrawable(drawable)"))
    }
}
