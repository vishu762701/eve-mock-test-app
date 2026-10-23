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

        val fallback = findElementById(doc.documentElement, "ivNotificationIdle")
        assertNotNull("Bell must have a persistent vector fallback at idle", fallback)
        assertEquals("@drawable/ic_notification", fallback?.designAttribute("android:src"))
        val action = findElementById(doc.documentElement, "notificationAction")
        assertNotNull("Notification tap target must be accessible", action)
        assertEquals("true", action?.designAttribute("android:clickable"))
        assertEquals("true", action?.designAttribute("android:focusable"))

        val bellBg = bell!!.designAttribute("android:background")
        assertEquals(
            "Bell uses the shared icon-only background",
            "@null",
            bellBg
        )

        val parent = bell.parentNode as? Element
        assertNotNull("btnNotification must have a parent", parent)
        val parentBg = parent!!.designAttribute("android:background")
        assertEquals("Clickable notification target remains visually bare", "@null", parentBg)
        assertEquals("Clickable parent retains source-matched Telegram ripple as foreground",
            "@drawable/telegram_action_icon_ripple", parent.designAttribute("android:foreground"))
        assertEquals("Lottie itself must not paint a second foreground ripple",
            "@null", bell.designAttribute("android:foreground"))
        assertEquals("@null", parent.designAttribute("android:stateListAnimator"))

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
        val light = loadColors("src/main/res/values/design_colors.xml")
        val dark = loadColors("src/main/res/values-night/design_colors.xml")
        assertEquals("#29787880", light["track"])
        assertEquals("#52787880", dark["track"])
        assertEquals("#000000", light["accent"])
        assertEquals("#FFFFFF", dark["accent"])
        for ((name, color) in listOf("design_tab_track" to "track", "design_tab_segment" to "accent")) {
            val drawable = loadXml("src/main/res/drawable/$name.xml")
            assertEquals("@color/$color", (drawable.getElementsByTagName("solid").item(0) as Element).getAttribute("android:color"))
            assertEquals(0, drawable.getElementsByTagName("stroke").length)
        }
    }
}
