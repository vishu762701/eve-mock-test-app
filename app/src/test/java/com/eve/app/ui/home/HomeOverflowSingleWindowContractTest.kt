package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Upstream ActionBarMenuItem.java:779-782 toggles its existing popup, never stacks windows. */
class HomeOverflowSingleWindowContractTest {
    @Test fun aRepeatedOverflowTapTogglesTheExistingPopupInsteadOfCreatingAnotherWindow() {
        val source = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val host = source.substringAfter("binding.btnOverflow.setOnClickListener { anchor ->")
            .substringBefore("// System back button closes drawer first")
        assertTrue(host.contains("val openPopup = homeOverflowPopup"))
        assertTrue(host.contains("if (openPopup?.isShowing == true)"))
        assertTrue(host.contains("openPopup.dismiss()"))
        assertTrue(host.contains("return@setOnClickListener"))
        assertTrue(host.contains("homeOverflowPopup = TelegramMenuPopup("))
        assertTrue(host.contains(").also { it.show(anchor) }"))
        assertTrue("Prevent opening after activity tear down",
            host.contains("isFinishing || isDestroyed"))
    }

    @Test fun activityDestructionCancelsCustomPopupMotionWithoutFiringNavigation() {
        val activity = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val onDestroy = activity.substringAfter("override fun onDestroy()")
            .substringBefore("override fun onSaveInstanceState")
        assertTrue(onDestroy.contains("homeOverflowPopup?.dismissForHostTeardown()"))
        assertTrue(onDestroy.contains("homeOverflowPopup = null"))
        val teardown = popup.substringAfter("fun dismissForHostTeardown()")
            .substringBefore("override fun dismiss()")
        assertTrue(teardown.contains("TelegramPopupSourceMotion.cancel(binding.root, actionRows())"))
        assertTrue(teardown.contains("GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)"))
        assertTrue(teardown.contains("if (isShowing) super.dismiss()"))
        assertTrue("No action callbacks when Activity is being destroyed",
            !teardown.contains("action()") && !teardown.contains("onLogout()"))
        assertTrue("User-initiated close keeps original 150ms exit",
            popup.contains("TelegramPopupSourceMotion.exit(binding.root, actionRows())"))
    }
}
