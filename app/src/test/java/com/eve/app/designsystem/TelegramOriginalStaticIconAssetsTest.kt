package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Exact SHA-256 byte parity with Telegram-master source ZIP WebPs; no approximated vectors. */
class TelegramOriginalStaticIconAssetsTest {
    private val sourceHashes = mapOf(
        "ic_back" to mapOf(
            "mdpi" to "dc131a7de3faea33a5b22ddff0a2c1f380a17740af5c816ee7dab330e92a323b",
            "hdpi" to "f15cfdfff716d710d70701ba6f1244f0e45380fa9592674f1e9a79aeaadd4d7f",
            "xhdpi" to "3c485929ec2487a9ca53f9f52b2ab91da0595456ac34d39b1cc90be280840b8d",
            "xxhdpi" to "a1a41777fb63cefd07aa7480687c55cfcf1ea0d2a8dda35c312e466e1a1195ea"
        ),
        "ic_more_vert" to mapOf(
            "mdpi" to "1753372e5d4e9da59a39e0828271be283828cc2f09e319ed01e170c6e2b71ee3",
            "hdpi" to "b6a2c9b85adaab7c63b9f4ab1b5e5e87c50aed1302ec0fc4b1faa0fd65d97780",
            "xhdpi" to "0e61b9b1b27198c178f407ee78590dbfa9e624bf4a59131394c5f25b9cbde9ec",
            "xxhdpi" to "0ec992659736897f869e2fa5baad353cb1c4d0f4b0f35ef3cf94de3c067cb499"
        ),
        "ic_search" to mapOf(
            "mdpi" to "c31d2043aec83ca82f4f5fe7cdba64882744ce0e6f0e2ad1adae8ec9af5acbbe",
            "hdpi" to "fd3b4a1c77b300853815e9e3a8706087e6eac5de04e3ba31da61a1fddfde5e79",
            "xhdpi" to "e8e30acecf18ed0f0eafa871a46481ec3d0c31b2dec2a4c847fcac2da5f14a0d",
            "xxhdpi" to "478962bae4ca0c8d48ba0a8b3eebe5900d063f3a47380bdb9448fc0d43317ca6"
        )
    )
    @Test fun fourDensityWebpFilesAreVerbatimFromOfficialSource() {
        val base = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        for ((resource, expected) in sourceHashes) {
            assertTrue("Old redraw must be removed: $resource",
                !File(base, "drawable/$resource.xml").exists())
            for ((density, sha) in expected) {
                val file = File(base, "drawable-$density/$resource.webp")
                assertTrue("Missing WebP: ${file.path}", file.length() > 0L)
                val actual = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                    .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
                assertEquals("${file.path} mismatched official source bytes", sha, actual)
            }
        }
    }
}
