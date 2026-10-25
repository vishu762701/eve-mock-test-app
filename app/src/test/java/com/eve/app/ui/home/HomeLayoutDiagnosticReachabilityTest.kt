package com.eve.app.ui.home

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Protect the debug-only field diagnostic from being silently disconnected. */
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

    @Test fun diagnosticRemainsDebugOnly() {
        val debug = source("src/debug/java/com/eve/app/ui/home/HomeLayoutDiagnostic.kt")
        val release = source("src/release/java/com/eve/app/ui/home/HomeLayoutDiagnostic.kt")
        assertTrue(debug.contains("decor.addView(this"))
        assertTrue(debug.contains("EVE HOME DEBUG"))
        assertTrue("Unknown full-screen layers must be observable", debug.contains("contentRoot?.let"))
        assertTrue("Fullscreen layer data must include geometry", debug.contains("append(info("))
        assertFalse("Release may not add on-screen overlays", release.contains("addView("))
        assertFalse("Release may not log diagnostics", release.contains("Log."))
    }
}
