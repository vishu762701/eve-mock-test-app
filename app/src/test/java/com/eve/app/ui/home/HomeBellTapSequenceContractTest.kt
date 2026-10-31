package com.eve.app.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeBellTapSequenceContractTest {
    private val code = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()

    @Test fun tapRunsOneVisibleAnimationBeforeNavigationAndNeverMarksInboxRead() {
        val click = code.substringAfter("binding.notificationAction.setOnClickListener {")
            .substringBefore("private fun setupHomeBannerSurface()")
        assertTrue(click.contains("bellTapNavigationPending"))
        assertTrue(click.contains("animateNotificationBell()"))
        assertTrue(click.contains("postDelayed(openInbox, 850L)"))
        assertTrue(click.indexOf("animateNotificationBell()") < click.indexOf("postDelayed(openInbox, 850L)"))
        assertTrue(click.contains("EveNavigationHelper.navigate("))
        assertFalse(click.contains("NotificationStore.markAllRead"))
    }

    @Test fun navigationDoesNotSurvivePauseAndReducedMotionIsImmediate() {
        assertTrue(code.contains("bellTapNavigationEpoch++"))
        assertTrue(code.contains("bellTapNavigationPending = false"))
        assertTrue(code.contains("if (tapEpoch == bellTapNavigationEpoch && !isFinishing && !isDestroyed"))
        assertTrue(code.contains("!com.eve.app.designsystem.DesignMotion.enabled(this)"))
        assertTrue(code.contains("binding.btnNotification.composition == null"))
    }
}
