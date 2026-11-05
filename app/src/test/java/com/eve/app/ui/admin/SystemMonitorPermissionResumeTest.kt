package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import com.eve.app.R
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.google.gson.JsonParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.io.File

/** SystemMonitor Activity re-validates fresh server rights on resume without duplicating UI wiring. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35], application = Application::class,
    shadows = [
        ProfileEditorAuthFixture.ShadowAuth::class,
        EditExamBoundaryFixture.AdminBoundary::class,
        AdminApiTransportFixture.Transport::class
    ],
    instrumentedPackages = ["com.eve.app.data.repository", "com.eve.app.data.remote"]
)
class SystemMonitorPermissionResumeTest {

    private val viewRight = "view_analytics_logs"
    private val retryRight = "manage_generated_tests"

    private fun response(json: String) = ApiResponse(success = true, data = JsonParser.parseString(json).asJsonObject)

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf(viewRight, retryRight)
        )
        SystemMonitorBoundaryFixture.reset()
        AdminApiTransportFixture.current = SystemMonitorBoundaryFixture.transport()
        SystemMonitorBoundaryFixture.overview = response("""{"totals":{"users":1,"exams":1,"publishedTests":1,"questions":1,"attempts":1},"activity":[]}""")
        SystemMonitorBoundaryFixture.health = response("""{"database":{"status":"verified","detail":"Local fixture"}}""")
        SystemMonitorBoundaryFixture.operations = response("""{"nextOffset":null,"jobs":[],"submissions":[],"failures":[]}""")
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        AdminApiTransportFixture.current = null
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
        SystemMonitorBoundaryFixture.reset()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun open() = Robolectric.buildActivity(SystemMonitorActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    @Test fun returningActivityRechecksViewAnalyticsLogsPermissionViaFreshServerRead() {
        val c = open()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("Activity must finish when view_analytics_logs right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningActivityWithRetainedRightsSurvivesAndRefreshes() {
        val c = open()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            c.restart().resume().visible()
            idle()
            assertFalse("Activity must remain active when rights are intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun contractEnsuresNoDuplicateAdminControlsWiringOnResume() {
        val source = File("src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt").readText()
        val stop = source.substringAfter("override fun onStop()").substringBefore("override fun onCreate")
        assertTrue("onStop sets reauthorizeOnResume flag", stop.contains("reauthorizeOnResume = true"))

        val resume = source.substringAfter("override fun onResume()").substringBefore("override fun onStop()")
        assertTrue("onResume consumes reauthorizeOnResume", resume.contains("reauthorizeOnResume = false"))
        assertTrue("onResume guards identity change", resume.contains("live.uid != initiatingUid || live.email != initiatingEmail"))
        assertTrue("onResume re-checks AdminAccessGate with fresh server authority",
            resume.contains("AdminAccessGate.requireAdmin"))
        assertFalse("onResume must not duplicate section creation", resume.contains("overview = section"))
    }
}
