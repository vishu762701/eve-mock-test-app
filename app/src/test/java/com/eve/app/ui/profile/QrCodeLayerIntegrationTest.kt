package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class QrCodeLayerIntegrationTest {
    @Test fun originalJavaRendererRunsBehindEveBranding() {
        val src = File("src/main/java/com/eve/app/telegram/qr/QrCodeLayer.java").readText()
        val host = File("src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt").readText()
        assertTrue(src.contains("public final class QrCodeLayer"))
        assertTrue(src.contains("public void assemble(Code code)"))
        assertTrue(src.contains("drawAssemble(canvas"))
        assertTrue(src.contains("drawVertices(Canvas.VertexMode.TRIANGLES"))
        assertFalse(src.contains("import org.telegram.messenger.SharedConfig"))
        assertTrue(host.contains("QrCodeLayer.build("))
        assertTrue(host.contains("qrLayer.assemble(data)"))
        assertTrue(host.contains("qrLayer.isAnimating()"))
        assertTrue(host.contains("QrCodeLayer.logoShare(data)"))
        assertTrue(host.contains("layer.setCardBounds(bounds.left"))
        assertTrue(host.contains("if (export)"))
        val notices = File("../THIRD_PARTY_NOTICES.md").readText()
        assertTrue(notices.contains("b9b5fcb69f538dfbc7dd87f628fe261ce1e69923"))
        assertTrue(notices.contains("QR Java QrCodeLayer integration"))
        assertFalse(host.contains("qrBitmap"))
        assertTrue(host.contains("qrCode = null"))
    }
}
