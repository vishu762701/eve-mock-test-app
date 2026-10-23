package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Verifies the Telegram action-icon tint and accessibility contract from parsed
 * XML attributes, independent of whitespace or XML element line wrapping.
 * This does NOT verify GPL bitmap/shape parity.
 */
class TelegramActionIconTintTest {
    private fun actionIcons(file: File): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val doc = factory.newDocumentBuilder().parse(file)
        val found = mutableListOf<Element>()
        fun visit(node: Node) {
            if (node is Element) {
                val source = node.getAttribute("android:src")
                val navigation = node.getAttribute("app:navigationIcon")
                if (source == "@drawable/ic_back" || source == "@drawable/ic_more_vert" ||
                    navigation == "@drawable/ic_back") {
                    found.add(node)
                }
            }
            val children = node.childNodes
            for (index in 0 until children.length) visit(children.item(index))
        }
        visit(doc.documentElement)
        return found
    }

    @Test fun everyActivityNavigationActionUsesTelegramSourceIconColors() {
        val files = File("src/main/res/layout").listFiles()!!.filter {
            it.isFile && it.name.startsWith("activity_") && it.extension == "xml"
        }
        var affected = 0
        for (file in files) {
            for (icon in actionIcons(file)) {
                val toolbarNavigation = icon.getAttribute("app:navigationIcon") == "@drawable/ic_back"
                val tintAttr = if (toolbarNavigation) "app:navigationIconTint" else "app:tint"
                assertEquals("Incorrect ActionBar tint in ${file.name}",
                    "@color/telegram_type_primary", icon.getAttribute(tintAttr))
                if (!toolbarNavigation) {
                    assertTrue("Navigation icon lacks content description in ${file.name}",
                        icon.hasAttribute("android:contentDescription"))
                }
                affected++
            }
        }
        assertTrue("Incomplete source role coverage of ActionBar navigation", affected >= 40)
    }

    @Test fun sourceMappedToolbarActionsHavePairedThemeTintsAndPreservedTargets() {
        val day = File("src/main/res/values/telegram_colors.xml").readText()
        val night = File("src/main/res/values-night/telegram_colors.xml").readText()
        assertTrue(day.contains("<color name=\"telegram_type_primary\">#FF1A1D21</color>"))
        assertTrue(night.contains("<color name=\"telegram_type_primary\">#FFFFFFFF</color>"))
        val screens = listOf("activity_main", "activity_settings", "activity_notifications",
            "activity_result", "activity_profile", "activity_result_detail",
            "activity_about", "activity_admin", "activity_appearance")
        var actions = 0
        for (screen in screens) {
            val file = File("src/main/res/layout/$screen.xml")
            val icons = actionIcons(file).filter {
                it.getAttribute("android:src") == "@drawable/ic_back" ||
                    it.getAttribute("android:src") == "@drawable/ic_more_vert"
            }
            assertTrue("No corresponding ActionBar action in $screen", icons.isNotEmpty())
            for (icon in icons) {
                assertEquals("Tint is not Telegram Day/Night action ink in $screen",
                    "@color/telegram_type_primary", icon.getAttribute("app:tint"))
                assertEquals("Touch size missing $screen",
                    "@dimen/design_touch_min", icon.getAttribute("android:layout_width"))
                assertTrue("Content description missing $screen",
                    icon.hasAttribute("android:contentDescription"))
                actions++
            }
        }
        assertEquals(10, actions)
    }
}
