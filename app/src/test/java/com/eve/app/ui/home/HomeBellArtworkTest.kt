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
        val source = com.google.gson.JsonParser.parseString(File("src/main/res/raw/notification_bell.json").readText()).asJsonObject
        val homeAnimation = com.google.gson.JsonParser.parseString(File("src/main/res/raw/home_notification_bell.json").readText()).asJsonObject
        val originalLayers = source.getAsJsonArray("layers")
        val homeLayers = homeAnimation.getAsJsonArray("layers")
        for (index in 0 until originalLayers.size()) {
            assertEquals("Bell paths must be the inbox paths", originalLayers[index].asJsonObject["shapes"], homeLayers[index].asJsonObject["shapes"])
        }
        assertEquals(source["fr"], homeAnimation["fr"])
        assertEquals(source["op"], homeAnimation["op"])
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
