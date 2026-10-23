package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Exact source bytes from the user-supplied Telegram-master Android ZIP. */
class TelegramAdditionalSourceIconAssetsTest {
    private val expected = mapOf(
        "ic_camera" to mapOf("mdpi" to "545cf38922349e4960b98f072d6421bbfc2d8ca220c623aa5f5e3adf045c2dcc", "hdpi" to "ea1830f00700d3f31a951781e65468a417414317554517f510717363f01daf52", "xhdpi" to "23385df81a23a1bc6762938ef71eb3165467ee095f066ef37c1421a125d0ca3b", "xxhdpi" to "92cacb2eeb6c1c4190e8f7c664678b6cf1eed62e69934ff88491580dbc3c3f8d"),
        "ic_copy" to mapOf("mdpi" to "f2c12012e8974a3ca52123044e5240543723b5af4efa832ce3959dce512fee50", "hdpi" to "4bf576bbaed88c0fd95473a0a712de1615363b694e6bd2f0b4780c15f99783d6", "xhdpi" to "44f3da840d0b029d85d02818cd88bc536943ff2d6312e49d61608e8272842d67", "xxhdpi" to "9818796f4fac8d2a1e1ae09ad923ac012d35426cafd5566c8fd7062c6c5e605d"),
        "ic_download" to mapOf("mdpi" to "e6f6ee0f50d3c6c0d4ad399d263d4f027f0800666218c38c16033902f941e2bb", "hdpi" to "acbce52dc236896807aadc7438a0973b839a44fbff32abdb4a1282da90e7d927", "xhdpi" to "7fd7b1b59b7f2a5266325b641eb78b2e56800a13d5808b811e22c49028069557", "xxhdpi" to "933815df791278b01ce382b863bddba612dfacb9ac7c721b91a6cf10d86ab73a"),
        "ic_info" to mapOf("mdpi" to "0f52a91c3db394f3ba7a7a07c4693e94fc37d8f8e8fa5d4358c2e743b37b0620", "hdpi" to "04a312aa8791511b843cdbb47516cdd58b520885590fe6d496aa1e5303289b37", "xhdpi" to "061d4b6f6c5846fcbfa2e3ac325899d9c42e6f49127fabecbf6d7bc3d13bdfe3", "xxhdpi" to "fd0801fc7d0bdcddb1a5365a57790c154aef76f71e107351c5e1675f7a107323"),
        "ic_link" to mapOf("mdpi" to "1d872ba286d05087409beb975b321ce5818b80218b8c1d9087aa3b74189999c7", "hdpi" to "faed86655fdbaeb6c97977f3185cc835d813d65e2ecda34b183b49c5bbe152f1", "xhdpi" to "ced8762979cd98dd5ed751c05f4319425028539a9861e02eec0b4a231e993cde", "xxhdpi" to "72e9124017de96198753c5e409b14fae28a347e6edd9ac664454c4b1e69b8eb7"),
        "ic_logout" to mapOf("mdpi" to "690e5db44024fdefa5290fc06da7bd1f1f4c7317874b41ca25df0c33fd75dc66", "hdpi" to "21b306f640308adcfb5e59e972d8cab869d0063d5074c839fc7f8f8040560261", "xhdpi" to "6d007eca4eedd8a32f17173520d4365382244e10a28b694354905315bfb0e6d6", "xxhdpi" to "6ee230dd08cd949c62e8353d64b67ffa1a114e7053052526d86b2192124f03c0"),
        "ic_send" to mapOf("mdpi" to "dabf91255165966a99c6b949e1e5c61a14aa2bdfae79dd043569af439a233c67", "hdpi" to "b308996b401bf5322f3f80fd9e8ecf30392727d785520bbee40405d287f59bd5", "xhdpi" to "d0333af3b7db3394e4985d3f98449db9b43403086bf60a415b181c4a87a22328", "xxhdpi" to "2b6366c5fab9ddb398e3a623a6cb4cf5b690a849ff96ede8b90212cc24e899eb")
    )

    @Test fun originalStaticAssetsKeepFourDensityVariantsAndSourceHash() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        for ((icon, densities) in expected) {
            assertTrue("Retired generic vector remains for $icon",
                !File(res, "drawable/$icon.xml").exists())
            for ((density, sha) in densities) {
                val path = File(res, "drawable-$density/$icon.webp")
                assertTrue("Missing original Telegram icon: $path", path.length() > 0L)
                val actual = MessageDigest.getInstance("SHA-256").digest(path.readBytes())
                    .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
                assertEquals("Non-exact Telegram glyph at $path", sha, actual)
            }
        }
    }
}
