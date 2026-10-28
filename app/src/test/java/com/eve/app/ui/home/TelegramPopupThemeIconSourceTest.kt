package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Byte-exact verified official Telegram theme menu vectors; no re-drawn icons. */
class TelegramPopupThemeIconSourceTest {
    private fun digest(bytes: ByteArray, algorithm: String): String =
        MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2, '0')
        }

    @Test fun exactTelegramDayAndNightMenuIconsArePreserved() {
        val sources = mapOf(
            "telegram_menu_day_mode_24.xml" to Pair(
                "f7c0b2b36531b877a01e82a1193ff39ae59ed11784e9123c90fed541d7318c84",
                "53c1495614215b6e69fda039cb8f69464a3998f8"
            ),
            "telegram_menu_night_mode_24.xml" to Pair(
                "66f143dbffd1c5abbc85dae15de22161a6a7d3c13ded1fc0168051b2af41d861",
                "bf0364d4826af33b83d578d2eae1594395590294"
            )
        )
        for ((name, hashes) in sources) {
            val bytes = File("src/main/res/drawable/$name").readBytes()
            assertEquals("Official raw SHA-256 mismatch: $name",
                hashes.first, digest(bytes, "SHA-256"))
            val sha = MessageDigest.getInstance("SHA-1")
            sha.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
            sha.update(bytes)
            val gitSha = sha.digest().joinToString("") {
                (it.toInt() and 255).toString(16).padStart(2, '0')
            }
            assertEquals("Official Git blob mismatch: $name", hashes.second, gitSha)
        }
    }

    @Test fun correctVerifiedOfficialIconIsSelectedForBothModes() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val switch = popup.substringAfter("private fun setupThemeCard()")
            .substringBefore("private fun setupListeners()")
        assertTrue(switch.contains("if (isDark)"))
        assertTrue(switch.contains("R.drawable.telegram_menu_day_mode_24"))
        assertTrue(switch.contains("R.drawable.telegram_menu_night_mode_24"))
        assertTrue("Preserve original theme reveal handoff",
            popup.contains("ThemeManager.reservePopupHandoff()"))
        assertTrue(!switch.contains("R.drawable.ic_theme_sun"))
    }
}
