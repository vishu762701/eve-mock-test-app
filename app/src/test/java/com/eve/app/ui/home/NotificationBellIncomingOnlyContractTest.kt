package com.eve.app.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NotificationBellIncomingOnlyContractTest {
    @Test fun inboxClickDoesNotFalselyAnimateButIncomingEventsDo() {
        val src = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val setup = src.substringAfter("private fun setupNotificationBell()")
            .substringBefore("private fun setupHomeBannerSurface()")
        val click = setup.substringAfter("binding.notificationAction.setOnClickListener")
        assertFalse("Navigation is not notification arrival", click.contains("animateNotificationBell()"))
        assertTrue("Local notification receiver still starts incoming animation",
            src.substringAfter("private val foregroundNotificationReceiver").substringBefore("private val notificationPermissionLauncher")
                .contains("animateNotificationBell()") ||
            src.substringAfter("private val foregroundNotificationReceiver").substringBefore("override fun onCreate")
                .contains("animateNotificationBell()"))
        assertTrue(src.contains("newCount > 0"))
        assertTrue(src.contains("pendingBellGeneration = null"))
        assertTrue(src.contains("bell.composition?.duration?.toLong()"))
    }
}
