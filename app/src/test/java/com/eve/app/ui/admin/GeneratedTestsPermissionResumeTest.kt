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

/** GeneratedTests Activity re-validates fresh server rights on resume without duplicating UI wiring. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35], application = Application::class,
    shadows = [
        ProfileEditorAuthFixture.ShadowAuth::class,
        EditExamBoundaryFixture.AdminBoundary::class,
        EditExamBoundaryFixture.ExamBoundary::class
    ],
    instrumentedPackages = ["com.eve.app.data.repository"]
)
class GeneratedTestsPermissionResumeTest {

    private val testRight = "manage_generated_tests"

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf(testRight)
        )
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun open() = Robolectric.buildActivity(GeneratedTestsActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    @Test fun returningActivityRechecksManageGeneratedTestsPermissionViaFreshServerRead() {
        val c = open()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("Activity must finish when manage_generated_tests right is revoked", c.get().isFinishing)
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
        val source = File("src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt").readText()
        val stop = source.substringAfter("override fun onStop()").substringBefore("override fun onSaveInstanceState")
        assertTrue("onStop sets reauthorizeOnResume flag", stop.contains("reauthorizeOnResume = true"))

        val resume = source.substringAfter("override fun onResume()").substringBefore("override fun onStop()")
        assertTrue("onResume consumes reauthorizeOnResume", resume.contains("reauthorizeOnResume = false"))
        assertTrue("onResume guards identity change", resume.contains("live.uid != authorizedUid || live.email != authorizedEmail"))
        assertTrue("onResume re-checks AdminAccessGate with fresh server authority",
            resume.contains("AdminAccessGate.requireAdmin"))
        assertFalse("onResume must not duplicate adapter or listener setup",
            resume.contains("adapter =") || resume.contains("setOnClickListener"))
    }
}
