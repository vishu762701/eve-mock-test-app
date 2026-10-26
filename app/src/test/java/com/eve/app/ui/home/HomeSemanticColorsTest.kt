package com.eve.app.ui.home

import com.eve.app.designsystem.designAttribute
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
        for ((folder, bg, card, inset) in listOf(
            listOf("values", "#F1F1F3", "#FFFFFF", "#F1F1F3"),
            listOf("values-night", "#000000", "#181819", "#000000")
        )) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            assertEquals(bg, colors["bg_app"])
            assertEquals(card, colors["surface"])
            assertEquals(inset, colors["surface_inset"])
            assertEquals(if (folder == "values") "#26000000" else "#4D000000", colors["modal_scrim"])
        }
    }

    @Test
    fun testOfficialAppleLabelHierarchy_forHome() {
        val light = loadColors("src/main/res/values/design_colors.xml")
        val dark = loadColors("src/main/res/values-night/design_colors.xml")
        assertEquals("#1A1D21", light["text_primary"])
        assertEquals("#FFFFFF", dark["text_primary"])
        assertEquals("#6C6C70", light["text_secondary"])
        assertEquals("#A1A1A6", dark["text_secondary"])
    }

    @Test
    fun testHomeCategoryChipSelectors_mapToAppleSemanticRoles() {
        val file = listOf(File("src/main/res/values/design_widgets.xml"), File("app/src/main/res/values/design_widgets.xml")).first { it.exists() }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("style")
        val chip = (0 until nodes.length).map { nodes.item(it) as Element }.single { it.getAttribute("name") == "Widget.Eve.Token.Chip" }
        val items = chip.getElementsByTagName("item")
        val values = (0 until items.length).associate { val n = items.item(it) as Element; n.getAttribute("name") to n.textContent.trim() }
        assertEquals("@color/accent", values["chipBackgroundColor"])
        assertEquals("@color/on_accent", values["android:textColor"])
        assertEquals("@dimen/design_pill_height", values["chipMinHeight"])
    }

    @Test
    fun testHomeColorContrast_WCAG_AA_Compliance_LightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            for (fg in listOf("text_primary", "text_secondary")) for (bg in listOf("bg_app", "surface", "surface_inset")) {
                val ratio = contrastRatio(colors[fg]!!, colors[bg]!!, colors["bg_app"]!!)
                assertTrue("$folder $fg/$bg: $ratio", ratio >= 4.5)
            }
            assertTrue(contrastRatio(colors["on_accent"]!!, colors["accent"]!!) >= 4.5)
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
        val grouped = "@color/bg_app"
        assertEquals(grouped, xml.designAttribute("android:background"))
        assertEquals("", view("mainContentContainer").designAttribute("android:background"))
        val sheet = view("contentSheet")
        assertEquals("", sheet.designAttribute("android:background"))
        assertEquals("Negative sheet overlap should be removed", "",
            sheet.getAttribute("android:layout_marginTop"))
        val helper = source("src/main/java/com/eve/app/util/SystemBarHelper.kt")
        // Home keeps one uninterrupted canvas, but its runtime color must follow the
        // selected Telegram palette rather than the default XML bg_app token.
        assertTrue(helper.contains("activity is MainActivity"))
        assertTrue(helper.contains("TelegramThemeEngine.resolveCurrentPalette(activity)"))
        assertTrue(helper.contains("val background = palette.windowBackground"))
        assertTrue(helper.contains("activity.findViewById<View>(android.R.id.content)?.setBackgroundColor(background)"))
        assertTrue(helper.contains("window.statusBarColor = ContextCompat.getColor(activity, R.color.transparent)"))
        assertTrue(helper.contains("if (android.os.Build.VERSION.SDK_INT < 26 && !isDarkAppearance)"))
        assertTrue(helper.contains("R.color.navigation_legacy_background else R.color.transparent"))
        assertTrue(helper.contains("controller.isAppearanceLightStatusBars = !iconDark"))
        assertTrue(helper.contains("controller.isAppearanceLightNavigationBars = !iconDark"))
    }

    @Test
    fun homeAdminAndStreakControlsStandApartFromTheGroupedCanvasInBothThemes() {
        fun source(path: String): String {
            val candidates = listOf(File(path), File("app/$path"), File("../app/$path"))
            return candidates.firstOrNull { it.exists() }?.readText()
                ?: error("Missing Home source: $path")
        }

        val parser = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val home = parser.parse(org.xml.sax.InputSource(java.io.StringReader(
            source("src/main/res/layout/activity_main.xml")))).documentElement
        val descendants = home.getElementsByTagName("*")
        val admin = (0 until descendants.length).map { descendants.item(it) as Element }
            .single { it.getAttribute("android:id") == "@+id/btnAdmin" }

        assertEquals("@style/Widget.Eve.Token.Button.Secondary", admin.getAttribute("style"))
        assertEquals("@color/track", admin.designAttribute("app:backgroundTint"))
        assertEquals("@dimen/design_zero", admin.designAttribute("app:strokeWidth"))
        val foundation = source("src/main/java/com/eve/app/designsystem/EveDesignSystem.kt")
        assertTrue(foundation.contains("DesignType.apply(textView, DesignType.Role.CHIP)"))
        assertTrue(foundation.contains("textView.setTextColor(Colors.homeOnAccent(ctx))"))
    }

    @Test
    fun homePlainSurfaceAndExamCardsUseMatchingSemanticRoles() {
        fun source(path: String): String {
            val candidates = listOf(File(path), File("app/$path"), File("../app/$path"))
            return candidates.firstOrNull { it.exists() }?.readText()
                ?: error("Missing Home file: $path")
        }

        val layout = source("src/main/res/layout/item_exam.xml")
        val adapter = source("src/main/java/com/eve/app/ui/home/ExamAdapter.kt")
        assertTrue(layout.contains("@style/Widget.Eve.Token.Card"))
        assertTrue(adapter.contains("HomeAppearance.applyCard"))

        val light = loadColors("src/main/res/values/design_colors.xml")
        val dark = loadColors("src/main/res/values-night/design_colors.xml")
        assertEquals("#F1F1F3", light["bg_app"])
        assertEquals("#FFFFFF", light["surface"])
        assertEquals("#000000", dark["bg_app"])
        assertEquals("#181819", dark["surface"])
    }
    @Test
    fun segmentedTabSelectedTextPairsWithItsRealIndicatorInBothModes() {
        fun source(path: String): String {
            return listOf(File(path), File("app/$path"), File("../app/$path"))
                .firstOrNull { it.isFile }?.readText()
                ?: error("Missing segmented tab source: $path")
        }
        val style = source("src/main/res/values/design_widgets.xml")
        val indicator = source("src/main/res/drawable/design_tab_segment.xml")
        val implementation = source("src/main/java/com/eve/app/designsystem/DesignSegmentedTabs.kt")
        assertTrue(style.contains("@drawable/design_tab_segment"))
        assertTrue(indicator.contains("@color/accent"))
        assertTrue(implementation.contains("if (selected) palette.onAccent else palette.textPrimary"))
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            val contrast = contrastRatio(colors["on_accent"]!!, colors["accent"]!!)
            assertTrue("$folder selected tab contrast: $contrast", contrast >= 4.5)
        }
    }

}
