package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EveQrScannerContractTest {
    @Test fun onDemandCameraIsPrivateAndOnlyOpensEveLinks() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val scanner = File("src/main/java/com/eve/app/ui/profile/EveQrLiveScannerActivity.kt").readText()
        assertTrue(manifest.contains("android.permission.CAMERA"))
        assertTrue(manifest.contains("EveQrLiveScannerActivity\" android:exported=\"false"))
        assertTrue(scanner.contains("ActivityResultContracts.RequestPermission()"))
        assertTrue(scanner.contains("ProfileQrPayload.parse(link)"))
        assertFalse(scanner.contains("Browser.openUrl"))
        assertFalse(scanner.contains("ConnectionsManager"))
        assertFalse(scanner.contains("startActivity(Intent(Intent.ACTION_VIEW"))
        assertEquals("test_name", ProfileQrPayload.parse("eve-mock-test://username/test_name"))
        assertNull(ProfileQrPayload.parse("https://t.me/example"))
        assertTrue(scanner.contains("inJustDecodeBounds = true"))
        assertTrue(scanner.contains("QrScanImageSizing.sampleSize"))
        assertTrue(scanner.contains("isDestroyed || isFinishing || closing"))
        assertFalse(scanner.contains("inSampleSize = 4"))
        // Upstream QrScanOverlay.java:101-112 uses exactly these source curves
        // and a 150ms decode interval; UI geometry remains ADAPTED.
        assertTrue(scanner.contains("CubicBezierInterpolator(0.05, 0.7, 0.1, 1.0)"))
        assertTrue(scanner.contains("CubicBezierInterpolator(0.3, 0.0, 0.1, 1.0)"))
        assertTrue(scanner.contains("now - lastScanAt < 150L"))
        assertTrue(scanner.contains("QrScannerFrameOverlay(this)"))
        assertTrue(scanner.contains("QrControlBackground(dp(20).toFloat())"))
        assertFalse(scanner.contains("GradientDrawable"))

    }
}
