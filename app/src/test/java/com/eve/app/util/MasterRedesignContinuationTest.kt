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
    fun testMenuAndDrawerGlassTokensAreTranslucentInBothThemes() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")

        // 3-dot overflow menu background must be translucent, not opaque white (#E5FFFFFF) or solid
        val lightMenuBg = lightColors["eve_menu_glass_bg"]
        assertNotNull("eve_menu_glass_bg must exist in light colors", lightMenuBg)
        assertTrue("Light menu glass must be translucent (start with #59)", lightMenuBg!!.startsWith("#59"))

        val darkMenuBg = darkColors["eve_menu_glass_bg"]
        assertNotNull("eve_menu_glass_bg must exist in dark colors", darkMenuBg)
        assertTrue("Dark menu glass must be translucent (start with #73)", darkMenuBg!!.startsWith("#73"))

        // Profile drawer tint must be translucent
        val lightDrawerTint = lightColors["eve_drawer_glass_tint"]
        assertNotNull("eve_drawer_glass_tint must exist in light colors", lightDrawerTint)
        assertTrue("Light drawer tint must be translucent (start with #59)", lightDrawerTint!!.startsWith("#59"))

        val darkDrawerTint = darkColors["eve_drawer_glass_tint"]
        assertNotNull("eve_drawer_glass_tint must exist in dark colors", darkDrawerTint)
        assertTrue("Dark drawer tint must be translucent (start with #73)", darkDrawerTint!!.startsWith("#73"))

        // Drawer base must be transparent to prevent covering blur
        assertEquals("@android:color/transparent", lightColors["eve_drawer_glass_base"])
        assertEquals("@android:color/transparent", darkColors["eve_drawer_glass_base"])
    }

    @Test
    fun testThemesUseFluidScaleNavigationAnimations() {
        val lightThemes = File("src/main/res/values/themes.xml").readText()
        val darkThemes = File("src/main/res/values-night/themes.xml").readText()

        assertTrue("Eve.WindowAnimation must use fluid_scale_enter", lightThemes.contains("@anim/fluid_scale_enter"))
        assertTrue("Eve.WindowAnimation must use fluid_scale_fade_out", lightThemes.contains("@anim/fluid_scale_fade_out"))
        assertTrue("Eve.WindowAnimation must use fluid_scale_fade_in", lightThemes.contains("@anim/fluid_scale_fade_in"))
        assertTrue("Eve.WindowAnimation must use fluid_scale_exit", lightThemes.contains("@anim/fluid_scale_exit"))

        assertFalse("Obsolete slide_in_right must not be in light themes", lightThemes.contains("@anim/slide_in_right"))
        assertFalse("Obsolete slide_out_right must not be in light themes", lightThemes.contains("@anim/slide_out_right"))
        assertFalse("Obsolete slide_in_left must not be in light themes", lightThemes.contains("@anim/slide_in_left"))
        assertFalse("Obsolete slide_out_left must not be in light themes", lightThemes.contains("@anim/slide_out_left"))

        assertFalse("Obsolete slide_in_left must not be in dark themes", darkThemes.contains("@anim/slide_in_left"))
        assertFalse("Obsolete slide_out_left must not be in dark themes", darkThemes.contains("@anim/slide_out_left"))
    }

    @Test
    fun testLockedTelegramThemeToggleRemainsUntouched() {
        val themeAnimatorFile = File("src/main/java/com/eve/app/util/ThemeSwitchAnimator.kt")
        val themeManagerFile = File("src/main/java/com/eve/app/util/ThemeManager.kt")

        assertTrue("ThemeSwitchAnimator must exist", themeAnimatorFile.exists())
        assertTrue("ThemeManager must exist", themeManagerFile.exists())

        val animatorCode = themeAnimatorFile.readText()
        assertTrue("Circular reveal logic must remain in ThemeSwitchAnimator", animatorCode.contains("ViewAnimationUtils.createCircularReveal"))
    }
}
