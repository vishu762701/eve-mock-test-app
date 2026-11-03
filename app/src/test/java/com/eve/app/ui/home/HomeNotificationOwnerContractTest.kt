package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeNotificationOwnerContractTest {
    @Test fun asynchronousBroadcastsCannotCrossAccountInProductionHomePath() {
        val src = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val handler = src.substringAfter("private fun startListeningToNotifications()")
            .substringBefore("private fun updateNotificationDot()")
        val fetch = handler.indexOf("getBroadcasts(50)")
        val guard = handler.indexOf("if (requestUid != FirebaseAuth.getInstance().currentUser?.uid)")
        val write = handler.indexOf("NotificationStore.importBroadcasts(")
        assertTrue("Home must bind requests to its original signed-in owner", handler.contains("val requestUid = homeOwnerUid"))
        assertTrue("Owner guard must run after network suspension and before cache mutation",
            fetch >= 0 && guard > fetch && write > guard)
        assertTrue(handler.contains("catch (cancel: kotlinx.coroutines.CancellationException)"))
    }

    @Test fun unrelatedAccountCannotAnimateOrShowUnreadDotOnRetainedHome() {
        val src = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val receiver = src.substringAfter("private val foregroundNotificationReceiver")
            .substringBefore("private var isExamNavigating")
        assertTrue(receiver.contains("if (!searchOwnerCurrent) return"))
        val dot = src.substringAfter("private fun updateNotificationDot()")
            .substringBefore("private fun restoreNotificationBellIdle()")
        assertTrue(dot.contains("searchOwnerCurrent && NotificationStore.hasUnread(this)"))
    }
}
