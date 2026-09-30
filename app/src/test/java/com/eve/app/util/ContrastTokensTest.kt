package com.eve.app.util

import org.junit.Assert.assertTrue
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
            if (name != null && value != null && value.startsWith("#")) {
                map[name] = value
            }
        }
        return map
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

    private fun contrastRatio(fgHex: String, bgHex: String): Double {
        val l1 = relativeLuminance(fgHex)
        val l2 = relativeLuminance(bgHex)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testStatusColors_againstBgAndSurface_inLightAndDark() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")

        val lightBg = lightColors["eve_bg"]!!
        val lightSurface = lightColors["eve_surface"]!!

        val darkBg = darkColors["eve_bg"]!!
        val darkSurface = darkColors["eve_surface"]!!

        val statusTokens = listOf("eve_status_success", "eve_status_error", "eve_status_warning", "eve_status_info")

        // In light theme: status colors must have contrast >= 4.5:1 against eve_bg and eve_surface
        for (token in statusTokens) {
            val fg = lightColors[token]!!
            val ratioBg = contrastRatio(fg, lightBg)
            val ratioSurface = contrastRatio(fg, lightSurface)
            assertTrue("Light $token vs eve_bg must be >= 4.5 (was $ratioBg)", ratioBg >= 4.5)
            assertTrue("Light $token vs eve_surface must be >= 4.5 (was $ratioSurface)", ratioSurface >= 4.5)
        }

        // In dark theme: status colors must have contrast >= 4.5:1 against eve_bg and eve_surface
        for (token in statusTokens) {
            val fg = darkColors[token]!!
            val ratioBg = contrastRatio(fg, darkBg)
            val ratioSurface = contrastRatio(fg, darkSurface)
            assertTrue("Dark $token vs eve_bg must be >= 4.5 (was $ratioBg)", ratioBg >= 4.5)
            assertTrue("Dark $token vs eve_surface must be >= 4.5 (was $ratioSurface)", ratioSurface >= 4.5)
        }
    }

    @Test
    fun testEveOnPrimary_againstEvePrimary_inLightAndDark() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")

        val lightRatio = contrastRatio(lightColors["eve_on_primary"]!!, lightColors["eve_primary"]!!)
        assertTrue("Light eve_on_primary vs eve_primary must be >= 4.5 (was $lightRatio)", lightRatio >= 4.5)

        val darkRatio = contrastRatio(darkColors["eve_on_primary"]!!, darkColors["eve_primary"]!!)
        assertTrue("Dark eve_on_primary vs eve_primary must be >= 4.5 (was $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testEveHeaderWarning_againstEvePrimary_inLightAndDark() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")

        // Warning text/icon on top bar requires >= 3.0:1 UI element / bold indicator contrast (text/icon >= 4.5 in light, >= 4.5 in dark)
        val lightRatio = contrastRatio(lightColors["eve_header_warning"]!!, lightColors["eve_primary"]!!)
        assertTrue("Light eve_header_warning vs eve_primary must be >= 4.5 (was $lightRatio)", lightRatio >= 4.5)

        val darkRatio = contrastRatio(darkColors["eve_header_warning"]!!, darkColors["eve_primary"]!!)
        assertTrue("Dark eve_header_warning vs eve_primary must be >= 4.5 (was $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testEveStatusWarning_againstWarningContainer_inLightAndDark() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")

        val lightRatio = contrastRatio(lightColors["eve_status_warning"]!!, lightColors["eve_status_warning_container"]!!)
        assertTrue("Light eve_status_warning vs warning_container must be >= 4.5 (was $lightRatio)", lightRatio >= 4.5)

        val darkRatio = contrastRatio(darkColors["eve_status_warning"]!!, darkColors["eve_status_warning_container"]!!)
        assertTrue("Dark eve_status_warning vs warning_container must be >= 4.5 (was $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testEvePremiumTokens_inLightAndDark() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")
        // Explicit required contrast pairs:
        // dark: #E3FF3B on #000000 and #111111 must be >= 4.5:1
        val darkRatio000000 = contrastRatio(darkColors["eve_premium"]!!, "#000000")
        val darkRatio111111 = contrastRatio(darkColors["eve_premium"]!!, "#111111")
        assertTrue("Dark #E3FF3B on #000000 must be >= 4.5 (was $darkRatio000000)", darkRatio000000 >= 4.5)
        assertTrue("Dark #E3FF3B on #111111 must be >= 4.5 (was $darkRatio111111)", darkRatio111111 >= 4.5)
        // light: #111111 on #E3FF3B must be >= 4.5:1
        val lightRatio111111onLime = contrastRatio("#111111", lightColors["eve_premium"]!!)
        assertTrue("Light #111111 on #E3FF3B must be >= 4.5 (was $lightRatio111111onLime)", lightRatio111111onLime >= 4.5)

        // eve_premium_text against screen background (eve_bg) and surface (eve_surface) must be >= 4.5:1
        val lightTextRatioBg = contrastRatio(lightColors["eve_premium_text"]!!, lightColors["eve_bg"]!!)
        val lightTextRatioSurface = contrastRatio(lightColors["eve_premium_text"]!!, lightColors["eve_surface"]!!)
        assertTrue("Light eve_premium_text vs eve_bg must be >= 4.5 (was $lightTextRatioBg)", lightTextRatioBg >= 4.5)
        assertTrue("Light eve_premium_text vs eve_surface must be >= 4.5 (was $lightTextRatioSurface)", lightTextRatioSurface >= 4.5)

        val darkTextRatioBg = contrastRatio(darkColors["eve_premium_text"]!!, darkColors["eve_bg"]!!)
        val darkTextRatioSurface = contrastRatio(darkColors["eve_premium_text"]!!, darkColors["eve_surface"]!!)
        assertTrue("Dark eve_premium_text vs eve_bg must be >= 4.5 (was $darkTextRatioBg)", darkTextRatioBg >= 4.5)
        assertTrue("Dark eve_premium_text vs eve_surface must be >= 4.5 (was $darkTextRatioSurface)", darkTextRatioSurface >= 4.5)

        // eve_premium_text against eve_premium_container must be >= 4.5:1
        val lightContainerRatio = contrastRatio(lightColors["eve_premium_text"]!!, lightColors["eve_premium_container"]!!)
        assertTrue("Light eve_premium_text vs eve_premium_container must be >= 4.5 (was $lightContainerRatio)", lightContainerRatio >= 4.5)

        val darkContainerRatio = contrastRatio(darkColors["eve_premium_text"]!!, darkColors["eve_premium_container"]!!)
        assertTrue("Dark eve_premium_text vs eve_premium_container must be >= 4.5 (was $darkContainerRatio)", darkContainerRatio >= 4.5)

        // eve_on_premium against eve_premium must be >= 4.5:1
        val lightOnPremiumRatio = contrastRatio(lightColors["eve_on_premium"]!!, lightColors["eve_premium"]!!)
        assertTrue("Light eve_on_premium vs eve_premium must be >= 4.5 (was $lightOnPremiumRatio)", lightOnPremiumRatio >= 4.5)

        val darkOnPremiumRatio = contrastRatio(darkColors["eve_on_premium"]!!, darkColors["eve_premium"]!!)
        assertTrue("Dark eve_on_premium vs eve_premium must be >= 4.5 (was $darkOnPremiumRatio)", darkOnPremiumRatio >= 4.5)

        // eve_premium_line against screen background (eve_bg) and surface (eve_surface) must be >= 4.5:1
        val lightLineRatioBg = contrastRatio(lightColors["eve_premium_line"]!!, lightColors["eve_bg"]!!)
        val lightLineRatioSurface = contrastRatio(lightColors["eve_premium_line"]!!, lightColors["eve_surface"]!!)
        assertTrue("Light eve_premium_line vs eve_bg must be >= 4.5 (was $lightLineRatioBg)", lightLineRatioBg >= 4.5)
        assertTrue("Light eve_premium_line vs eve_surface must be >= 4.5 (was $lightLineRatioSurface)", lightLineRatioSurface >= 4.5)

        val darkLineRatioBg = contrastRatio(darkColors["eve_premium_line"]!!, darkColors["eve_bg"]!!)
        val darkLineRatioSurface = contrastRatio(darkColors["eve_premium_line"]!!, darkColors["eve_surface"]!!)
        assertTrue("Dark eve_premium_line vs eve_bg must be >= 4.5 (was $darkLineRatioBg)", darkLineRatioBg >= 4.5)
        assertTrue("Dark eve_premium_line vs eve_surface must be >= 4.5 (was $darkLineRatioSurface)", darkLineRatioSurface >= 4.5)
    }
}
