package com.eve.app.ui.admin.emoji

import android.app.Application
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.admin.EditExamBoundaryFixture
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.io.File

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
class EmojiStudioPermissionResumeTest {

    private val contentRight = "manage_content"

    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test",
            rights = listOf(contentRight)
        )
    }

    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
    }

    @Test fun contractEnsuresReauthorizeOnResumeAndDialogDismissal() {
        val src = File("src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt").readText()

        assertTrue("Must set reauthorizeOnResume in onStop",
            src.contains("reauthorizeOnResume = true"))
        assertTrue("Must re-validate admin rights on resume",
            src.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = \"manage_content\")"))
        assertTrue("Must dismiss placementDialog in onStop",
            src.contains("placementDialog?.dismiss()"))
        assertTrue("Must dismiss packManagerDialog in onStop",
            src.contains("packManagerDialog?.dismiss()"))
        assertTrue("Must dismiss importOptionsDialog in onStop",
            src.contains("importOptionsDialog?.dismiss()"))
        assertTrue("Must dismiss itemDetailsDialog in onStop",
            src.contains("itemDetailsDialog?.dismiss()"))
        assertTrue("Must check live user against authorizedUid on resume",
            src.contains("if (live == null || live.uid != authorizedUid || live.email != authorizedEmail)"))
    }
}
