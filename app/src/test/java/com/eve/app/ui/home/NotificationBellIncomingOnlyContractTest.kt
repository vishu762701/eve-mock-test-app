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
        assertTrue(src.contains("newCount > 0 && hadBroadcastBaseline"))
        assertTrue(src.contains("NotificationStore.hasEstablishedBroadcastBaseline(this@MainActivity)"))
        assertTrue(src.contains("pendingBellGeneration = null"))
        assertTrue(src.contains("bell.composition?.duration?.toLong()"))
        assertTrue("Only incoming callbacks may queue a bell until Activity resumes",
            src.contains("pendingIncomingBellUntilResume = true"))
        val onResume = src.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")
        assertTrue("Queue must flush after Lifecycle dispatch", onResume.contains("binding.root.post {"))
        assertTrue("Previously acknowledged inbox content must not replay",
            onResume.contains("if (NotificationStore.hasUnread(this)) animateNotificationBell()"))
        assertFalse("Opening the inbox must not queue a fake notification",
            click.contains("pendingIncomingBellUntilResume"))

        val inbox = File("src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt").readText()
        assertTrue("Successful empty inbox sync must establish the first server baseline",
            inbox.contains("NotificationStore.importBroadcasts(this@NotificationsActivity, res.data.orEmpty())"))
        assertTrue("Inbox owns acknowledgement, not Home",
            inbox.contains("NotificationStore.markAllRead(this@NotificationsActivity)"))
    }
}
