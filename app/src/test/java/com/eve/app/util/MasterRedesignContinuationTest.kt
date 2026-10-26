package com.eve.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class MasterRedesignContinuationTest {

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
            if (name != null && value != null) {
                map[name] = value
            }
        }
        return map
    }

    @Test
    fun testMenuUsesApi31MaterialAndOlderFallbackWhileDrawerIsOpaque() {
        for (theme in listOf("values", "values-night")) {
            val colors = loadColors("src/main/res/$theme/design_colors.xml")
            assertTrue("Medium tint", colors["glass_tint"]!!.startsWith("#C2"))
            assertTrue("Unavailable blur fallback", colors["glass_fallback"]!!.startsWith("#C2"))
        }
        val home = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        assertTrue(home.contains("EveDesignSystem.Colors.surface(this)"))
    }

    @Test
    fun testThemesUseFluidScaleNavigationAnimations() {
        val lightThemes = File("src/main/res/values/themes.xml").readText()
        val darkThemes = File("src/main/res/values-night/themes.xml").readText()

        assertTrue("Eve.WindowAnimation must use design_screen_enter", lightThemes.contains("@anim/design_screen_enter"))
        assertTrue("Eve.WindowAnimation must use design_screen_recede", lightThemes.contains("@anim/design_screen_recede"))
        assertTrue("Eve.WindowAnimation must use design_screen_return", lightThemes.contains("@anim/design_screen_return"))
        assertTrue("Eve.WindowAnimation must use design_screen_exit", lightThemes.contains("@anim/design_screen_exit"))

        assertFalse("Obsolete slide_in_right must not be in light themes", lightThemes.contains("@anim/slide_in_right"))
        assertFalse("Obsolete slide_out_right must not be in light themes", lightThemes.contains("@anim/slide_out_right"))
        assertFalse("Obsolete slide_in_left must not be in light themes", lightThemes.contains("@anim/slide_in_left"))
        assertFalse("Obsolete slide_out_left must not be in light themes", lightThemes.contains("@anim/slide_out_left"))

        assertFalse("Obsolete slide_in_left must not be in dark themes", darkThemes.contains("@anim/slide_in_left"))
        assertFalse("Obsolete slide_out_left must not be in dark themes", darkThemes.contains("@anim/slide_out_left"))
    }

    @Test
    fun testThemeSwitchUsesSingleRecreationWithoutBitmapOverlay() {
        val themeAnimatorFile = File("src/main/java/com/eve/app/util/ThemeSwitchAnimator.kt")
        val themeManagerFile = File("src/main/java/com/eve/app/util/ThemeManager.kt")

        assertTrue("ThemeSwitchAnimator must exist", themeAnimatorFile.exists())
        assertTrue("ThemeManager must exist", themeManagerFile.exists())

        val animatorCode = themeAnimatorFile.readText()
        assertFalse("Captured system bars must not overlay the new theme", animatorCode.contains("PixelCopy"))
        assertFalse("Theme switching must not block all touch input", animatorCode.contains("FLAG_NOT_TOUCHABLE"))
        assertFalse("AppCompat owns recreation", animatorCode.contains("activity.recreate()"))
    }
}
