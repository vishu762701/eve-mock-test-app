package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import com.eve.app.R
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.DynamicAdminItem
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.io.File

/** AdminsAndRights and AdminRightsEdit Activities re-validate fresh server rights on resume without duplicating UI wiring. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35], application = Application::class,
    shadows = [
        ProfileEditorAuthFixture.ShadowAuth::class,
        EditExamBoundaryFixture.AdminBoundary::class
    ],
    instrumentedPackages = ["com.eve.app.data.repository"]
)
class AdminRightsPermissionResumeTest {

    private val adminRight = "manage_admins_rights"

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf(adminRight)
        )
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun openList() = Robolectric.buildActivity(AdminsAndRightsActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    private fun openEdit() = Robolectric.buildActivity(
        AdminRightsEditActivity::class.java,
        AdminRightsEditActivity.createIntent(
            RuntimeEnvironment.getApplication(),
            DynamicAdminItem(email = "target@example.test", isOwner = false, rights = emptyList())
        )
    ).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    @Test fun returningListActivityRechecksManageAdminsRightsPermissionViaFreshServerRead() {
        val c = openList()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("List activity must finish when manage_admins_rights right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningListActivityWithRetainedRightsSurvives() {
        val c = openList()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            c.restart().resume().visible()
            idle()
            assertFalse("List activity must remain active when rights are intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningEditActivityRechecksManageAdminsRightsPermissionViaFreshServerRead() {
        val c = openEdit()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("Edit activity must finish when manage_admins_rights right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningEditActivityWithRetainedRightsSurvives() {
        val c = openEdit()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            c.restart().resume().visible()
            idle()
            assertFalse("Edit activity must remain active when rights are intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun contractsEnsureNoDuplicateAdminControlsWiringOnResume() {
        val listSource = File("src/main/java/com/eve/app/ui/admin/AdminsAndRightsActivity.kt").readText()
        val listStop = listSource.substringAfter("override fun onStop()").substringBefore("override fun onResume()")
        assertTrue("List onStop sets reauthorizeOnResume flag", listStop.contains("reauthorizeOnResume = true"))
        val listResume = listSource.substringAfter("override fun onResume()").substringBefore("private fun setupUi()")
        assertTrue("List onResume consumes reauthorizeOnResume", listResume.contains("reauthorizeOnResume = false"))
        assertTrue("List onResume guards identity change", listResume.contains("live.uid != authorizedUid || live.email != authorizedEmail"))
        assertTrue("List onResume re-checks AdminAccessGate", listResume.contains("AdminAccessGate.requireAdmin"))
        assertFalse("List onResume must not re-run setupUi", listResume.contains("setupUi()"))

        val editSource = File("src/main/java/com/eve/app/ui/admin/AdminRightsEditActivity.kt").readText()
        val editStop = editSource.substringAfter("override fun onStop()").substringBefore("override fun onStart()")
        assertTrue("Edit onStop sets reauthorizeOnResume flag", editStop.contains("reauthorizeOnResume = true"))
        val editResume = editSource.substringAfter("override fun onResume()").substringBefore("private var targetEmail")
        assertTrue("Edit onResume consumes reauthorizeOnResume", editResume.contains("reauthorizeOnResume = false"))
        assertTrue("Edit onResume guards identity change", editResume.contains("live.uid != activeUid || live.email != activeEmail"))
        assertTrue("Edit onResume re-checks AdminAccessGate", editResume.contains("AdminAccessGate.requireAdmin"))
    }
}
