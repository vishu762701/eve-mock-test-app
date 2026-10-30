package com.eve.app.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Contract verifying Admin UI dialogs use verified Telegram source chrome and proper styling. */
class AdminDialogSourceContractTest {

    @Test fun adminActivityAppliesTelegramSourceChromeAcrossAllModals() {
        val source = File("src/main/java/com/eve/app/ui/admin/AdminActivity.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.showTelegramSourceDialog"))
        assertTrue(source.contains("import com.eve.app.ui.home.TelegramDialogSourceBackground"))
        val sourceDialogCount = source.split(".showTelegramSourceDialog()").size - 1
        assertEquals("AdminActivity plain dialog builders must all use showTelegramSourceDialog", 11, sourceDialogCount)
        val customBackgroundCount = source.split("TelegramDialogSourceBackground.apply(").size - 1
        assertEquals("AdminActivity custom view dialogs must all apply TelegramDialogSourceBackground", 5, customBackgroundCount)
    }

    @Test fun manageExamsActivityUsesSourceDialogsForPromptsAndDetails() {
        val source = File("src/main/java/com/eve/app/ui/admin/ManageExamsActivity.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.showTelegramSourceDialog"))
        val sourceDialogCount = source.split(".showTelegramSourceDialog()").size - 1
        assertEquals("ManageExamsActivity must route all 14 plain dialog builders through source dialog adapter", 14, sourceDialogCount)
    }

    @Test fun adminDeleteFlowAppliesDestructiveAndSourceStyling() {
        val source = File("src/main/java/com/eve/app/ui/admin/AdminDeleteFlow.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.showDestructive"))
        assertTrue(source.contains("import com.eve.app.designsystem.showTelegramSourceDialog"))
        assertTrue(source.contains(".showDestructive()"))
        assertTrue(source.contains(".showTelegramSourceDialog()"))
    }

    @Test fun secondaryAdminActivitiesRouteDialogsThroughSourceAdapters() {
        val activities = listOf(
            "src/main/java/com/eve/app/ui/admin/EditAboutActivity.kt",
            "src/main/java/com/eve/app/ui/admin/FlaggedQuestionsActivity.kt",
            "src/main/java/com/eve/app/ui/admin/ManagePremiumActivity.kt",
            "src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt",
            "src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt",
            "src/main/java/com/eve/app/ui/admin/SendNotificationActivity.kt",
            "src/main/java/com/eve/app/ui/admin/AdminAnalyticsActivity.kt"
        )
        // The read-only generated-test preview now uses the actual source-derived
        // native message scroll host instead of the previous Material adapter.
        val preview = File("src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt").readText()
        assertTrue(preview.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(preview.contains(".setMessage(formattedText.toString())"))
        assertTrue(preview.contains(".setPositiveButton(\"Close\", null)"))
        assertTrue(preview.contains(".show()"))
        for (path in activities) {
            val content = File(path).readText()
            assertTrue("$path must import showTelegramSourceDialog", content.contains("import com.eve.app.designsystem.showTelegramSourceDialog"))
            assertTrue("$path must call showTelegramSourceDialog()", content.contains(".showTelegramSourceDialog()"))
        }
    }
}
