package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Exact byte parity verification for Telegram complete mission imported WebP assets.
 */
class TelegramCompleteMissionIconsTest {
    private val expectedHashes = mapOf(
        "ic_report_flag_premium" to mapOf(
            "mdpi" to "8914c7faa580ae23c6c0b25679e82d8637b410231d148bb0850b00d0c600e7e8",
            "hdpi" to "9df4693ff62c93de0082a33bac9a1d44a0784b9ea4e2a6146994a0ddb978831b",
            "xhdpi" to "4beee6167a9164fd2fe90d986d54342bd5e5c372845b89f6ce867942474d6592",
            "xxhdpi" to "d0d565c1c699ac6828c99088ed7590e6ee058d3775c54b4b87ced7a80c603efe"
        ),
        "ic_history" to mapOf(
            "mdpi" to "b3c12bf78c7cb7b5ab9d04b3039c653d659d73b945e4b6e9be738b6ecdb9c21c",
            "hdpi" to "f4c2d0365304566139ea5fe7407ce1266e894af801eb2d44c03203ca0f29dd6c",
            "xhdpi" to "57236c540fb06486e0ae6774408ceae4cc031e48bdc97bcd856430dbf2fc1ce6",
            "xxhdpi" to "dd8bccb699ebf2ed52a8517a498cb9f47a26e41f782849d4190c8e4729a2cd65"
        ),
        "ic_add" to mapOf(
            "mdpi" to "16a19c8a27cb2ae608eceb53e50c13544900a903361129e218ddce732a1cd9b3",
            "hdpi" to "81a346f323ffd18cda8952127fc1f9b0f31747f8e7e1083ba1d0024e95caf9e5",
            "xhdpi" to "79e39cc14ae116ddc8cf79d03eeff9cb94a5297080f69e6f1139be2f7abb80df",
            "xxhdpi" to "11fca749f2b1798686325cb28a47808d63d235707335120192dbb511e885f042"
        ),
        "ic_language" to mapOf(
            "mdpi" to "cbf4776b4448c38c99b7a58dd8d7616589d7f842fd7af7f89c450226eb1c0518",
            "hdpi" to "6e47a0fa85bbb765f1a0fbe4631c2b254ab34a8a391a5e6963cf60d51229e2da",
            "xhdpi" to "30be702e10d00eb4004ebaad78067ef406fb884b575d9a3d6cf9b33b3013459f",
            "xxhdpi" to "1f9672b11e371e9b50b67a9c43f4c1cab4d043c0b40e9e3d003f867c0716e0a4"
        ),
        "ic_qr_code" to mapOf(
            "mdpi" to "8956948ac2bcca22d111a217d5740b4c9b20f495becc50387edddfe2977c7926",
            "hdpi" to "29e0d1890b97162e50d9cacc648eac359764722c9608e88bbb24f06bdc181dd6",
            "xhdpi" to "3a2341fe7d09b27234e40bfe542a129933b5addc6af916447fe9e8b8d30e954e",
            "xxhdpi" to "c11f7643f207764ba9fb3c79ef1db9b0b070a256c2cf3223bbe5e03b74203fc1"
        ),
        "ic_key" to mapOf(
            "mdpi" to "43f6ab528fb348e7b2372f2ce075c09135f0d9d9283af97bfaca4303d0ffeb44",
            "hdpi" to "3f609b9e397dcfdaa1a4a72615061d5a133363a16dac6a739a73ff1bb4816dd9",
            "xhdpi" to "7091bc225e9c2b248bad063a3fdc0ac45e4bd95c318422d73400ec3a6b62d450",
            "xxhdpi" to "9fd480c9df86ab39cef1bc5c81b8472d9c61c0c403421440481caba1310f98e9"
        ),
        "ic_person" to mapOf(
            "mdpi" to "e03d43425f45fe3128e5554ce18bcaeb9d957fced47a12d212b8f8aa78650b41",
            "hdpi" to "2c4f53592a8a27f6cf4d4a8239dd4b58bde440ee38f79698ff2b8ff275e316fd",
            "xhdpi" to "bbe11784d20aa77066c58eed91b73aa95d631f1a9f24caab2c4dc1ad99e6d453",
            "xxhdpi" to "3654a85acc11a37b41b90eb001f16a481b79b9fc67695ba0875e272655895687"
        ),
        "ic_user_profile" to mapOf(
            "mdpi" to "e03d43425f45fe3128e5554ce18bcaeb9d957fced47a12d212b8f8aa78650b41",
            "hdpi" to "2c4f53592a8a27f6cf4d4a8239dd4b58bde440ee38f79698ff2b8ff275e316fd",
            "xhdpi" to "bbe11784d20aa77066c58eed91b73aa95d631f1a9f24caab2c4dc1ad99e6d453",
            "xxhdpi" to "3654a85acc11a37b41b90eb001f16a481b79b9fc67695ba0875e272655895687"
        ),
        "ic_users" to mapOf(
            "mdpi" to "f5ab40cdf3de465a60ea83f3743e392698cda0f45e901871e496a679823a3410",
            "hdpi" to "cb3ed7970185639272810f744739052829d3b58cd4cc5cd884cacbed0ad490d6",
            "xhdpi" to "c9430873a6e0e3727e0c914933cb3db69a8375d5d0968dd47bf0e28a44f89778",
            "xxhdpi" to "4c782b9c8de860fea3ea916b40c5daaa784a02d3ea49ba60a2909ee5e63cbd8f"
        ),
        "ic_calendar" to mapOf(
            "mdpi" to "d81f98f5b5a2bb69a995c8ebe3df6951b0aaf8a53f55674f36ca9004fd6a159e",
            "hdpi" to "99e9f5ce32cda2b3fce142e91256e5ffda601dbdff91315373315a0d93de634f",
            "xhdpi" to "7d6551d529e796b083d85a99967f3ae3c171870b23756e2ecf766f4ba8665e03",
            "xxhdpi" to "dba1cf0239e6feab1f245e417e3fcc51be5a752da3228dd9f95788604cf3f41e"
        ),
        "ic_analytics" to mapOf(
            "mdpi" to "d0b7de6683a778ede8f565ed9042ed1b5ed38f05d961c3e8b63876ed72df83d2",
            "hdpi" to "bbadfda4097687ad64199cfea40b81ea5604745a6f93e9606ebbc22d3deefc64",
            "xhdpi" to "defb964e7ef12eea22eb832e23793605c499c8552119a2bc1e5d89bb59052571",
            "xxhdpi" to "3adf9185782f0033b2fd155cc0449fc29de7384a160fbd525dba47a567b1d281"
        ),
        "msg_home" to mapOf(
            "mdpi" to "ca3235c2fdf74ad823231b6955589608f19d6cc44dff1c9c1940005c1ad9112d",
            "hdpi" to "683894d81292455242e2e74a47e392e188c52242ecceace4d17c3550d1d7d32e",
            "xhdpi" to "1f6327367cfc294f885fff985bf52b811406e9c60146c6347357071f8fa1e2c3",
            "xxhdpi" to "f5c9a7a5f6994772db226e15cd35d3f62427a06b5b143e531d4e8eaf6143e566"
        ),
        "msg_theme" to mapOf(
            "mdpi" to "83ef697f1fab9f4a9d546e435d3256e1782ea02d7316dbc13375309fc530e40a",
            "hdpi" to "c683b52c94541be46b1f5146ba3c5604f936bf0942dddbdb334333d5bb2c06d8",
            "xhdpi" to "75a14a53378e60375c1205dbd3f6c491edb57a34d20027591fcda17746982bd8",
            "xxhdpi" to "58aa716a9b8cd6e487fcc86d9faefb1ee3271d0d405743161da102a11c7ee31c"
        ),
        "msg_palette" to mapOf(
            "mdpi" to "b733f47411634e5ae8f78be0d38d1706c3b923965c3d9042e0ae4d2f5cbbd521",
            "hdpi" to "8b09731eebd74355e170c70c5a069dcf622bd2029d6443f0ce83eeab6b6046cc",
            "xhdpi" to "fcbd956e4d67378e1aaa2b3ea527371a36072c0cb7b7f6cf93683e22b4920389",
            "xxhdpi" to "d57e4ea0a4d41061c5d8c5b50399c2ab4e86ed42c8e6aa79e66f6983d2b50859"
        )
    )

    @Test fun importedWebpAssetsMatchExactSourceHashes() {
        val base = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        for ((icon, densities) in expectedHashes) {
            assertTrue("Old vector must not exist for $icon", !File(base, "drawable/$icon.xml").exists())
            for ((density, expectedSha) in densities) {
                val file = File(base, "drawable-$density/$icon.webp")
                assertTrue("Missing WebP file: ${file.path}", file.isFile && file.length() > 0)
                val actualSha = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                assertEquals("${file.path} SHA-256 mismatch", expectedSha, actualSha)
            }
        }
    }
}
