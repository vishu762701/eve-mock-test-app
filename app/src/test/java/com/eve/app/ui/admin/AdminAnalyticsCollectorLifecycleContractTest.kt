package com.eve.app.ui.admin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression for the FULL lint RepeatOnLifecycleWrongUsage gate. */
class AdminAnalyticsCollectorLifecycleContractTest {
    private val source = File("src/main/java/com/eve/app/ui/admin/AdminAnalyticsActivity.kt").readText()

    @Test fun observerRegistrationIsOnCreateOnlyNotOnResumeAuthorizationPath() {
        val creation = source.substringAfter("override fun onCreate(")
            .substringBefore("private fun setup()")
        val observer = creation.indexOf("repeatOnLifecycle(Lifecycle.State.STARTED)")
        val binding = creation.indexOf("binding = ActivityAdminAnalyticsBinding.inflate")
        val authorization = creation.lastIndexOf("authorizeAnalytics()")
        assertTrue("Collector should start after binding, before first protected read",
            binding >= 0 && observer > binding && authorization > observer)

        val setup = source.substringAfter("private fun setup()")
            .substringBefore("private fun selectedTimeRange()")
        val resume = source.substringAfter("override fun onResume()")
            .substringBefore("private val questionAdapter")
        assertFalse("Reauthorizing setup must not install another collector",
            setup.contains("repeatOnLifecycle("))
        assertFalse("onResume must not launch lifecycle collection",
            resume.contains("repeatOnLifecycle("))
        assertTrue("Existing permission check must still guard each published result",
            creation.contains("if (!currentAccess()) return@collect"))
    }
}
