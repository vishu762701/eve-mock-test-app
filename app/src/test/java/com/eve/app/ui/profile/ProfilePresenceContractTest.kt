package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfilePresenceContractTest {
    @Test fun onlyServerAuthLeaseMayDisplayOnline() {
        assertEquals("online", ProfilePresenceStatus.label(true))
        assertEquals("offline", ProfilePresenceStatus.label(false))
        assertEquals("Status unavailable", ProfilePresenceStatus.label(null))
        assertEquals("Status unavailable", ProfilePresenceStatus.label(null))
    }

    @Test fun globalForegroundUsesShortMultiDeviceLeasesAndOldWorkerFailsClosed() {
        val app = File("src/main/java/com/eve/app/EveApplication.kt").readText()
        val client = File("src/main/java/com/eve/app/util/ProfilePresenceClient.kt").readText()
        val api = File("src/main/java/com/eve/app/data/remote/EveApiService.kt").readText()
        val activity = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val gradle = File("build.gradle.kts").readText()
        assertTrue(app.contains("ProfilePresenceClient.register()"))
        assertTrue(app.contains("ProfilePresenceClient.onAuthChanged(auth.currentUser != null)"))
        assertTrue(gradle.contains("androidx.lifecycle:lifecycle-process:2.8.4"))
        for (s in listOf("ProcessLifecycleOwner.get().lifecycle.addObserver(this)",
            "UUID.randomUUID().toString()", "HEARTBEAT_MS = 30_000L",
            "onStop(owner: LifecycleOwner)", "scope.launch", "unsupportedByServer = true",
            "ApiClient.apiService.endPresence", "ApiClient.apiService.startPresence",
            "FirebaseAuth.getInstance().currentUser == null")) {
            assertTrue("Missing real presence lifecycle: $s", client.contains(s))
        }
        assertTrue(api.contains("val isOnline: Boolean? = null"))
        assertTrue(api.contains("hasBody = true"))
        assertTrue(activity.contains("ProfilePresenceStatus.label(profile.isOnline)"))
        assertFalse(activity.contains("binding.tvStatus.text = \"online\""))
    }
}
