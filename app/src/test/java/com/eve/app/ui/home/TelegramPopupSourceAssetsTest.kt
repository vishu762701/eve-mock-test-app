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
        Source("mdpi", 611,
            "8ab86f9766f8b4501f3d2e29bc6d29c88efd03307d424d04d15ba42cf3824e22",
            "e757e48312943bcea7f4577fef9e5d6f48612f4c"),
        Source("hdpi", 664,
            "84aa21dbbb203d43f751faf1ffa628af3db468265e882fe790f30b240d3bab49",
            "a59945a2728562a4dbce645d8d6ea70f0b4a894a"),
        Source("xhdpi", 934,
            "561ff08468cbf8745e2622358809904466b5c72748fef97a742fdde83e0e3bfa",
            "a755c2b17917492c0e8200a9be0350019fa56e36"),
        Source("xxhdpi", 1385,
            "c0cf9ea52108442f76d39cc736f991e0c8a1ffd7615cf80d5f03db38d6453811",
            "7f92a8eaa31cd9c527da8e7f677642762122293a")
    )

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    @Test fun allFourTelegramPopupNinePatchesMatchImmutableSourceBytes() {
        for (expected in source) {
            val file = File(
                "src/main/res/drawable-${expected.density}/telegram_popup_fixed_alert2.9.png"
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
