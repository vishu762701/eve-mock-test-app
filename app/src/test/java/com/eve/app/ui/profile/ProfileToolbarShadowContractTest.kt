package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileToolbarShadowContractTest {
    @Test fun profileHasNoForeignToolbarZShadowOrMisleadingExactClaim() {
        val xml = File("src/main/res/layout/activity_profile.xml").readText()
        val toolbar = xml.substringAfter("<com.eve.app.designsystem.TelegramActionBar")
            .substringBefore("</com.eve.app.designsystem.TelegramActionBar>")
        assertTrue(toolbar.contains("android:elevation=\"0dp\""))
        assertTrue(toolbar.contains("android:translationZ=\"0dp\""))
        assertFalse(xml.contains("<!-- Telegram-exact Profile Page -->"))
        val glass = File("src/main/java/com/eve/app/designsystem/TelegramActionBar.kt").readText()
        assertTrue(glass.contains("drawHairline"))
        assertTrue(glass.contains("glassEnabled"))
    }
}
