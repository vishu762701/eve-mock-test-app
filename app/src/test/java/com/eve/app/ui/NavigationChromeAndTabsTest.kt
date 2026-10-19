package com.eve.app.ui

import com.eve.app.designsystem.designAttribute
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NavigationChromeAndTabsTest {

    private fun loadXml(relativePath: String): Document {
        val fileCandidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = fileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }

    private fun findElementById(node: Node, targetId: String): Element? {
        if (node is Element) {
            val id = node.getAttribute("android:id")
            if (id == targetId || id == "@+id/$targetId" || id.endsWith("/$targetId")) {
                return node
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            val found = findElementById(children.item(i), targetId)
            if (found != null) return found
        }
        return null
    }

    private fun loadColors(relativePath: String): Map<String, String> {
        val doc = loadXml(relativePath)
        val colorNodes = doc.getElementsByTagName("color")
        val map = mutableMapOf<String, String>()
        for (i in 0 until colorNodes.length) {
            val node = colorNodes.item(i) as Element
            val name = node.getAttribute("name")
            val value = node.textContent?.trim()
            if (name.isNotEmpty() && value != null) {
                map[name] = value
            }
        }
        return map
    }

    @Test
    fun testTopHeaders_areTransparentContainersWithoutCapsuleBackgrounds() {
        val layoutFiles = listOf(
            "src/main/res/layout/activity_main.xml",
            "src/main/res/layout/activity_settings.xml",
            "src/main/res/layout/activity_profile.xml",
            "src/main/res/layout/activity_result.xml",
            "src/main/res/layout/activity_admin.xml",
            "src/main/res/layout/activity_history.xml",
            "src/main/res/layout/activity_leaderboard.xml"
        )

        for (path in layoutFiles) {
            val doc = loadXml(path)
            val toolbars = doc.getElementsByTagName("com.eve.app.ui.common.EveGlassToolbar")
            assertTrue("Expected at least 1 EveGlassToolbar in $path", toolbars.length >= 1)
            val headerToolbar = toolbars.item(0) as Element
            val bg = headerToolbar.designAttribute("android:background")
            assertTrue(
                "Header toolbar in $path must have transparent or empty background (was '$bg')",
                bg.isEmpty() || bg == "@android:color/transparent" || bg == "@null"
            )
        }
    }

    @Test
    fun testBareLottieNotificationBell_andParentContainer() {
        val doc = loadXml("src/main/res/layout/activity_main.xml")
        val bell = findElementById(doc.documentElement, "btnNotification")
        assertNotNull("btnNotification must exist in activity_main.xml", bell)

        val bellBg = bell!!.designAttribute("android:background")
        assertEquals(
            "btnNotification background must be @android:color/transparent",
            "@android:color/transparent",
            bellBg
        )

        val parent = bell.parentNode as? Element
        assertNotNull("btnNotification must have a parent", parent)
        val parentBg = parent!!.designAttribute("android:background")
        assertEquals(
            "btnNotification parent FrameLayout must have @android:color/transparent background",
            "",
            parentBg
        )

        val dotUnread = findElementById(parent, "dotUnread")
        assertNotNull("dotUnread must exist inside bell parent", dotUnread)
    }

    @Test
    fun testResultTabs_separatedFromHeaderToolbar_andHasSingleTrackSurface() {
        val doc = loadXml("src/main/res/layout/activity_result.xml")
        val tabLayout = findElementById(doc.documentElement, "tabLayoutResult")
        assertNotNull("tabLayoutResult must exist in activity_result.xml", tabLayout)

        assertEquals(
            "tabLayoutResult must have bg_result_segmented_track as its background",
            "@drawable/design_tab_track",
            tabLayout!!.designAttribute("android:background")
        )

        val parent = tabLayout.parentNode as? Element
        assertNotNull("tabLayoutResult must have a parent container", parent)
        assertNotEquals(
            "tabLayoutResult parent must NOT be EveGlassToolbar",
            "com.eve.app.ui.common.EveGlassToolbar",
            parent!!.tagName
        )

        val parentBg = parent.designAttribute("android:background")
        assertTrue(
            "tabLayoutResult parent must not have duplicate track background (was '$parentBg')",
            parentBg.isEmpty() || parentBg == "@android:color/transparent" || parentBg == "@null"
        )
    }

    @Test
    fun testSegmentedControl_colorAliasesAndDrawables() {
        val lightColors = loadColors("src/main/res/values/approved_ui_colors.xml")
        val darkColors = loadColors("src/main/res/values-night/approved_ui_colors.xml")

        assertEquals("@color/eve_surface_variant", lightColors["eve_result_tab_track"])
        assertEquals("@color/eve_secondary_system_grouped_background", darkColors["eve_result_tab_track"])

        assertEquals("@color/eve_system_background", lightColors["eve_result_tab_selected"])
        assertEquals("@color/eve_system_gray2", darkColors["eve_result_tab_selected"])

        assertEquals("@color/eve_label_primary", lightColors["eve_result_tab_selected_text"])
        assertEquals("@color/eve_label_primary", darkColors["eve_result_tab_selected_text"])

        val trackDoc = loadXml("src/main/res/drawable/bg_result_segmented_track.xml")
        val solidTrack = trackDoc.getElementsByTagName("solid").item(0) as Element
        assertEquals("@color/eve_result_tab_track", solidTrack.getAttribute("android:color"))
        assertEquals("Track must have no stroke", 0, trackDoc.getElementsByTagName("stroke").length)

        val indicatorDoc = loadXml("src/main/res/drawable/bg_result_segmented_indicator.xml")
        val solidIndicator = indicatorDoc.getElementsByTagName("solid").item(0) as Element
        assertEquals("@color/eve_result_tab_selected", solidIndicator.getAttribute("android:color"))
        assertEquals("Indicator must have no stroke", 0, indicatorDoc.getElementsByTagName("stroke").length)
    }
}
