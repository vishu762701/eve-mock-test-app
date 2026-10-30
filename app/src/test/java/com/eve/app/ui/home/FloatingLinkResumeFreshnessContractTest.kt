package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Home's position is public for all users; PUT remains server-admin only.
 * The resume freshness gate must not depend on the 10-minute singleton cache. */
class FloatingLinkResumeFreshnessContractTest {
    @Test fun resumeUsesAuthoritativePublicRefreshWithoutChangingAdminWrites() {
        val src = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val onResume = src.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")
        assertTrue("Returning students must see a changed admin position",
            onResume.contains("refreshFloatingLink(forceRefresh = true)"))
        val method = src.substringAfter("private fun refreshFloatingLink(forceRefresh: Boolean = false)")
            .substringBefore("private fun applyFloatingAirplaneTheme()")
        assertTrue(method.contains("getFloatingLinkData(forceRefresh = forceRefresh)"))
        assertTrue("Older async response must not restore stale coordinates",
            method.contains("generation != floatingLinkFetchGeneration"))
        assertTrue("Fresh public read must still update the ordinary user's position",
            method.contains("applyFloatingAirplanePosition(data?.x, data?.y)"))
        assertTrue("Only authorized admin long-press can submit coordinates",
            src.contains("if (!isFloatingAirplaneAdmin) return@Runnable"))
    }
}
