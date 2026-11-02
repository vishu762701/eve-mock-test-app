package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class QrScannerFrameGeometryTest {
    @Test fun sourceRatioAndOffset() {
        val b = QrScannerFrameGeometry.bounds(1080, 2400, 3f)
        assertEquals(712.8f, b.width, .01f)
        assertEquals(712.8f, b.height, .01f)
        assertEquals(540f, (b.left + b.right) / 2f, .01f)
        assertEquals(1140f, (b.top + b.bottom) / 2f, .01f)
        val landscape = QrScannerFrameGeometry.bounds(900, 450, 2f)
        assertEquals(297f, landscape.width, .01f)
        assertEquals(185f, (landscape.top + landscape.bottom) / 2f, .01f)
    }

    @Test fun cameraLifecycleMatchesSourceOrdering() {
        val s = File("src/main/java/com/eve/app/ui/profile/EveQrLiveScannerActivity.kt").readText()
        assertTrue(s.contains("if (cameraRequestStarted || closing || completed.get()"))
        assertTrue(s.contains("QrScannerFrameOverlay(this)"))
        // The source 390ms shape morph now owns completion, not ViewPropertyAnimator.
        assertTrue(s.contains("root.animateSourceOpen { requestCameraAfterOpening() }"))
        assertTrue(s.contains("root.setAlreadyOpened()"))
        val close = s.substringAfter("private fun closeScanner(destination: Uri?) {")
            .substringBefore("override fun onDestroy()")
        val snapshot = close.indexOf("root.captureCloseFrame(preview)")
        val release = close.indexOf("provider?.unbindAll()")
        val beginMorph = close.indexOf("root.animateSourceClose {")
        val finished = close.indexOf("finish()")
        assertTrue("close must capture before unbinding CameraX", snapshot >= 0 && snapshot < release)
        assertTrue("close must release camera before morph", release >= 0 && release < beginMorph)
        assertTrue("finish must occur after close animation callback",
            beginMorph >= 0 && beginMorph < finished)
        assertTrue("duplicate results must be rejected before scanner dismissal",
            s.contains("if (!completed.compareAndSet(false, true)) return"))
    }
}
