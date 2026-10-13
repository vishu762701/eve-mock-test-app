package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Phase 1 Official Apple iOS 26 Semantic Color Foundation Verification for EVE Home Screen.
 *
 * Verifies:
 * 1. Authoritative Apple iOS 26 color token values and dynamic Day/Night pairs.
 * 2. Dedicated Home category chip selector state mappings (accent container & label).
 * 3. WCAG 2.1 Level AA contrast compliance (>= 4.5:1 for normal text, >= 3.0:1 for graphical UI).
 */
class HomeSemanticColorsTest {

    private fun loadColors(relativePath: String): Map<String, String> {
        val fileCandidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = fileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find colors file: $relativePath")

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val colorNodes = doc.getElementsByTagName("color")

        val map = mutableMapOf<String, String>()
        for (i in 0 until colorNodes.length) {
            val node = colorNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            val value = node.textContent?.trim()
            if (name != null && value != null && (value.startsWith("#") || value.startsWith("@color/") || value == "@android:color/transparent")) {
                map[name] = if (value == "@android:color/transparent") "#00000000" else value
            }
        }
        fun resolve(name: String, seen: Set<String> = emptySet()): String {
            check(name !in seen) { "Cyclic color: $name" }
            val value = map[name] ?: return "#00000000"
            return if (value.startsWith("@color/")) resolve(value.removePrefix("@color/"), seen + name) else value
        }
        return map.keys.associateWith { resolve(it) }
    }

    private fun loadSelector(relativePath: String): List<Pair<String?, String>> {
        val fileCandidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = fileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find selector file: $relativePath")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val items = doc.getElementsByTagName("item")
        val result = mutableListOf<Pair<String?, String>>()
        for (i in 0 until items.length) {
            val el = items.item(i) as Element
            val state = el.getAttribute("android:state_checked").ifEmpty { null }
            val color = el.getAttribute("android:color")
            result.add(state to color)
        }
        return result
    }

    private fun parseHex(hex: String): Int {
        val clean = hex.removePrefix("#")
        val parsed = clean.toLong(16).toInt()
        return if (clean.length <= 6) {
            (0xFF shl 24) or (parsed and 0xFFFFFF)
        } else {
            parsed
        }
    }

    private fun red(c: Int): Int = (c shr 16) and 0xFF
    private fun green(c: Int): Int = (c shr 8) and 0xFF
    private fun blue(c: Int): Int = c and 0xFF
    private fun alpha(c: Int): Double = ((c shr 24) and 0xFF) / 255.0

