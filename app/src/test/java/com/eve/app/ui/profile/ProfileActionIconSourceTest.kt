package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class ProfileActionIconSourceTest {
    private fun gitBlob(path: String): String {
        val bytes = File("src/main/res/drawable/$path").readBytes()
        val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-1")
            .digest(header + bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    @Test fun allSixTelegramVectorsAreOriginalByteExact() {
        val originals = mapOf(
            "telegram_profile_photo.xml" to "eeb444043a1604a691a408d6d4161f62d09ad76a",
            "telegram_profile_edit.xml" to "4419e6284f9fd2d67250165904636d5bdc459369",
            "telegram_profile_settings.xml" to "67dbfcfac15413f0308c65e0403a34ec76b2f87f",
            "telegram_profile_outline_photo.xml" to "11e2373136ba8ee9dba936df41a89cc74dcd7bea",
            "telegram_profile_outline_edit.xml" to "6b30b4aec7ebcd8dabfa6ec48786777c8f326276",
            "telegram_profile_outline_settings.xml" to "eea35faa4a59b617603d8a9fad67a8b3c4b7ceef"
        )
        for ((path, upstreamBlob) in originals) {
            assertEquals("Original Telegram drawable bytes must not be approximated: $path",
                upstreamBlob, gitBlob(path))
        }
    }

    @Test fun lightActionButtonsUseOriginalOutlinesAndDarkUseOriginalFilledGlyphs() {
        val light = ProfileActionIcons.resources(darkSurface = false)
        val dark = ProfileActionIcons.resources(darkSurface = true)
        assertTrue(light.photo != dark.photo)
        assertTrue(light.edit != dark.edit)
        assertTrue(light.settings != dark.settings)
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        assertTrue(source.contains("override fun onApplyThemePalette("))
        assertTrue(source.contains("ProfileActionIcons.applyTo("))
        assertTrue(source.contains("darkSurface = palette.isDark"))
        assertTrue(source.contains("super.onApplyThemePalette(palette)"))
    }
}
