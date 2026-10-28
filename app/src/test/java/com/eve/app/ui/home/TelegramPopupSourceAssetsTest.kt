package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Verifies original unmodified Telegram source pixels, not rendered emulator fidelity. */
class TelegramPopupSourceAssetsTest {
    private data class Source(
        val density: String, val size: Int,
        val sha256: String, val gitBlob: String
    )

    private val source = listOf(
        Source("mdpi", 1161,
            "6557266c5b8caf3e2a293dfb3083494a2db77d00f7a829387356dbd261fef99f",
            "53fa939aa586826cb9edcacd5624aca5e1eb12ea"),
        Source("hdpi", 1903,
            "947bab8642f394213ebd617bd27e0187ce2b9d81eda9199b1bcde2a0c68b6f4e",
            "17d032ef07a97dede8c2edb5f0fa7dbdeae9b94b"),
        Source("xhdpi", 2803,
            "b17e6b374f698950f81507a409891aec5385e20c549922762516f18f883952f2",
            "a1ccada709280d953a0ae7175d06efcbb1fb91fa"),
        Source("xxhdpi", 4590,
            "9d78c7ec81e0938056a37177cef0899e338d817a23e7e4db4f820ec0d78a4505",
            "5275c8cd0ff41411cbf5621d56333395a7e871c8")
    )

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    @Test fun allFourTelegramPopupNinePatchesMatchImmutableSourceBytes() {
        for (expected in source) {
            val file = File(
                "src/main/res/drawable-${expected.density}/telegram_dialog_popup_fixed_alert4.9.png"
            )
            assertTrue("Source 9-patch missing: ${file.path}", file.isFile)
            val bytes = file.readBytes()
            assertEquals("Source image size changed: ${expected.density}", expected.size, bytes.size)
            assertEquals("Original SHA-256 mismatch: ${expected.density}",
                expected.sha256, hex(MessageDigest.getInstance("SHA-256").digest(bytes)))

            // Git hashes exactly: SHA-1("blob " + decimalLength + NUL + raw file).
            val md = MessageDigest.getInstance("SHA-1")
            md.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
            md.update(bytes)
            assertEquals("Original immutable upstream Git blob mismatch: ${expected.density}",
                expected.gitBlob, hex(md.digest()))
        }
    }
}
