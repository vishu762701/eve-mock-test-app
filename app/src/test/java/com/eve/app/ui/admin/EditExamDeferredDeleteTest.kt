package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.content.Intent
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
import com.eve.app.util.Constants
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowDialog
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk=[35],application=Application::class,
    shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
        EditExamBoundaryFixture.ExamBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class EditExamDeferredDeleteTest {
    @Before fun before() {
        // Explicit virtual vsync prevents idle() from advancing through the Undo deadline.
        org.robolectric.shadows.ShadowChoreographer.setPaused(true)
        ProfileEditorAuthFixture.reset();ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset();rights("manage_exams","manage_questions")
        EditExamBoundaryFixture.exams=listOf(Exam(id="exam",examName="Exam"))
        EditExamBoundaryFixture.questions=listOf(Question(id="question",examId="exam",questionText="Question",
            optionA="A",optionB="B",optionC="C",optionD="D",correctAnswer="B"))
    }
    @After fun after() {ProfileEditorAuthFixture.reset();EditExamBoundaryFixture.reset()}
    private fun rights(vararg values:String) {EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=values.toList())}
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun advance(ms:Long) {shadowOf(Looper.getMainLooper()).idleFor(ms,TimeUnit.MILLISECONDS)}
    private fun exercise(block:(EditExamActivity,org.robolectric.android.controller.ActivityController<EditExamActivity>)->Unit) {
        val intent=Intent(RuntimeEnvironment.getApplication(),EditExamActivity::class.java)
            .putExtra(Constants.EXTRA_EXAM_ID,"exam").putExtra(Constants.EXTRA_EXAM_NAME,"Exam")
        val c=Robolectric.buildActivity(EditExamActivity::class.java,intent)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible();idle()
        try{block(c.get(),c)}finally{
            ShadowDialog.getLatestDialog()?.dismiss()
            if(c.get().lifecycle.currentState!=androidx.lifecycle.Lifecycle.State.DESTROYED)c.pause().stop().destroy()
            advance(6000)
        }
    }
    private fun rows(a:EditExamActivity)=a.findViewById<RecyclerView>(R.id.rvQuestions)
    private fun delete(a:EditExamActivity):View {
        val rv=rows(a);val adapter=rv.adapter as QuestionManageAdapter
        val row=adapter.onCreateViewHolder(rv,0);adapter.onBindViewHolder(row,0)
        row.itemView.findViewById<View>(R.id.btnDelete).performClick();idle()
        val dialog=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        assertTrue(dialog.isShowing)
        return requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).also {
            it.performClick();idle()
        }
    }
    private fun message(a:EditExamActivity)=a.findViewById<TextView>(R.id.tvBulletinMessage)?.text?.toString().orEmpty()
    @Test fun noDeleteBeforeFiveSecondsAndSuccessRequiresWorkerAcknowledgment()=exercise {a,_->
        delete(a);assertEquals(0,EditExamBoundaryFixture.writes)
        assertEquals(0,rows(a).adapter!!.itemCount)
        advance(4999);assertEquals(0,EditExamBoundaryFixture.writes)
        advance(1);assertEquals(1,EditExamBoundaryFixture.writes)
        assertFalse(message(a)==a.getString(R.string.question_deleted))
        EditExamBoundaryFixture.questions=emptyList<Question>()
        EditExamBoundaryFixture.pendingWrite.resume(Unit);idle()
        assertEquals(a.getString(R.string.question_deleted),message(a))
    }
    @Test fun undoRestoresVisibleRowWithoutAnyDeleteOrUpdateRequest()=exercise {a,_->
        delete(a)
        a.findViewById<View>(R.id.btnUndoAction).performClick();idle();advance(6000)
        assertEquals(0,EditExamBoundaryFixture.writes)
        assertEquals(1,rows(a).adapter!!.itemCount)
    }
    @Test fun repeatedRetainedConfirmationCannotQueueTwoDeletes()=exercise {a,_->
        val button=delete(a);button.performClick();idle();advance(5000)
        assertEquals(1,EditExamBoundaryFixture.writes)
    }
    @Test fun revokedPermissionDuringUndoWindowDoesNotDispatchDelete()=exercise {a,_->
        delete(a);rights("manage_exams");advance(5000)
        assertEquals(0,EditExamBoundaryFixture.writes);assertEquals(1,rows(a).adapter!!.itemCount)
    }
    @Test fun switchedAccountCannotCommitPendingDeletion()=exercise {a,_->
        delete(a);ProfileEditorAuthFixture.listeners.clear();ProfileEditorAuthFixture.setOwner("owner-b")
        advance(5000);assertEquals(0,EditExamBoundaryFixture.writes)
    }
    @Test fun stoppingCancelsUncommittedDeletionAndRestoresTheRow()=exercise {a,c->
        delete(a);c.pause().stop();advance(6000)
        assertEquals(0,EditExamBoundaryFixture.writes)
        c.start().resume();idle();assertEquals(1,rows(a).adapter!!.itemCount)
    }
    @Test fun suspendedFreshPermissionCannotCommitAcrossStopAndReturn()=exercise {a,c->
        delete(a);EditExamBoundaryFixture.holdMembership=true;advance(5000)
        assertEquals(0,EditExamBoundaryFixture.writes)
        assertNotNull(EditExamBoundaryFixture.pendingMembership)
        c.pause().stop();c.start().resume();idle()
        EditExamBoundaryFixture.holdMembership=false
        EditExamBoundaryFixture.pendingMembership.resume(true);idle()
        assertEquals(0,EditExamBoundaryFixture.writes)
    }
    @Test fun deniedWorkerAcknowledgmentRestoresRowAndDoesNotClaimDeletion()=exercise {a,_->
        delete(a);advance(5000)
        EditExamBoundaryFixture.pendingWrite.resumeWithException(IllegalStateException("Worker denied"));idle()
        assertEquals(1,rows(a).adapter!!.itemCount)
        assertFalse(message(a)==a.getString(R.string.question_deleted))
        assertTrue(message(a).contains("Worker denied"))
    }
    @Test fun recreatingCancelsTheUncommittedWindow()=exercise {a,c->
        delete(a)
        val state=android.os.Bundle()
        c.pause().saveInstanceState(state).stop().destroy()
        val restored=Robolectric.buildActivity(EditExamActivity::class.java,a.intent)
        restored.get().setTheme(R.style.Theme_Eve)
        restored.setup(state).visible();idle()
        try {
            advance(6000)
            assertFalse(restored.get().isFinishing)
            assertEquals(0,EditExamBoundaryFixture.writes)
            assertEquals(1,rows(restored.get()).adapter!!.itemCount)
        } finally {restored.pause().stop().destroy()}
    }
    @Test fun suspendedRightsCannotDispatchAfterOwnerChanges()=exercise {a,_->
        delete(a);EditExamBoundaryFixture.holdPermissions=true;advance(5000)
        assertNotNull(EditExamBoundaryFixture.pendingPermissions)
        ProfileEditorAuthFixture.listeners.clear();ProfileEditorAuthFixture.setOwner("owner-b")
        EditExamBoundaryFixture.pendingPermissions.resume(EditExamBoundaryFixture.permissions);idle()
        assertEquals(0,EditExamBoundaryFixture.writes)
    }
    @Test fun lateWorkerSuccessAfterStopAndReturnOnlyRevalidatesRows()=exercise {a,c->
        delete(a);advance(5000);assertEquals(1,EditExamBoundaryFixture.writes)
        c.pause().stop();c.start().resume();idle()
        EditExamBoundaryFixture.questions=emptyList<Question>()
        EditExamBoundaryFixture.pendingWrite.resume(Unit);idle()
        assertFalse(message(a)==a.getString(R.string.question_deleted))
        assertEquals(0,rows(a).adapter!!.itemCount)
        advance(6000);assertEquals(1,EditExamBoundaryFixture.writes)
    }
}
