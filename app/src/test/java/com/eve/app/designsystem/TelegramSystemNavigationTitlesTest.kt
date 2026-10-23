package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TelegramSystemNavigationTitlesTest {
    @Test fun systemAndAnalyticsNavigationTitlesUseActionBarType() {
        val root = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        val xml = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val ns = "http://schemas.android.com/apk/res/android"
        for ((filename, title) in mapOf(
            "activity_app_config.xml" to "App Config & Maintenance",
            "activity_admin_analytics.xml" to "Admin Analytics",
            "activity_api_usage.xml" to "API Usage & Cost Monitor",
            "activity_system_monitor.xml" to "System Health & Operations"
        )) {
            val doc = xml.newDocumentBuilder().parse(File(root, filename))
            val nodes = doc.getElementsByTagName("TextView")
            val matches = (0 until nodes.length).map { nodes.item(it) as Element }
                .filter { it.getAttributeNS(ns, "text") == title
                    && it.getAttribute("style") == "@style/TextAppearance.Eve.Telegram.NavigationTitle" }
            assertEquals("$filename title", 1, matches.size)
            assertEquals("$filename tag", "eve_telegram_type", matches.single().getAttributeNS(ns, "tag"))
        }
    }
}
