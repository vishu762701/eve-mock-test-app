package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.remote.AdminMeData
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** Real standalone Activity, protected row, native confirmation and ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
 EditExamBoundaryFixture.ExamBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class ManageQuestionDeletionTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_questions"))
        EditExamBoundaryFixture.exams=listOf(Exam(id="exam",examName="Exam"))
        EditExamBoundaryFixture.questions=listOf(Question(id="question",examId="exam",questionText="Question",optionA="A",optionB="B",optionC="C",optionD="D",correctAnswer="B"))
    }
    @After fun after() { ProfileEditorAuthFixture.reset(); EditExamBoundaryFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun exercise(block:(ManageQuestionsActivity,View)->Unit) {
        val c=Robolectric.buildActivity(ManageQuestionsActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve); c.setup().visible(); idle()
        try {
            val rv=c.get().findViewById<RecyclerView>(R.id.rvQuestions)
            @Suppress("UNCHECKED_CAST") val adapter=rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
            assertEquals(1,adapter.itemCount)
            val holder=adapter.createViewHolder(rv,0); adapter.bindViewHolder(holder,0)
            block(c.get(),holder.itemView)
        } finally { ShadowDialog.getLatestDialog()?.dismiss(); c.pause().stop().destroy(); idle() }
    }
    private fun confirm(row:View):View {
        row.findViewById<View>(R.id.btnDelete).performClick(); idle()
        val dialog=requireNotNull(ShadowDialog.getLatestDialog()) as TelegramNativeAlertDialog
        assertTrue(dialog.isShowing)
        return requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))
    }
    private fun message(a:ManageQuestionsActivity)=a.findViewById<TextView>(R.id.tvBulletinMessage)?.text?.toString()
    @Test fun pendingServerReplyMustNotBeAnnouncedAsDeleted()=exercise { activity,row ->
        confirm(row).performClick(); idle()
        assertEquals(1,EditExamBoundaryFixture.writes)
        assertNull("No success before server confirmation",message(activity))
    }
    @Test fun deniedReplyShowsFailureInsteadOfFalseSuccess()=exercise { activity,row ->
        confirm(row).performClick(); idle()
        requireNotNull(EditExamBoundaryFixture.pendingWrite).resumeWithException(IllegalStateException("Permission denied")); idle()
        assertEquals("Permission denied",message(activity))
    }
    @Test fun secondRowTapWhilePendingCannotIssueAnotherDeletion()=exercise { _,row ->
        confirm(row).performClick(); idle()
        row.findViewById<View>(R.id.btnDelete).performClick(); idle()
        val dialog=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        if(dialog.isShowing)dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
        idle(); assertEquals(1,EditExamBoundaryFixture.writes)
    }
    @Test fun confirmedReplyProducesOneSuccess()=exercise { activity,row ->
        confirm(row).performClick(); idle()
        EditExamBoundaryFixture.questions=emptyList<Question>()
        requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
        assertEquals("Question deleted",message(activity))
        assertEquals(1,EditExamBoundaryFixture.writes)
    }
    @Test fun cancelKeepsQuestionAndDoesNotWrite()=exercise { activity,row ->
        confirm(row)
        val dialog=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)?.performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.writes); assertNull(message(activity))
    }
    @Test fun queuedIdentityChangeCannotUseRetainedConfirmation()=exercise { _,row ->
        val delete=confirm(row)
        ProfileEditorAuthFixture.listeners.clear() // Model SDK listener dispatch arriving later.
        ProfileEditorAuthFixture.setOwner("owner-b")
        delete.performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.writes)
    }
    @Test fun lateCompletionCannotNotifyReplacementIdentity()=exercise { activity,row ->
        confirm(row).performClick(); idle()
        ProfileEditorAuthFixture.listeners.clear()
        ProfileEditorAuthFixture.setOwner("owner-b")
        requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
        assertNull(message(activity)); assertEquals(1,EditExamBoundaryFixture.writes)
    }
    @Test fun staleBoundRowCannotDeleteFromPreviousExam()=exercise { activity,row ->
        val vm=androidx.lifecycle.ViewModelProvider(activity)[AdminViewModel::class.java]
        EditExamBoundaryFixture.questions=emptyList<Question>()
        vm.loadQuestions("different-exam"); idle()
        row.findViewById<View>(R.id.btnDelete).performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.writes)
        assertNull(ShadowDialog.getLatestDialog())
    }
    @Test @Config(qualifiers="hi-rIN-ldrtl-night")
    fun hindiNightConfirmationUsesExistingDestructiveControls() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f)
        exercise { activity,row ->
        assertEquals(1.5f,activity.resources.configuration.fontScale)
        val delete=confirm(row)
        assertEquals(activity.getString(R.string.question_delete_action),(delete as TextView).text.toString())
        // TextView single-line mode uses a line minimum, so measure the real button bounds.
        val window=requireNotNull(ShadowDialog.getLatestDialog()?.window)
        val decor=window.decorView
        val metrics=activity.resources.displayMetrics
        decor.measure(View.MeasureSpec.makeMeasureSpec(window.attributes.width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels,View.MeasureSpec.AT_MOST))
        decor.layout(0,0,decor.measuredWidth,decor.measuredHeight)
        assertTrue("Actual destructive button height=${delete.height}",delete.height>=48*metrics.density)
        delete.performClick(); idle()
        requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
        assertEquals(activity.getString(R.string.question_deleted),message(activity))
        }
    }

    @Test fun finishingHostCannotUseRetainedConfirmation()=exercise { activity,row ->
        val delete=confirm(row); activity.finish(); delete.performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.writes)
    }
    @Test fun roleWithoutQuestionRightsCannotLoadProtectedRows() {
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_exams"))
        val c=Robolectric.buildActivity(ManageQuestionsActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve); c.setup().visible(); idle()
        try {
            assertTrue(c.get().isFinishing)
            assertEquals(0,EditExamBoundaryFixture.dataReads)
            assertEquals(0,EditExamBoundaryFixture.writes)
        } finally {c.pause().stop().destroy();idle()}
    }

}