    private fun channelLuminance(value8Bit: Int): Double {
        val s = value8Bit / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun compositeColor(fgHex: String, bgHex: String): String {
        val fg = parseHex(fgHex)
        val bg = parseHex(bgHex)
        val a = alpha(fg)
        val r = (red(fg) * a + red(bg) * (1 - a)).toInt().coerceIn(0, 255)
        val g = (green(fg) * a + green(bg) * (1 - a)).toInt().coerceIn(0, 255)
        val b = (blue(fg) * a + blue(bg) * (1 - a)).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    private fun relativeLuminance(colorHex: String): Double {
        val c = parseHex(colorHex)
        val r = channelLuminance(red(c))
        val g = channelLuminance(green(c))
        val b = channelLuminance(blue(c))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(fgHex: String, bgHex: String, canvasHex: String = "#FFFFFF"): Double {
        val compBg = compositeColor(bgHex, canvasHex)
        val compFg = compositeColor(fgHex, compBg)
        val l1 = relativeLuminance(compFg)
        val l2 = relativeLuminance(compBg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testOfficialAppleBackgroundHierarchy_forHome() {
        val light = loadColors("src/main/res/values/colors.xml")
        val dark = loadColors("src/main/res/values-night/colors.xml")

        // 1. System Canvas (Root Home background) - OLED true black in dark
        assertEquals("#FFFFFF", light["eve_system_background"])
        assertEquals("#000000", dark["eve_system_background"])
        assertEquals("#FFFFFF", light["eve_bg"])
        assertEquals("#000000", dark["eve_bg"])
        assertEquals("#FFFFFF", light["eve_canvas"])
        assertEquals("#000000", dark["eve_canvas"])

        // 2. Secondary System Background (Home Exam cards, category chip unselected)
        assertEquals("#F2F2F7", light["eve_secondary_system_background"])
        assertEquals("#1C1C1E", dark["eve_secondary_system_background"])
        assertEquals("#F2F2F7", light["eve_secondary_background"])
        assertEquals("#1C1C1E", dark["eve_secondary_background"])

        // 3. Tertiary System Background (Nested surfaces, modal dialogs)
        assertEquals("#FFFFFF", light["eve_tertiary_system_background"])
        assertEquals("#2C2C2E", dark["eve_tertiary_system_background"])

        // 4. Grouped Backgrounds
        assertEquals("#F2F2F7", light["eve_system_grouped_background"])
        assertEquals("#000000", dark["eve_system_grouped_background"])

        // 5. Drawer Scrim
        assertEquals("#26000000", light["eve_drawer_scrim"])
        assertEquals("#4D000000", dark["eve_drawer_scrim"])
    }

    @Test
    fun testOfficialAppleLabelHierarchy_forHome() {
        val light = loadColors("src/main/res/values/colors.xml")
        val dark = loadColors("src/main/res/values-night/colors.xml")

        // 1. Primary Label (Greeting, Exam Title, Section headers)
        assertEquals("#000000", light["eve_label_primary"])
        assertEquals("#FFFFFF", dark["eve_label_primary"])

        // 2. Secondary Label (EVE kicker, metadata, subtitles)
        assertEquals("#50505A", light["eve_label_secondary"])
        assertEquals("#C5C5CC", dark["eve_label_secondary"])

        // 3. Tertiary Label (Placeholders, subtle captions)
        assertEquals("#62626D", light["eve_label_tertiary"])
        assertEquals("#ABABB3", dark["eve_label_tertiary"])
        assertEquals(light["eve_label_tertiary"], light["eve_placeholder_text"])
        assertEquals(dark["eve_label_tertiary"], dark["eve_placeholder_text"])

        // 4. Header Ink Tokens
        assertEquals("#FFFFFF", light["eve_header_ink"])
        assertEquals("#000000", dark["eve_header_ink"])
        assertEquals("#000000", light["eve_header_ink_text"])
        assertEquals("#FFFFFF", dark["eve_header_ink_text"])
    }

    @Test
    fun testHomeCategoryChipSelectors_mapToAppleSemanticRoles() {
        val bgItems = loadSelector("src/main/res/color/selector_home_category_chip_bg.xml")
        assertEquals(2, bgItems.size)
        assertEquals("true" to "@color/eve_accent_container", bgItems[0])
        assertEquals(null to "@color/eve_secondary_system_background", bgItems[1])

        val textItems = loadSelector("src/main/res/color/selector_home_category_chip_text.xml")
        assertEquals(2, textItems.size)
        assertEquals("true" to "@color/eve_accent_text", textItems[0])
        assertEquals(null to "@color/eve_label_primary", textItems[1])
    }

    @Test
    fun testHomeColorContrast_WCAG_AA_Compliance_LightAndDark() {
        val light = loadColors("src/main/res/values/colors.xml")
        val dark = loadColors("src/main/res/values-night/colors.xml")

        for ((themeName, colors, canvas) in listOf(
            Triple("Light", light, "#FFFFFF"),
            Triple("Dark", dark, "#000000")
        )) {
            // Primary label on canvas >= 4.5:1
            val primaryOnCanvas = contrastRatio(colors["eve_label_primary"]!!, colors["eve_system_background"]!!, canvas)
            assertTrue("$themeName eve_label_primary on canvas must be >= 4.5:1 (was $primaryOnCanvas)", primaryOnCanvas >= 4.5)

            // Secondary label on canvas >= 4.5:1
            val secOnCanvas = contrastRatio(colors["eve_label_secondary"]!!, colors["eve_system_background"]!!, canvas)
            assertTrue("$themeName eve_label_secondary on canvas must be >= 4.5:1 (was $secOnCanvas)", secOnCanvas >= 4.5)

            // Primary label on secondary card background >= 4.5:1
            val primaryOnCard = contrastRatio(colors["eve_label_primary"]!!, colors["eve_secondary_system_background"]!!, canvas)
            assertTrue("$themeName eve_label_primary on card must be >= 4.5:1 (was $primaryOnCard)", primaryOnCard >= 4.5)

            // Secondary label on secondary card background >= 4.5:1
            val secOnCard = contrastRatio(colors["eve_label_secondary"]!!, colors["eve_secondary_system_background"]!!, canvas)
            assertTrue("$themeName eve_label_secondary on card must be >= 4.5:1 (was $secOnCard)", secOnCard >= 4.5)

            // Selected category chip: accent text on accent container >= 4.5:1
            val accentOnContainer = contrastRatio(colors["eve_accent_text"]!!, colors["eve_accent_container"]!!, canvas)
            assertTrue("$themeName eve_accent_text on eve_accent_container must be >= 4.5:1 (was $accentOnContainer)", accentOnContainer >= 4.5)

            // Neutral button text on neutral button background >= 4.5:1
            val neutralButtonTextOnBg = contrastRatio(colors["eve_button_neutral_text"]!!, colors["eve_button_neutral_bg"]!!, canvas)
            assertTrue("$themeName neutral button text on bg must be >= 4.5:1 (was $neutralButtonTextOnBg)", neutralButtonTextOnBg >= 4.5)

            // Interactive primary blue on canvas (UI interactive control >= 3.0:1)
            val primaryOnBg = contrastRatio(colors["eve_primary"]!!, colors["eve_system_background"]!!, canvas)
            assertTrue("$themeName eve_primary on canvas must be >= 3.0:1 (was $primaryOnBg)", primaryOnBg >= 3.0)

            // White on primary interactive blue button >= 4.5:1
            val onPrimaryContrast = contrastRatio(colors["eve_on_primary"]!!, colors["eve_primary"]!!, canvas)
            assertTrue("$themeName eve_on_primary on eve_primary must be >= 4.5:1 (was $onPrimaryContrast)", onPrimaryContrast >= 4.5)
        }
    }

    @Test
    fun homeOptionAUsesOneUninterruptedGroupedCanvas() {
        fun source(path: String): String {
            val candidates = listOf(File(path), File("app/$path"), File("../app/$path"))
            return candidates.firstOrNull { it.exists() }?.readText()
                ?: error("Missing Home source file: $path")
        }

        val parser = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xml = parser.parse(org.xml.sax.InputSource(java.io.StringReader(
            source("src/main/res/layout/activity_main.xml")))).documentElement
        fun view(id: String): Element {
            val descendants = xml.getElementsByTagName("*")
            for (i in 0 until descendants.length) {
                val el = descendants.item(i) as Element
                if (el.getAttribute("android:id") == "@+id/$id") return el
            }
            error("Missing Home view: $id")
        }
        val grouped = "@color/eve_system_grouped_background"
        assertEquals(grouped, xml.getAttribute("android:background"))
        for (id in listOf("mainContentContainer", "topBarContainer", "searchTopBar",
            "maintenanceOverlay", "forceUpdateOverlay")) {
            assertEquals("$id should have the full-window Home surface",
                grouped, view(id).getAttribute("android:background"))
        }
        val sheet = view("contentSheet")
        assertEquals("@drawable/bg_content_sheet", sheet.getAttribute("android:background"))
        assertEquals("Negative sheet overlap should be removed", "",
            sheet.getAttribute("android:layout_marginTop"))
        val sheetDrawable = parser.parse(org.xml.sax.InputSource(java.io.StringReader(
            source("src/main/res/drawable/bg_content_sheet.xml")))).documentElement
        assertEquals("Home background must have no rounded sheet corners", 0,
            sheetDrawable.getElementsByTagName("corners").length)
        assertEquals(grouped, (sheetDrawable.getElementsByTagName("solid").item(0) as Element)
            .getAttribute("android:color"))
        val helper = source("src/main/java/com/eve/app/util/SystemBarHelper.kt")
        assertTrue(helper.contains("activity is MainActivity"))
        assertTrue(helper.contains("if (isHome) R.color.eve_system_grouped_background else R.color.eve_bg"))
        assertTrue(helper.contains("window.navigationBarColor = if (isHome) background else"))
        assertTrue(helper.contains("activity.findViewById<View>(android.R.id.content)?.setBackgroundColor(background)"))
    }

    @Test
    fun homeGroupedSurfaceAndExamCardsUseMatchingSemanticRoles() {
        fun source(path: String): String {
            val candidates = listOf(File(path), File("app/$path"), File("../app/$path"))
            return candidates.firstOrNull { it.exists() }?.readText()
                ?: error("Missing Home file: $path")
        }

        val sheet = source("src/main/res/drawable/bg_content_sheet.xml")
        val layout = source("src/main/res/layout/item_exam.xml")
        val adapter = source("src/main/java/com/eve/app/ui/home/ExamAdapter.kt")
        assertTrue(sheet.contains("@color/eve_system_grouped_background"))
        assertTrue(layout.contains("@style/Widget.Eve.Card.Group"))
        assertTrue(adapter.contains("R.color.eve_secondary_system_grouped_background"))

        val light = loadColors("src/main/res/values/colors.xml")
        val dark = loadColors("src/main/res/values-night/colors.xml")
        assertEquals("#F2F2F7", light["eve_system_grouped_background"])
        assertEquals("#FFFFFF", light["eve_secondary_system_grouped_background"])
        assertEquals("#000000", dark["eve_system_grouped_background"])
        assertEquals("#1C1C1E", dark["eve_secondary_system_grouped_background"])
    }
}
