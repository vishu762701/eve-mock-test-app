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
        assertTrue(scanner.contains("isFinishing || isDestroyed"))
        assertFalse(scanner.contains("inSampleSize = 4"))
        // Upstream QrScanOverlay.java:101-112 uses exactly these source curves
        // and a 150ms decode interval; UI geometry remains ADAPTED.
        val source = File("src/main/java/com/eve/app/telegram/qr/QrScanMorph.java").readText()
        assertTrue(source.contains("CubicBezierInterpolator(0.05, 0.7, 0.1, 1.0)"))
        assertTrue(source.contains("CubicBezierInterpolator(0.3, 0.0, 0.1, 1.0)"))
        assertTrue(scanner.contains("root.animateSourceOpen"))
        assertTrue(scanner.contains("root.animateSourceClose"))
        assertTrue(scanner.contains("root.captureCloseFrame(preview)"))
        // Found-code pixels come from real ZXing result points mapped by
        // CameraX with the same shared ViewPort, never approximate corners.
        assertTrue(scanner.contains("result.resultPoints?.takeIf { it.size == 4 }"))
        assertTrue(scanner.contains("QrScanFound.toUnrotated"))
        assertTrue(scanner.contains("ImageProxyTransformFactory()"))
        assertTrue(scanner.contains("CoordinateTransform(transform, target).mapPoints(xy)"))
        assertTrue(scanner.contains("UseCaseGroup.Builder()"))
        assertTrue(scanner.contains("group.setViewPort(viewPort)"))
        assertTrue(scanner.contains("QrScanFound.FOUND_DELAY_MS"))
        assertTrue(scanner.contains("shouldShowRequestPermissionRationale"))
        assertTrue(scanner.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(scanner.contains("Settings.ACTION_APPLICATION_DETAILS_SETTINGS"))
        assertTrue(scanner.contains("override fun onResume()"))
        assertTrue(scanner.contains("if (cameraBinding || provider != null"))
        assertTrue(scanner.contains("torchState.request()"))
        assertTrue(scanner.contains("future.addListener"))
        assertTrue(scanner.contains("torchState.complete(request, success)"))
        assertTrue(scanner.contains("torchState.reset()"))
        assertTrue(scanner.contains("gallery.launch(\"image/*\")"))
        assertTrue(scanner.contains("override fun onPause()"))
        assertTrue(scanner.contains("cameraSession.acceptsFrame(ticket)"))
        assertTrue(scanner.contains("cancelFoundNavigation()"))
        assertTrue(scanner.contains("lifecycle.withResumed"))
        assertTrue(manifest.contains("android:theme=\"@style/Theme.Eve.QrScene\""))
        assertTrue(scanner.contains("now - lastScanAt < 150L"))
        assertTrue(scanner.contains("QrScannerFrameOverlay(this)"))
        assertTrue(scanner.contains("root.scannerGlass(dp(20).toFloat())"))
        assertTrue(scanner.contains("preview.previewStreamState.observe(this)"))
        assertFalse(scanner.contains("GradientDrawable"))

    }
}
