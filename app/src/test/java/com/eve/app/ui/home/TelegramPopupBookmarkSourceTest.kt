package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Existing EVE bookmark image is exactly Telegram msg_saved.webp on all densities. */
class TelegramPopupBookmarkSourceTest {
    private data class Original(val density: String, val bytes: Int, val sha256: String, val gitBlob: String)
    private val assets = listOf(
        Original("mdpi", 134, "873ad24c7b1951de855a069a66e5e0f63500e24cbf8c3a4ab1099a182ae29f73", "e7cdcff6e452c07459dc31eac58af0f36d1c7d62"),
        Original("hdpi", 204, "7453f638e2a396bfdaa04910f1a7cb6eb304d88cc11d44b9f8bd8de4f9dcac06", "ec3097732ea3a2462e241386876ca93a00af44d2"),
        Original("xhdpi", 226, "09b8b18ba5f5f161945483ea13aec029bc4d47dc271e330720cdff3f397b4c2c", "bf4fda65e037332f7bfe8f02a74d2ebc53238eba"),
        Original("xxhdpi", 270, "457066fd5447cd0389503b422a1eecee520cd17cbfd5b2c111d4a8c5f9b763c0", "63c064cafb3e217d55df9b31575352dc7c9efe66")
    )
    private fun hex(bytes: ByteArray) = bytes.joinToString("") {
        (it.toInt() and 255).toString(16).padStart(2, '0')
    }

    @Test fun savedQuestionMenuIconIsExactlyTelegramMsgSavedOriginal() {
        for (original in assets) {
            val file = File("src/main/res/drawable-${original.density}/ic_bookmark.webp")
            assertTrue("Unchanged Telegram Saved original missing: ${file.path}", file.isFile)
            val bytes = file.readBytes()
            assertEquals("Official original byte length ${original.density}", original.bytes, bytes.size)
            assertEquals("Original SHA-256 ${original.density}", original.sha256,
                hex(MessageDigest.getInstance("SHA-256").digest(bytes)))
            val sha = MessageDigest.getInstance("SHA-1")
            sha.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
            sha.update(bytes)
            assertEquals("Official source git blob ${original.density}", original.gitBlob,
                hex(sha.digest()))
        }
    }

    @Test fun bookmarksRowKeepsSavedOriginalNotBrowserSpecificIcon() {
        val xml = File("src/main/res/layout/popup_telegram_menu.xml").readText()
        val block = xml.substringAfter("android:id=\"@+id/menuRowBookmarks\"")
            .substringBefore("</LinearLayout>")
        assertTrue(block.contains("@drawable/ic_bookmark"))
        assertTrue(!block.contains("@drawable/telegram_menu_browser_bookmarks"))
        assertTrue(block.contains("android:minHeight=\"48dp\""))
        val source = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        assertTrue(source.contains("binding.menuRowBookmarks.setOnClickListener"))
        assertTrue(source.contains("dismissWithAction { onBookmarks() }"))
        assertTrue(source.contains("ImageViewCompat.setImageTintList(child"))
    }
}
