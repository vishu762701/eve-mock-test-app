package com.eve.app.ui.home

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class HomeBellArtworkTest {
    private fun sha(path: String) = MessageDigest.getInstance("SHA-256").digest(File(path).readBytes())
        .joinToString("") { "%02x".format(it) }

    @Test fun everyHomeFrameAndFallbackUsesTheApprovedIndependentSource() {
        val home = File("src/main/res/layout/activity_main.xml").readText()
        assertTrue(home.contains("@drawable/ic_home_notification"))
        assertTrue(home.contains("@raw/home_notification_bell"))
        assertFalse(home.contains("@drawable/ic_bell\""))
        assertFalse(home.contains("@raw/notification_bell\""))
        val provenance = File("../docs/sources/home-bell/provenance.json").readText()
        for (path in listOf("src/main/res/drawable/ic_home_notification.xml", "src/main/res/raw/home_notification_bell.json")) {
            assertTrue("Artifact digest must match recorded source conversion", provenance.contains(sha(path)))
        }
        assertTrue(provenance.contains(sha("../docs/sources/home-bell/bell.svg")))
        assertTrue(File("../docs/sources/home-bell/LICENSE").readText().contains("MIT License"))
        assertTrue(File("src/main/assets/licenses/Tabler-LICENSE.txt").readText().contains("Permission is hereby granted"))
    }

    @Test fun replacingHomeNeverDeletesSharedInboxAndReminderArtwork() {
        assertTrue(File("src/main/res/layout/activity_notifications.xml").readText().contains("@raw/notification_bell"))
        assertTrue(File("src/main/res/layout/item_notification.xml").readText().contains("@drawable/ic_bell"))
        assertTrue(File("src/main/res/raw/notification_bell.json").isFile)
        for (density in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi")) {
            assertTrue(File("src/main/res/drawable-$density/ic_bell.webp").isFile)
        }
    }
}
