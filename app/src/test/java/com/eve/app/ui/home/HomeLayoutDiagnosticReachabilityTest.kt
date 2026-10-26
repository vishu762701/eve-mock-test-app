package com.eve.app.ui.home

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Protect Home layout diagnostic contract: log-only in debug, zero on-screen overlays, no-op in release. */
class HomeLayoutDiagnosticReachabilityTest {
    private fun source(path: String): String {
        val candidates = listOf(File(path), File("../app/$path"), File("app/$path"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: throw AssertionError("Source not found: $path")
    }

    @Test fun homeResumeReportsPillAndDrawerState() {
        val home = source("src/main/java/com/eve/app/ui/home/MainActivity.kt")
        val resume = home.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")
        assertTrue("Home resume must invoke layout diagnostic", resume.contains("HomeLayoutDiagnostic.report("))
        for (field in listOf("bottomPill", "drawerLayout", "maintenanceOverlay",
                "forceUpdateOverlay", "loadingHome", "progressGroup",
                "ivDrawerGlassBlurBackground", "mainContentContainer")) {
            assertTrue("Missing diagnostic input $field", resume.contains(field))
        }
    }

    @Test fun diagnosticRemainsDebugOnlyAndNeverAddsOnScreenOverlay() {
        val debug = source("src/debug/java/com/eve/app/ui/home/HomeLayoutDiagnostic.kt")
        val release = source("src/release/java/com/eve/app/ui/home/HomeLayoutDiagnostic.kt")
        assertFalse("Debug may not inject on-screen overlays or views into decor", debug.contains("addView("))
        assertFalse("Debug may not add views to decor", debug.contains("decor.addView"))
        assertFalse("Debug may not bring panel to front", debug.contains("bringToFront()"))
        assertTrue("Debug must log diagnostics via Log.d", debug.contains("Log.d("))
        assertTrue("Debug logs layout snapshot", debug.contains("EVE HOME DEBUG"))
        assertTrue("Unknown full-screen layers must be observable", debug.contains("contentRoot?.let"))
        assertTrue("Fullscreen layer data must include geometry", debug.contains("append(info("))
        assertFalse("Release may not add on-screen overlays", release.contains("addView("))
        assertFalse("Release may not log diagnostics", release.contains("Log."))
    }
}
