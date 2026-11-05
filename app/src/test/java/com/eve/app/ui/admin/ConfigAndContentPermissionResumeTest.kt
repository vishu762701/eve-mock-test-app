package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import com.eve.app.R
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
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

/** ApiUsage and EditAbout Activities re-validate fresh server rights on resume without duplicating UI wiring. */
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
class ConfigAndContentPermissionResumeTest {

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf("view_analytics_logs", "manage_content")
        )
        ApiUsageBoundaryFixture.reset()
        AppContentBoundaryFixture.reset()
        AdminApiTransportFixture.current = ApiUsageBoundaryFixture.transport()
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        AdminApiTransportFixture.current = null
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
        ApiUsageBoundaryFixture.reset()
        AppContentBoundaryFixture.reset()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun openApiUsage() = Robolectric.buildActivity(ApiUsageActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    private fun openEditAbout(): org.robolectric.android.controller.ActivityController<EditAboutActivity> {
        AdminApiTransportFixture.current = AppContentBoundaryFixture.transport()
        return Robolectric.buildActivity(EditAboutActivity::class.java).apply {
            get().setTheme(R.style.Theme_Eve)
            setup().visible()
            idle()
        }
    }

    @Test fun returningApiUsageRechecksViewAnalyticsLogsPermissionViaFreshServerRead() {
        val c = openApiUsage()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("ApiUsage must finish when view_analytics_logs right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningApiUsageWithRetainedRightsSurvives() {
        val c = openApiUsage()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            c.restart().resume().visible()
            idle()
            assertFalse("ApiUsage must remain active when rights are intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningEditAboutRechecksManageContentPermissionViaFreshServerRead() {
        val c = openEditAbout()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("EditAbout must finish when manage_content right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningEditAboutWithRetainedRightsSurvives() {
        val c = openEditAbout()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            c.restart().resume().visible()
            idle()
            assertFalse("EditAbout must remain active when rights are intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun contractsEnsureNoDuplicateAdminControlsWiringOnResume() {
        val usageSource = File("src/main/java/com/eve/app/ui/admin/ApiUsageActivity.kt").readText()
        val usageStop = usageSource.substringAfter("override fun onStop()").substringBefore("private fun setupUi()")
        assertTrue("ApiUsage onStop sets reauthorizeOnResume flag", usageStop.contains("reauthorizeOnResume = true"))
        val usageResume = usageSource.substringAfter("override fun onResume()").substringBefore("override fun onStop()")
        assertTrue("ApiUsage onResume consumes reauthorizeOnResume", usageResume.contains("reauthorizeOnResume = false"))
        assertTrue("ApiUsage onResume guards identity change", usageResume.contains("live.uid != initiatingUid || live.email != initiatingEmail"))
        assertTrue("ApiUsage onResume re-checks AdminAccessGate", usageResume.contains("AdminAccessGate.requireAdmin"))

        val aboutSource = File("src/main/java/com/eve/app/ui/admin/EditAboutActivity.kt").readText()
        val aboutStop = aboutSource.substringAfter("override fun onStop()").substringBefore("override fun onSaveInstanceState")
        assertTrue("EditAbout onStop sets reauthorizeOnResume flag", aboutStop.contains("reauthorizeOnResume = true"))
        val aboutResume = aboutSource.substringAfter("override fun onResume()").substringBefore("override fun onStop()")
        assertTrue("EditAbout onResume consumes reauthorizeOnResume", aboutResume.contains("reauthorizeOnResume = false"))
        assertTrue("EditAbout onResume guards identity change", aboutResume.contains("live.uid != initiatingUid || live.email != initiatingEmail"))
        assertTrue("EditAbout onResume re-checks AdminAccessGate", aboutResume.contains("AdminAccessGate.requireAdmin"))
    }
}
