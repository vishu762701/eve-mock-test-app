package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramAdditionalListRowsTest {
    @Test fun syllabusAdminUserAndBannerCellsUseSourceAuditedListRoles() {
        val mappings = mapOf(
            "item_syllabus.xml" to mapOf(
                "tvExamName" to "ListTitle", "tvFileName" to "ListPreview"),
            "item_user_admin.xml" to mapOf(
                "tvUserName" to "ListTitle", "tvUserEmail" to "ListPreview",
                "tvUserDates" to "ListTime"),
            "item_admin_banner.xml" to mapOf(
                "tvBannerOrder" to "ListTitle", "tvBannerDate" to "ListTime",
                "tvBannerLink" to "ListPreview")
        )
        for ((file, targets) in mappings) {
            val xml = File("src/main/res/layout/" + file).readText()
            for ((id, role) in targets) {
                val item = xml.lineSequence().first { it.contains("android:id=\"@+id/" + id + "\"") }
                assertTrue("Wrong Telegram style on " + file + "#" + id,
                    item.contains("style=\"@style/TextAppearance.Eve.Telegram." + role + "\""))
                assertTrue("No protected Telegram tag for " + file + "#" + id,
                    item.contains("android:tag=\"eve_telegram_type\""))
            }
        }
    }

    @Test fun variableLengthMetadataStillEllipsizesAndAdminActionsRemain() {
        val s = File("src/main/res/layout/item_syllabus.xml").readText()
        val user = File("src/main/res/layout/item_user_admin.xml").readText()
        val banner = File("src/main/res/layout/item_admin_banner.xml").readText()
        assertTrue(s.contains("android:ellipsize=\"middle\""))
        assertTrue(s.contains("android:singleLine=\"true\""))
        assertTrue(user.contains("android:ellipsize=\"end\""))
        assertTrue(user.contains("android:id=\"@+id/btnMoreUser\""))
        assertTrue(banner.contains("android:id=\"@+id/btnDeleteBanner\""))
        assertTrue(banner.contains("android:id=\"@+id/btnMoveUp\""))
        assertTrue(banner.contains("android:id=\"@+id/btnMoveDown\""))
    }
}
