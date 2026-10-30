package com.eve.app.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Contract verifying Admin UI dialogs use verified Telegram source chrome and proper styling. */
class AdminDialogSourceContractTest {

    @Test fun adminActivityUsesNativeSourceIsolatedHostForAllOwnedDialogVariants() {
        val source = File("src/main/java/com/eve/app/ui/admin/AdminActivity.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertEquals("Original confirmation, list, custom-form and progress variants",
            22, source.split("TelegramNativeAlertDialog.Builder(").size - 1)
        assertTrue("Do not overlay native dialog with obsolete AppCompat background",
            !source.contains("TelegramDialogSourceBackground.apply("))
        assertTrue("Floating link form validation and server authorization remain",
            source.contains("FloatingLinkRepository.isValidHttpsUrl(finalUrl)") &&
                source.contains("floatingLinkRepo.updateFloatingLink(finalUrl)"))
        assertTrue("Feedback and banner operations remain connected",
            source.contains("feedbackRepo.createFeedbackPost(") &&
                source.contains("bannerRepo."))
        assertTrue("Destructive actions remain visibly red",
            source.contains(".makeRed(DialogInterface.BUTTON_POSITIVE).show()"))
    }

    @Test fun manageExamsUsesFourteenNativeSourceDerivedDialogsWithCancelHandling() {
        val source = File("src/main/java/com/eve/app/ui/admin/ManageExamsActivity.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertEquals("All 14 exam-management confirmations and notices must use the native host",
            14, source.split("TelegramNativeAlertDialog.Builder(").size - 1)
        assertTrue("The 14 native confirmations must show without Material adapters",
            source.split(".show()").size - 1 >= 14)
        assertTrue(source.contains(".setOnCancelListener {"))
        assertTrue(source.contains("triggerAiTestGeneration(data)"))
        assertTrue(source.contains("saveExamSettings {"))
        val native = File("src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt").readText()
        assertTrue(native.contains("dialog.setOnCancelListener(cancelListener)"))
    }

    @Test fun adminDeleteFlowAppliesDestructiveAndSourceStyling() {
        val source = File("src/main/java/com/eve/app/ui/admin/AdminDeleteFlow.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertTrue(source.contains("TelegramNativeAlertDialog.Builder(activity)"))
        assertTrue(source.contains(".makeRed(DialogInterface.BUTTON_POSITIVE).show()"))
        assertTrue(source.contains("if (!pending.add(key)) return"))
        assertTrue(source.contains("repository.deleteExam(id)"))
        assertTrue(source.contains("repository.deleteGeneratedTest(id)"))
        assertTrue(source.contains("if (!processing) pending.remove(key)"))
    }

    @Test fun secondaryAdminActivitiesRouteDialogsThroughSourceAdapters() {
        val activities = listOf(
            "src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt",
            "src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt",
            "src/main/java/com/eve/app/ui/admin/AdminAnalyticsActivity.kt"
        )
        // The read-only generated-test preview now uses the actual source-derived
        // native message scroll host instead of the previous Material adapter.
        val preview = File("src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt").readText()
        assertTrue(preview.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(preview.contains(".setMessage(formattedText.toString())"))
        assertTrue(preview.contains(".setPositiveButton(\"Close\", null)"))
        assertTrue(preview.contains(".show()"))
        val editAbout = File("src/main/java/com/eve/app/ui/admin/EditAboutActivity.kt").readText()
        assertTrue(editAbout.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertEquals(2, editAbout.split("TelegramNativeAlertDialog.Builder(").size - 1)
        assertTrue(editAbout.contains(".setNeutralButton(\"Cancel\""))
        for (path in activities) {
            val content = File(path).readText()
            assertTrue("$path must import native Telegram host", content.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
            assertTrue("$path must use native Telegram host", content.contains("TelegramNativeAlertDialog.Builder("))
            assertTrue("$path must not open the obsolete Material adapter", !content.contains(".showTelegramSourceDialog()"))
        }
        val syllabus = File(activities[0]).readText()
        assertTrue("Choosing a new exam must not dismiss before Reassign",
            syllabus.contains(".setSingleChoiceItems(examLabels, 0, dismissOnSelection = false)"))
        assertTrue(syllabus.contains("examRepo.updateExamFullSettings("))
        val users = File(activities[1]).readText()
        assertTrue(users.contains("adminRepo.toggleUserStatus(user.id, disable)"))
        assertTrue(users.contains("authorizedUid != FirebaseAuth.getInstance().currentUser?.uid"))
        val analytics = File(activities[2]).readText()
        assertTrue(analytics.contains("examRepo.updateQuestion(updated)"))
    }

    @Test fun publishingPremiumAndModerationDialogsUseNativeHostWithoutChangingAuthority() {
        val files = listOf(
            "SendNotificationActivity.kt",
            "ManagePremiumActivity.kt",
            "FlaggedQuestionsActivity.kt"
        )
        for (name in files) {
            val src = File("src/main/java/com/eve/app/ui/admin/$name").readText()
            assertTrue("$name must use Telegram-native UI", src.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
            assertTrue("$name must no longer open the legacy Material adapter", !src.contains("showTelegramSourceDialog()"))
        }
        val notify = File("src/main/java/com/eve/app/ui/admin/SendNotificationActivity.kt").readText()
        assertEquals(3, notify.split("TelegramNativeAlertDialog.Builder(this)").size - 1)
        assertEquals(2, notify.split(".makeRed(DialogInterface.BUTTON_POSITIVE)").size - 1)
        assertTrue(notify.contains("deleteSelectedBroadcasts()"))
        assertTrue(notify.contains("deleteBroadcast(broadcast)"))
        assertTrue(notify.contains("sendNotification(title, message)"))
        val premium = File("src/main/java/com/eve/app/ui/admin/ManagePremiumActivity.kt").readText()
        assertTrue(premium.contains("ApiClient.api.grantPremium("))
        assertTrue(premium.contains("ApiClient.api.revokePremium("))
        assertTrue(premium.contains(".setView(dialogView)"))
        assertTrue(premium.contains(".makeRed(DialogInterface.BUTTON_POSITIVE)"))
        val flagged = File("src/main/java/com/eve/app/ui/admin/FlaggedQuestionsActivity.kt").readText()
        assertTrue(flagged.contains("flagRepo.dismissFlags("))
        assertTrue(flagged.contains("examRepo.updateQuestion(updated)"))
        assertTrue(flagged.contains(".setView(dialogView)"))
    }
}
