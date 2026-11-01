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
    }
}
