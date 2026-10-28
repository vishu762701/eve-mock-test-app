package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Seven EVE actions remain reachable as 48dp+ standalone accessibility targets. */
class HomeMenuAccessibilityContractTest {
    private val rowIds = listOf(
        "cardTheme", "menuRowHistory", "menuRowBookmarks", "menuRowMistakes",
        "menuRowTopic", "menuRowPyq", "menuRowLogout"
    )

    @Test fun popupRetainsSevenFocusableActionsAtLeast48dpTall() {
        val layout = File("src/main/res/layout/popup_telegram_menu.xml")
        val dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layout)
        for (id in rowIds) {
            val nodes = dom.getElementsByTagName("LinearLayout")
            val element = (0 until nodes.length).map { nodes.item(it) as Element }
                .firstOrNull { it.getAttribute("android:id") == "@+id/$id" }
            assertNotNull("Missing menu action $id", element)
            assertEquals("true", element!!.getAttribute("android:clickable"))
            assertEquals("true", element.getAttribute("android:focusable"))
            assertEquals("48dp", element.getAttribute("android:minHeight"))
            assertEquals(1, element.getElementsByTagName("ImageView").length)
            assertEquals(1, element.getElementsByTagName("TextView").length)
        }
    }

    @Test fun originalTelegramRadialPopupMaskIsAppliedWithExactRowCorners() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val selector = File("src/main/java/com/eve/app/ui/home/TelegramPopupRadialSelector.java").readText()
        assertTrue(popup.contains("row.background = TelegramPopupRadialSelector.create("))
        assertTrue(popup.contains("if (index == 0) 6 else 0"))
        assertTrue(popup.contains("if (index == rows.lastIndex) 6 else 0"))
        assertTrue(popup.contains("row.foreground = null"))
        assertTrue(selector.contains("path.addRoundRect(rect, radii, Path.Direction.CW)"))
        assertTrue(selector.contains("new Paint(Paint.ANTI_ALIAS_FLAG)"))
        assertTrue(selector.contains("final int save = canvas.save()"))
        assertTrue(selector.contains("canvas.restoreToCount(save)"))
        assertTrue(selector.contains("3de37c8cfb039452be7398909461da6288babf15"))
        assertTrue(!selector.contains("org.telegram.messenger"))
    }

    @Test fun sourceBackedNinePatchOnlyChangesHomeOverflow() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val glass = File("src/main/java/com/eve/app/designsystem/DesignGlassLayout.kt").readText()
        assertTrue(popup.contains("R.drawable.telegram_popup_fixed_alert2"))
        assertTrue(popup.contains("PorterDuff.Mode.MULTIPLY"))
        assertTrue(popup.contains("palette.submenuBackground"))
        assertTrue(popup.contains("binding.root.glassEnabled = false"))
        assertTrue(popup.contains("binding.root.originalPopupBackground = sourceBackground"))
        assertTrue(glass.contains("var originalPopupBackground: Drawable? = null"))
        assertTrue(glass.contains("if (source != null) background = source"))
    }

    @Test fun allRowsAreReadAsSingleDynamicLabelWithoutDecorativeDuplication() {
        val source = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val roles = source.substringAfter("private fun applyOriginalMenuRoles()")
            .substringBefore("private fun dismissWithAction(")
        for (id in rowIds) assertTrue(roles.contains("binding.$id"))
        assertTrue(roles.contains("row.contentDescription = child.text"))
        assertTrue(roles.contains("row.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES"))
        assertTrue(roles.contains("child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO"))
        assertTrue(roles.contains("binding.root.updatePalette(palette)"))
    }
}
