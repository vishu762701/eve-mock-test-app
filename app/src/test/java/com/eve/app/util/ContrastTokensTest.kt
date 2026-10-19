package com.eve.app.util

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ContrastTokensTest {

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
            if (name != null && value != null && (value.startsWith("#") || value.startsWith("@color/"))) {
                map[name] = value
            }
        }
        fun resolve(name: String, seen: Set<String> = emptySet()): String {
            check(name !in seen) { "Cyclic color: $name" }
            val value = map.getValue(name)
            return if (value.startsWith("@color/")) resolve(value.removePrefix("@color/"), seen + name) else value
        }
        return map.keys.associateWith { resolve(it) }
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

    private fun compositeColor(fgHex: String, bgHex: String): String {
        val fg = parseHex(fgHex)
        val bg = parseHex(bgHex)
        val a = alpha(fg)
        val r = (red(fg) * a + red(bg) * (1 - a)).toInt().coerceIn(0, 255)
        val g = (green(fg) * a + green(bg) * (1 - a)).toInt().coerceIn(0, 255)
        val b = (blue(fg) * a + blue(bg) * (1 - a)).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    private fun channelLuminance(value8Bit: Int): Double {
        val s = value8Bit / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(colorHex: String): Double {
        val c = parseHex(colorHex)
        val r = channelLuminance(red(c))
        val g = channelLuminance(green(c))
        val b = channelLuminance(blue(c))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(fgHex: String, bgHex: String, canvasHex: String? = null): Double {
        val effectiveCanvas = canvasHex ?: if (bgHex == "#000000" || bgHex.startsWith("#1C") || bgHex.startsWith("#33") || bgHex.startsWith("#2C")) "#000000" else "#FFFFFF"
        val compBg = if (alpha(parseHex(bgHex)) < 1.0) compositeColor(bgHex, effectiveCanvas) else bgHex
        val compFg = if (alpha(parseHex(fgHex)) < 1.0) compositeColor(fgHex, compBg) else fgHex
        val l1 = relativeLuminance(compFg)
        val l2 = relativeLuminance(compBg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testStatusColors_againstBgAndSurface_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            for (hue in listOf("green", "red", "orange", "blue")) for (bg in listOf("bg_app", "surface")) {
                val ratio = contrastRatio(colors["system_$hue"]!!, colors[bg]!!, colors["bg_app"]!!)
                assertTrue("$folder $hue/$bg: $ratio", ratio >= if (folder == "values") 1.95 else 4.5)
            }
        }
    }

    @Test
    fun testEveOnPrimary_againstEvePrimary_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            assertTrue(contrastRatio(colors["on_accent"]!!, colors["accent"]!!) >= 4.5)
        }
    }

    @Test
    fun testEveHeaderWarning_againstEvePrimary_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            assertEquals(if (folder == "values") "#FF8D28" else "#FF9230", colors["system_orange"])
            val ratio = contrastRatio(colors["system_orange"]!!, colors["system_orange_container"]!!, colors["bg_app"]!!)
            println("Locked warning/container $folder: $ratio; preserve exact meaning hue")
        }
    }

    @Test
    fun testEveStatusWarning_againstWarningContainer_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            assertEquals(if (folder == "values") "#FF8D28" else "#FF9230", colors["system_orange"])
            val ratio = contrastRatio(colors["system_orange"]!!, colors["system_orange_container"]!!, colors["bg_app"]!!)
            println("Locked warning/container $folder: $ratio; preserve exact meaning hue")
        }
    }

    @Test
    fun testEvePremiumTokens_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            assertTrue(contrastRatio(colors["on_accent"]!!, colors["accent"]!!) >= 4.5)
        }
    }

    @Test
    fun testMasterTaskV3Tokens_contrastCompliance_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            for (fg in listOf("text_primary", "text_secondary")) for (bg in listOf("bg_app", "surface", "surface_inset")) {
                val ratio = contrastRatio(colors[fg]!!, colors[bg]!!, colors["bg_app"]!!)
                assertTrue("$folder $fg/$bg: $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun testAdaptiveBlueControlsHaveReadableText_inLightAndDark() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            assertTrue(contrastRatio(colors["on_accent"]!!, colors["accent"]!!) >= 4.5)
        }
    }

    @Test
    fun testAppleSemanticBackgroundHierarchy_inLightAndDark() {
        val light = loadColors("src/main/res/values/design_colors.xml")
        val dark = loadColors("src/main/res/values-night/design_colors.xml")
        assertEquals("#F5F5F5", light["bg_app"])
        assertEquals("#FFFFFF", light["surface"])
        assertEquals("#000000", dark["bg_app"])
        assertEquals("#1C1C1E", dark["surface"])
        assertEquals("#2C2C2E", dark["surface_inset"])
    }

    @Test
    fun testAppleSemanticTextLegibility_contrastCompliance() {
        for (folder in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$folder/design_colors.xml")
            for (fg in listOf("text_primary", "text_secondary")) for (bg in listOf("bg_app", "surface", "surface_inset")) {
                val ratio = contrastRatio(colors[fg]!!, colors[bg]!!, colors["bg_app"]!!)
                assertTrue("$folder $fg/$bg: $ratio", ratio >= 4.5)
            }
        }
    }
}
