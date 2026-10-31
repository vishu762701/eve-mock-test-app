package com.eve.app.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileActionAccessibilitySourceTest {
    @Test fun threeActionsExposeTelegramButtonClassTextClickAndEnabledState() {
        val code = File("src/main/java/com/eve/app/ui/profile/ProfileActionAccessibility.kt").readText()
        val expected = listOf(
            "Button::class.java.name",
            "info.text = label.text",
            "info.isClickable = host.isClickable",
            "info.isFocusable = host.isFocusable",
            "info.isEnabled = host.isEnabled",
            "AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK",
            "View.IMPORTANT_FOR_ACCESSIBILITY_YES",
            "View.IMPORTANT_FOR_ACCESSIBILITY_NO"
        )
        for (part in expected) assertTrue("Missing original Telegram accessibility semantic: $part", code.contains(part))
        val activity = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val setup = activity.substringAfter("private fun setupProfileActionAccessibility()")
            .substringBefore("private fun setupCells()")
        for (id in listOf("btnSetPhoto", "btnEditInfo", "btnSettings")) {
            assertTrue("Missing action $id in accessibility setup", setup.contains("binding.$id"))
        }
        assertTrue(activity.contains("setupProfileActionAccessibility()"))
    }
}
