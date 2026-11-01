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
        assertTrue(s.contains(".withEndAction { requestCameraAfterOpening() }"))
        assertTrue(s.contains("if (cameraRequestStarted || closing || completed.get()"))
        assertTrue(s.contains("QrScannerFrameOverlay(this)"))
        assertTrue(s.indexOf("provider?.unbindAll()\n        camera = null") <
            s.indexOf(".withEndAction { finish() }.start()"))
    }
}
