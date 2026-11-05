package com.eve.app.ui.admin

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.view.View
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.eve.app.util.Constants
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

/** ManageExistingExams and EditExam Activities re-validate fresh server rights on resume without duplicating UI wiring. */
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
class EditExamPermissionResumeTest {

    private val examRight = "manage_exams"

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.exams = listOf(Exam(id = "exam-1", examName = "Sample Exam"))
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf(examRight, "manage_questions", "manage_content")
        )
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun openManageExisting() = Robolectric.buildActivity(ManageExistingExamsActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    private fun openEditExam() = Robolectric.buildActivity(
        EditExamActivity::class.java,
        Intent(RuntimeEnvironment.getApplication(), EditExamActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "exam-1")
            putExtra(Constants.EXTRA_EXAM_NAME, "Sample Exam")
        }
    ).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }

    @Test fun returningManageExistingRechecksManageExamsPermissionViaFreshServerRead() {
        val c = openManageExisting()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("ManageExistingExams must finish when manage_exams right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningManageExistingWithRetainedRightsSurvives() {
        val c = openManageExisting()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            c.restart().resume().visible()
            idle()
            assertFalse("ManageExistingExams must remain active when rights are intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningEditExamRechecksManageExamsPermissionViaFreshServerRead() {
        val c = openEditExam()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = emptyList()
            )
            c.restart().resume().visible()
            idle()
            assertTrue("EditExam must finish when manage_exams right is revoked", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun returningEditExamWithRetainedRightsDynamicallyUpdatesPermissions() {
        val c = openEditExam()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            assertEquals(View.VISIBLE, c.get().findViewById<View>(R.id.btnAddQuestion).visibility)
            c.pause().stop()
            // Revoke manage_questions but keep manage_exams
            EditExamBoundaryFixture.permissions = AdminMeData(
                email = "owner@example.test", rights = listOf(examRight)
            )
            c.restart().resume().visible()
            idle()
            assertFalse("EditExam must remain active when manage_exams right is intact", c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
            assertEquals("btnAddQuestion should be hidden when manage_questions right is revoked",
                View.GONE, c.get().findViewById<View>(R.id.btnAddQuestion).visibility)
        } finally {
            c.destroy()
            idle()
        }
    }

    @Test fun contractsEnsureNoDuplicateAdminControlsWiringOnResume() {
        val existingSource = File("src/main/java/com/eve/app/ui/admin/ManageExistingExamsActivity.kt").readText()
        val existingStop = existingSource.substringAfter("override fun onStop()").substringBefore("override fun onResume()")
        assertTrue("ManageExistingExams onStop sets reauthorizeOnResume flag", existingStop.contains("reauthorizeOnResume = true"))
        val existingResume = existingSource.substringAfter("override fun onResume()").substringBefore("private fun setupAdminControls()")
        assertTrue("ManageExistingExams onResume consumes reauthorizeOnResume", existingResume.contains("reauthorizeOnResume = false"))
        assertTrue("ManageExistingExams onResume guards identity change", existingResume.contains("live.uid != authorizedUid || live.email != authorizedEmail"))
        assertTrue("ManageExistingExams onResume re-checks AdminAccessGate", existingResume.contains("AdminAccessGate.requireAdmin"))
        assertFalse("ManageExistingExams onResume must not re-run setupAdminControls", existingResume.contains("setupAdminControls()"))

        val editSource = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        val editStop = editSource.substringAfter("override fun onStop()").substringBefore("override fun onRestoreInstanceState")
        assertTrue("EditExam onStop sets reauthorizeOnResume flag", editStop.contains("reauthorizeOnResume = true"))
        val editResume = editSource.substringAfter("override fun onResume()").substringBefore("override fun onStop()")
        assertTrue("EditExam onResume consumes reauthorizeOnResume", editResume.contains("reauthorizeOnResume = false"))
        assertTrue("EditExam onResume guards identity change", editResume.contains("live.uid != editorOwnerUid || live.email != editorOwnerEmail"))
        assertTrue("EditExam onResume re-checks AdminAccessGate", editResume.contains("AdminAccessGate.requireAdmin"))
    }
}
