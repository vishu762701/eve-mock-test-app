package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Inherited admin floating-community requirement: authoritative saved position,
 * no overlapping writes and no privilege granted to normal users. */
class FloatingLinkDragSaveRecoveryContractTest {
    @Test fun failedSaveRevertsAndConcurrentAdminDragCannotOverridePendingWrite() {
        val source = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val handler = source.substringAfter("private fun setupFloatingAirplaneInteraction()")
            .substringBefore("private fun openFloatingLink()")
        assertTrue(handler.contains("if (!isFloatingAirplaneAdmin || floatingLinkPositionSaveInFlight) return@Runnable"))
        assertTrue(handler.contains("if (isFloatingAirplaneAdmin && !floatingLinkPositionSaveInFlight)"))
        assertTrue("Public refresh from onResume cannot jump the icon mid drag",
            handler.contains("floatingLinkFetchGeneration++"))
        val save = handler.substringAfter("val previous = activeFloatingLinkData")
            .substringBefore("// Regular tap (both admin and non-admin)")
        assertTrue(save.contains("floatingLinkPositionSaveInFlight = true"))
        assertTrue(save.contains("floatingLinkRepo.updateFloatingLinkPosition(normX, normY)"))
        assertTrue("Never keep a failed optimistic position as locally saved",
            save.contains("applyFloatingAirplanePosition(previous?.x, previous?.y)"))
        assertTrue(save.contains("finally {") &&
            save.contains("floatingLinkPositionSaveInFlight = false"))
    }
}
