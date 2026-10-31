package com.eve.app.ui.profile

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test

class ProfileQrPayloadTest {
    @Test fun realQrRoundTripContainsOnlyTheEditableUsername() {
        val payload = ProfileQrPayload.encode("@Alice_123")!!
        assertEquals("eve-mock-test://username/alice_123", payload)
        val matrix = ProfileQrPayload.matrix("Alice_123")
        val pixels = IntArray(matrix.width * matrix.height) { if (matrix[it % matrix.width, it / matrix.width]) 0xff000000.toInt() else -1 }
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))).text
        assertEquals(payload, decoded)
        assertEquals("alice_123", ProfileQrPayload.parse(decoded))
        assertFalse(payload.contains("uid")); assertFalse(payload.contains("phone")); assertFalse(payload.contains("dob"))
    }
    @Test fun scannedPayloadCannotBecomeAnExternalUrlOrCarryPrivateFields() {
        for (value in listOf("https://example.com/alice", "eve-mock-test://username/alice?phone=1234567890",
            "eve-mock-test://username/alice#token", "eve-mock-test://username@evil/alice",
            "eve-mock-test://username:80/alice", "eve-mock-test://username/alice/another",
            "eve-mock-test://username/%61lice", "eve-mock-test://username/../../alice", "x".repeat(5000))) {
            assertNull(value, ProfileQrPayload.parse(value))
        }
        for (name in listOf("", "ab", "_alice", "alice_", "12345", "a".repeat(33), "+911234567890", "alice@test")) assertNull(ProfileQrPayload.encode(name))
    }
}
