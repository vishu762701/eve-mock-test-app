package com.eve.app.ui.common

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProgressivePaletteSourcePhysicsTest {
    @Test fun entryUsesVerifiedTelegram320msQuintAndReducedMotion() {
        val src = File("src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt").readText()
        assertTrue(src.contains("CubicBezierInterpolator.EASE_OUT_QUINT"))
        assertTrue(src.contains(".setDuration(320L)"))
        assertTrue(src.contains("DesignMotion.enabled(view.context)"))
        assertTrue(src.contains("pendingReveals.remove(item.number)"))
        assertTrue(src.contains("onViewDetachedFromWindow"))
        assertTrue(src.contains("notifyItemRangeInserted(previous.size, appended.size)"))
    }
}
