package com.eve.app.ui.admin

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AdminHubTelegramFidelityTest {

    @Test
    fun testAdminHubXmlStructureMatchesTelegramSettingsLayout() {
        val xml = File("src/main/res/layout/activity_admin.xml").readText()

        // Root background token
        assertTrue("Root must use @color/bg_app", xml.contains("android:background=\"@color/bg_app\""))

        // Telegram ActionBar
        assertTrue("Must contain TelegramActionBar", xml.contains("<com.eve.app.ui.admin.tg.TelegramActionBar"))

        // Telegram Section Headers
        assertTrue("Must contain HeaderCell for Content", xml.contains("android:id=\"@+id/headerContent\""))
        assertTrue("Must contain HeaderCell for People", xml.contains("android:id=\"@+id/headerPeople\""))
        assertTrue("Must contain HeaderCell for Communication", xml.contains("android:id=\"@+id/headerCommunication\""))
        assertTrue("Must contain HeaderCell for Insights", xml.contains("android:id=\"@+id/headerInsights\""))
        assertTrue("Must contain HeaderCell for System", xml.contains("android:id=\"@+id/headerSystem\""))

        // Telegram Shadow Dividers
        assertTrue("Must contain ShadowSectionCell", xml.contains("<com.eve.app.ui.admin.tg.ShadowSectionCell"))

        // Telegram Cells for rows
        assertTrue("Must contain TextCell for Exams", xml.contains("android:id=\"@+id/rowExams\""))
        assertTrue("Must contain TextCell for Questions", xml.contains("android:id=\"@+id/rowQuestions\""))
        assertTrue("Must contain TextCell for Users", xml.contains("android:id=\"@+id/rowUsers\""))
        assertTrue("Must contain TextCell for Admins", xml.contains("android:id=\"@+id/rowAdmins\""))
        assertTrue("Must contain TextCheckCell for Maintenance", xml.contains("android:id=\"@+id/cellMaintenance\""))

        // Search RecyclerView
        assertTrue("Must contain rvAdminSearch", xml.contains("android:id=\"@+id/rvAdminSearch\""))
    }

    @Test
    fun testAdminActivityIncludesMultilingualSearchWithHindiKeywords() {
        val source = File("src/main/java/com/eve/app/ui/admin/AdminActivity.kt").readText()

        // English & Hindi search keywords
        assertTrue("Must index exams in Hindi", source.contains("परीक्षा"))
        assertTrue("Must index questions in Hindi", source.contains("प्रश्न"))
        assertTrue("Must index users in Hindi", source.contains("उपयोगकर्ता"))
        assertTrue("Must index broadcast in Hindi", source.contains("प्रसारण"))
        assertTrue("Must index maintenance in Hindi", source.contains("रखरखाव"))
        assertTrue("Must index analytics in Hindi", source.contains("एनालिटिक्स"))
        assertTrue("Must index syllabus in Hindi", source.contains("पाठ्यक्रम"))

        // Telegram Avatar integration
        assertTrue("Must use AvatarDrawable", source.contains("AvatarDrawable.create(this, name, email)"))

        // Telegram native dialogs contract
        assertTrue("Must import TelegramNativeAlertDialog", source.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertEquals("Must maintain 22 native dialogs", 22, source.split("TelegramNativeAlertDialog.Builder(").size - 1)
    }

    @Test
    fun testMovedInlinePanelsExistAsDedicatedActivities() {
        val questionsActivity = File("src/main/java/com/eve/app/ui/admin/ManageQuestionsActivity.kt")
        assertTrue("ManageQuestionsActivity must exist", questionsActivity.exists())
        val qSource = questionsActivity.readText()
        assertTrue("ManageQuestionsActivity must use AdminAccessGate", qSource.contains("AdminAccessGate.requireAdmin"))
        assertTrue("ManageQuestionsActivity must use TelegramActionBar", qSource.contains("binding.telegramActionBar"))

        val adminsActivity = File("src/main/java/com/eve/app/ui/admin/AdminsAndRightsActivity.kt")
        assertTrue("AdminsAndRightsActivity must exist", adminsActivity.exists())
        val aSource = adminsActivity.readText()
        assertTrue("AdminsAndRightsActivity must use AdminAccessGate", aSource.contains("AdminAccessGate.requireAdmin"))
        assertTrue("AdminsAndRightsActivity must use TelegramActionBar", aSource.contains("binding.telegramActionBar"))

        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue("Manifest must register ManageQuestionsActivity", manifest.contains("android:name=\".ui.admin.ManageQuestionsActivity\""))
        assertTrue("Manifest must register AdminsAndRightsActivity", manifest.contains("android:name=\".ui.admin.AdminsAndRightsActivity\""))
        assertTrue("Manifest must register AdminRightsEditActivity", manifest.contains("android:name=\".ui.admin.AdminRightsEditActivity\""))

        val rightsActivity = File("src/main/java/com/eve/app/ui/admin/AdminRightsEditActivity.kt")
        assertTrue("AdminRightsEditActivity must exist", rightsActivity.exists())
        val rSource = rightsActivity.readText()
        assertTrue("AdminRightsEditActivity must use AdminAccessGate", rSource.contains("AdminAccessGate.requireAdmin"))
        assertTrue("AdminRightsEditActivity must use TelegramActionBar", rSource.contains("binding.telegramActionBar"))

        val rightsXml = File("src/main/res/layout/activity_admin_rights_edit.xml").readText()
        assertTrue("Rights layout must contain headerRights", rightsXml.contains("android:id=\"@+id/headerRights\""))
        assertTrue("Rights layout must contain checkManageExams", rightsXml.contains("android:id=\"@+id/checkManageExams\""))
        assertTrue("Rights layout must contain checkManageQuestions", rightsXml.contains("android:id=\"@+id/checkManageQuestions\""))
        assertTrue("Rights layout must contain btnDismissAdmin", rightsXml.contains("android:id=\"@+id/btnDismissAdmin\""))
    }

    @Test
    fun testRecentActionsTelegramLogFidelity() {
        val logXml = File("src/main/res/layout/activity_activity_log.xml").readText()
        assertTrue("Log layout must contain TelegramActionBar", logXml.contains("<com.eve.app.ui.admin.tg.TelegramActionBar"))
        assertTrue("Log layout must contain TelegramSegmentedTabs", logXml.contains("<com.eve.app.ui.admin.tg.TelegramSegmentedTabs"))
        assertTrue("Log layout must contain rvAuditLogs", logXml.contains("android:id=\"@+id/rvAuditLogs\""))
        assertTrue("Log layout must contain progressBar", logXml.contains("android:id=\"@+id/progressBar\""))
        assertTrue("Log layout must contain emptyContainer", logXml.contains("android:id=\"@+id/emptyContainer\""))
        assertTrue("Log layout must use search lottie for empty state", logXml.contains("@raw/search"))

        val activitySource = File("src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt").readText()
        assertTrue("ActivityLogActivity must use AdminAccessGate", activitySource.contains("AdminAccessGate.requireAdmin"))
        assertTrue("ActivityLogActivity must support cursor pagination", activitySource.contains("oldestTimestamp"))
        assertTrue("ActivityLogActivity must support filter bottom sheet", activitySource.contains("showFilterSheet"))
        assertTrue("ActivityLogActivity must support detail dialog", activitySource.contains("showLogDetailDialog"))

        val adapterSource = File("src/main/java/com/eve/app/ui/admin/AuditLogAdapter.kt").readText()
        assertTrue("AuditLogAdapter must support day grouping headers", adapterSource.contains("AuditLogListItem.Header"))
        assertTrue("AuditLogAdapter must support log entries", adapterSource.contains("AuditLogListItem.Entry"))
    }
    @Test
    fun testTelegramSourceExactAdminTabAndRightsMetrics() {
        val tabs = File("src/main/java/com/eve/app/ui/admin/tg/TelegramSegmentedTabs.kt").readText()
        assertTrue("Tabs must use Telegram 320ms indicator timing", tabs.contains("duration = 320L"))
        assertTrue("Tabs must use Telegram EASE_OUT_QUINT control points", tabs.contains("PathInterpolator(0.23f, 1f, 0.32f, 1f)"))
        assertTrue("Tabs must use the 14dp indicator radius", tabs.contains("dp(14f)"))
        assertTrue("Tabs must keep Telegram's translucent selector", tabs.contains("indicatorPaint.alpha = 31"))
        val rights = File("src/main/java/com/eve/app/ui/admin/tg/TextCheckCell.kt").readText()
        assertTrue("Subtitle rows must expand to Telegram 64dp", rights.contains("64f else 50f"))
        assertTrue("Switch must keep Telegram 22dp margin", rights.contains("marginEnd = dp(22f)"))
    }

    @Test
    fun testTelegramAdminSearchUsesSourceTimingAndCancelsStaleTransitions() {
        val source = File("src/main/java/com/eve/app/ui/admin/tg/TelegramActionBar.kt").readText()
        assertTrue("Original ActionBar search timing must be retained", source.contains("setDuration(150L)"))
        assertTrue("Search must retain Telegram's outgoing title scale", source.contains("scaleX(0.95f).scaleY(0.95f)"))
        assertTrue("Search must cancel stale completion callbacks", source.contains("generation != searchAnimationGeneration"))
        assertTrue("Back button must remain clickable over search", source.contains("backButton.bringToFront()"))
    }

    @Test
    fun testSharedAdminCellsRetainTelegramRowMeasurementsAndRtl() {
        val root = "src/main/java/com/eve/app/ui/admin/tg/"
        val cell = File(root + "TextCell.kt").readText()
        assertTrue("Telegram 50dp row must be explicit", cell.contains("val rowHeight = dp(50f)"))
        assertTrue("Physical divider pixel must increase layout height", cell.contains("(if (needDivider) 1 else 0)"))
        assertTrue("RTL must mirror divider inset", cell.contains("width - start"))
        val detail = File(root + "TextDetailCell.kt").readText()
        assertTrue("Telegram detail rows must be 60dp", detail.contains("val rowHeight = dp(60f)"))
        assertTrue("Detail divider must be one physical pixel", detail.contains("height - 1f"))
        val check = File(root + "TextCheckCell.kt").readText()
        assertTrue("Subtitle changes must remeasure rights rows", check.contains("requestLayout() // Telegram TextCheckCell.java:148-153"))
    }

    @Test
    fun testAdminSearchSystemBackClosesSearchBeforeLeavingActivity() {
        val actionBar = File("src/main/java/com/eve/app/ui/admin/tg/TelegramActionBar.kt").readText()
        assertTrue("Search-back must be lifecycle bound", actionBar.contains("bindSystemBack(owner: OnBackPressedDispatcherOwner)"))
        assertTrue("System Back must collapse expanded search", actionBar.contains("if (isSearchExpanded) {\n                    closeSearch()"))
        assertTrue("Fallback Back must forward rather than recurse", actionBar.contains("isEnabled = false"))
        for (activity in listOf("AdminActivity.kt", "ActivityLogActivity.kt", "emoji/EmojiStudioActivity.kt")) {
            val source = File("src/main/java/com/eve/app/ui/admin/" + activity).readText()
            assertTrue("Search-aware hardware Back missing for " + activity, source.contains(".bindSystemBack(this)"))
        }
    }

    @Test
    fun testAdminDialogsUseTelegramRendererForRecentActionsAndEmojiStudio() {
        val activityLog = File("src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt").readText()
        val emoji = File("src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt").readText()
        assertFalse("Recent Actions must not construct AndroidX AlertDialog", activityLog.contains("import androidx.appcompat.app.AlertDialog"))
        assertFalse("Emoji Studio must not construct AndroidX AlertDialog", emoji.contains("import androidx.appcompat.app.AlertDialog"))
        assertEquals("Recent Actions must use Telegram modal renderer", 1, activityLog.split("TelegramNativeAlertDialog.Builder(this)").size - 1)
        assertEquals("All six Emoji Studio dialogs must use Telegram modal renderer", 6, emoji.split("TelegramNativeAlertDialog.Builder(this)").size - 1)
        assertTrue("Delete confirmation must use destructive text role", emoji.contains("makeRed(DialogInterface.BUTTON_POSITIVE)"))
    }

    @Test
    fun testEmojiPlacementCanBeRemovedOnlyAfterWorkerConfirmation() {
        val activity = File("src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt").readText()
        val layout = File("src/main/res/layout/bottom_sheet_emoji_placement.xml").readText()
        assertTrue("Removal must use the Telegram-derived TextCell", layout.contains("<com.eve.app.ui.admin.tg.TextCell"))
        assertTrue("Removal row must exist", layout.contains("android:id=\"@+id/rowRemovePlacement\""))
        assertTrue("Removal must call the actual Worker-backed repository", activity.contains("emojiRepo.clearPlacement(slotType, targetId)"))
        assertTrue("Success must be conditional on server response", activity.contains("if (result.isSuccess)"))
        assertTrue("Failure must be visible to admins", activity.contains("result.exceptionOrNull()?.toUserFriendlyMessage()"))
        assertTrue("Destructive confirmation must be Telegram-rendered", activity.contains(".setTitle(\"Remove Placement?\")"))
    }

}
