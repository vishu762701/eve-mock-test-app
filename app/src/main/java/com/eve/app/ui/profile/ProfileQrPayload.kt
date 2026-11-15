package com.eve.app.ui.profile

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URI
import java.util.Locale

/** Public, editable username only. This is never authentication or proof of ownership. */
internal object ProfileQrPayload {
    private val username = Regex("[A-Za-z][A-Za-z0-9_]{3,30}[A-Za-z0-9]")
    fun normalize(value: String): String? = value.removePrefix("@").takeIf { username.matches(it) }?.lowercase(Locale.ROOT)
    fun encode(value: String): String? = normalize(value)?.let { "eve-mock-test://username/$it" }
    fun parse(value: String): String? {
        if (value.length > 128) return null
        return runCatching {
            val uri = URI(value)
            if (uri.scheme != "eve-mock-test") return null
            val auth = uri.rawAuthority ?: return null
            if (uri.rawQuery != null || uri.rawFragment != null) return null
            val path = uri.rawPath ?: return null
            if (!path.startsWith("/") || path.count { it == '/' } != 1) return null
            val identifier = path.substring(1)
            if (auth == "username") {
                normalize(identifier)
            } else if (auth == "user") {
                identifier.takeIf { it.isNotBlank() }
            } else null
        }.getOrNull()
    }
    fun matrix(value: String, size: Int = 512) = QRCodeWriter().encode(
        encode(value) ?: if (value.startsWith("eve-mock-test://")) value else "eve-mock-test://user/$value",
        BarcodeFormat.QR_CODE, size, size,
        mapOf(EncodeHintType.MARGIN to 4)
    )
}
