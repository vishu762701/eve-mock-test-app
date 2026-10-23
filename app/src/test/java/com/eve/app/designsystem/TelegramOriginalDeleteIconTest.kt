package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class TelegramOriginalDeleteIconTest {
    @Test fun deleteIconMatchesActualUpstreamDensityFiles() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        assertFalse(File(res, "drawable/ic_delete.xml").exists())
        val hashes = mapOf(
            "mdpi" to "0701b4a4ebca6417762be23462cc3e5625a6cfd5bd0f40b44f2d193f567c4ae2",
            "hdpi" to "c7c5aaf309ce1202ab691edb91373ae7f2449e4ab8caf22a0905f2f598768b51",
            "xhdpi" to "53ad74f3c566461a3072a411f32c293899ab2572e48a937cbe79ef09d9fb16a7"
        )
        for ((density, expected) in hashes) {
            val icon = File(res, "drawable-$density/ic_delete.webp")
            assertTrue(icon.path, icon.isFile)
            val digest = MessageDigest.getInstance("SHA-256").digest(icon.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals(icon.path, expected, digest)
        }
        assertFalse("Upstream provides no xxhdpi delete source", File(res, "drawable-xxhdpi/ic_delete.webp").exists())
    }
}
