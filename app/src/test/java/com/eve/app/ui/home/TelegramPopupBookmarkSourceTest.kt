package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Bookmarks menu artwork must be Telegram's actual four-density original. */
class TelegramPopupBookmarkSourceTest {
    private data class Original(val density: String, val bytes: Int, val sha256: String, val gitBlob: String)
    private val assets = listOf(
        Original("mdpi", 166, "7ae2785dabb6e9bf3d711a1ce582b666129da3235ac9348d0e3ac967846680f0", "ebc89f3d587e40abd03d6ebfa281dfa24e9a8756"),
        Original("hdpi", 214, "a2c4d7d9c31164425c9849777d5ce2164367b13f79331bc8452817a9579d1f6b", "17cd432a4dc0b6b82fd2348150ad4e18cb1cff8a"),
        Original("xhdpi", 250, "9569e339988dd9b2687c16f33b014332413601f6054781d49ead303682a6a877", "469163a675760e854044f8e82a95055a0e50d33c"),
        Original("xxhdpi", 304, "283d3123c1b194471b0cf9cb0c3669f94a06e66192355a81183d33ebd274349a", "9790581c0bbcf2075383a0033ed15b453b7c2626")
    )
    private fun hex(bytes: ByteArray) = bytes.joinToString("") {
        (it.toInt() and 255).toString(16).padStart(2, '0')
    }

    @Test fun exactOfficialTelegramBookmarksMenuImagesWereNotReencoded() {
        for (original in assets) {
            val file = File("src/main/res/drawable-${original.density}/telegram_menu_browser_bookmarks.webp")
            assertTrue("Official source image missing: ${file.path}", file.isFile)
            val bytes = file.readBytes()
            assertEquals("Original image size ${original.density}", original.bytes, bytes.size)
            assertEquals("Original SHA-256 ${original.density}", original.sha256,
                hex(MessageDigest.getInstance("SHA-256").digest(bytes)))
            val sha = MessageDigest.getInstance("SHA-1")
            sha.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
            sha.update(bytes)
            assertEquals("Official upstream Git object ${original.density}", original.gitBlob,
                hex(sha.digest()))
        }
    }

    @Test fun bookmarksRowUsesOfficialIconWithoutChangingAction() {
        val xml = File("src/main/res/layout/popup_telegram_menu.xml").readText()
        val block = xml.substringAfter("android:id=\"@+id/menuRowBookmarks\"")
            .substringBefore("android:id=\"@+id/menuRowMistakes\"")
        assertTrue(block.contains("@drawable/telegram_menu_browser_bookmarks"))
        assertTrue(!block.contains("@drawable/ic_bookmark"))
        assertTrue(block.contains("android:minHeight=\"48dp\""))
        val source = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        assertTrue(source.contains("binding.menuRowBookmarks.setOnClickListener"))
        assertTrue(source.contains("dismissWithAction { onBookmarks() }"))
        assertTrue(source.contains("ImageViewCompat.setImageTintList(child"))
    }
}
