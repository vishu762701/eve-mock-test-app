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

/** AppConfig Activity re-validates fresh server rights on resume without duplicating UI wiring. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35], application = Application::class,
    shadows = [
        ProfileEditorAuthFixture.ShadowAuth::class,
        EditExamBoundaryFixture.AdminBoundary::class
    ],
    instrumentedPackages = ["com.eve.app.data.repository"]
)
class AppConfigPermissionResumeTest {

    private val configRight = "manage_config_maintenance"

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf(configRight, "manage_questions")
        )
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun open() = Robolectric.buildActivity(AppConfigActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    @Test fun returningActivityRechecksManageConfigPermissionViaFreshServerRead() {
        val c = open()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("Activity must finish when manage_config_maintenance right is revoked", c.get().isFinishing)
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
        val source = File("src/main/java/com/eve/app/ui/admin/AppConfigActivity.kt").readText()
        val stop = source.substringAfter("override fun onStop()").substringBefore("override fun onStart()")
        assertTrue("onStop sets reauthorizeOnResume flag", stop.contains("reauthorizeOnResume = true"))

        val resume = source.substringAfter("override fun onResume()").substringBefore("override fun onCreate")
        assertTrue("onResume consumes reauthorizeOnResume", resume.contains("reauthorizeOnResume = false"))
        assertTrue("onResume guards identity change", resume.contains("live.uid != configOwnerUid || live.email != configOwnerEmail"))
        assertTrue("onResume re-checks AdminAccessGate with fresh server authority",
            resume.contains("AdminAccessGate.requireAdmin"))
        assertFalse("onResume must not duplicate click listeners",
            resume.contains("setOnClickListener"))
    }
}
