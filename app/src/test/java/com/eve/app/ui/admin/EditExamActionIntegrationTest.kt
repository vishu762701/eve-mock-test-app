package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.eve.app.util.Constants
import kotlin.coroutines.resume
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** Actual protected editor; synthetic server boundaries never ship in app/production. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
    shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,EditExamBoundaryFixture.ExamBoundary::class],
    instrumentedPackages=["com.eve.app.data.repository"])
class EditExamActionIntegrationTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.exams=listOf(Exam(id="exam",examName="Exam"))
        EditExamBoundaryFixture.questions=listOf(Question(id="q",examId="exam",questionText="Q",optionA="A",optionB="B",optionC="C",optionD="D",correctAnswer="A"))
    }
    @After fun after() { ProfileEditorAuthFixture.reset(); EditExamBoundaryFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun open(rights:List<String>,owner:Boolean=false):ActivityController<EditExamActivity> {
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",isOwner=owner,rights=rights)
        val intent=Intent(RuntimeEnvironment.getApplication(),EditExamActivity::class.java)
            .putExtra(Constants.EXTRA_EXAM_ID,"exam").putExtra(Constants.EXTRA_EXAM_NAME,"Exam")
        val controller=Robolectric.buildActivity(EditExamActivity::class.java,intent)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible(); idle()
        return controller
    }
    private fun close(c:ActivityController<EditExamActivity>) {
        c.pause().stop().destroy(); idle()
        assertTrue("Identity listeners must release the destroyed host",ProfileEditorAuthFixture.listeners.isEmpty())
    }
    @Test fun examOnlyRoleDoesNotExposeQuestionOrEmojiMutationControls() {
        val c=open(listOf("manage_exams"))
        try {
            assertFalse(c.get().isFinishing); assertTrue(EditExamBoundaryFixture.freshReads>0)
            for(id in listOf(R.id.btnAddQuestion,R.id.btnBulkUpload,R.id.btnPickEmoji))
                assertEquals("Control $id requires its own server right",View.GONE,c.get().findViewById<View>(id).visibility)
            @Suppress("UNCHECKED_CAST")
            val adapter=c.get().findViewById<RecyclerView>(R.id.rvQuestions).adapter as QuestionManageAdapter
            val parent=c.get().findViewById<RecyclerView>(R.id.rvQuestions)
            val holder=adapter.onCreateViewHolder(parent,0); adapter.onBindViewHolder(holder,0)
            for(id in listOf(R.id.btnEdit,R.id.btnDelete)) assertEquals(View.GONE,holder.itemView.findViewById<View>(id).visibility)
            assertFalse(holder.itemView.findViewById<TextView>(R.id.tvCorrect).text.toString().contains("Correct:"))
            holder.itemView.findViewById<View>(R.id.btnEdit).performClick()
            holder.itemView.findViewById<View>(R.id.btnDelete).performClick()
            assertNull(ShadowDialog.getLatestDialog())
        } finally { close(c) }
    }
    @Test fun questionRightPreservesQuestionActionsButDoesNotGrantEmojiStudio() {
        val c=open(listOf("manage_exams","manage_questions"))
        try {
            assertEquals(View.VISIBLE,c.get().findViewById<View>(R.id.btnAddQuestion).visibility)
            assertEquals(View.VISIBLE,c.get().findViewById<View>(R.id.btnBulkUpload).visibility)
            assertEquals(View.GONE,c.get().findViewById<View>(R.id.btnPickEmoji).visibility)
        } finally { close(c) }
    }
    @Test fun ownerKeepsAllExistingActionsAndDeniedEntryLoadsNothing() {
        val owner=open(emptyList(),true)
        try { for(id in listOf(R.id.btnAddQuestion,R.id.btnBulkUpload,R.id.btnPickEmoji)) assertEquals(View.VISIBLE,owner.get().findViewById<View>(id).visibility) }
        finally { close(owner) }
        EditExamBoundaryFixture.reset()
        val denied=open(listOf("manage_questions"))
        try { assertTrue(denied.get().isFinishing); assertEquals(0,EditExamBoundaryFixture.dataReads) }
        finally { close(denied) }
    }
    @Test fun accountReplacementClosesThePrivilegedEditor() {
        val c=open(listOf("manage_exams","manage_questions"))
        try {
            assertFalse(c.get().isFinishing)
            val parent=c.get().findViewById<RecyclerView>(R.id.rvQuestions)
            val adapter=parent.adapter as QuestionManageAdapter
            val holder=adapter.onCreateViewHolder(parent,0); adapter.onBindViewHolder(holder,0)
            ProfileEditorAuthFixture.setOwner("owner-b"); idle()
            assertTrue(c.get().isFinishing)
            assertEquals(View.INVISIBLE,c.get().findViewById<ViewGroup>(android.R.id.content).getChildAt(0).visibility)
            holder.itemView.findViewById<View>(R.id.btnEdit).performClick()
            holder.itemView.findViewById<View>(R.id.btnDelete).performClick()
            assertNull(ShadowDialog.getLatestDialog())
            assertEquals(0,EditExamBoundaryFixture.writes)
        }
        finally { close(c) }
    }
    @Test fun actualAddButtonKeepsInvalidDraftInsteadOfClosing() {
        val c=open(listOf("manage_exams","manage_questions"))
        try {
            c.get().findViewById<View>(R.id.btnAddQuestion).performClick(); idle()
            val dialog=ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
            dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
            assertTrue("Actual EditExam Add validation must retain the dialog",dialog.isShowing)
            assertEquals(0,EditExamBoundaryFixture.writes)
            dialog.dismiss()
        } finally { close(c) }
    }
    private fun beginValidAdd(c:ActivityController<EditExamActivity>):com.eve.app.designsystem.TelegramNativeAlertDialog {
        c.get().findViewById<View>(R.id.btnAddQuestion).performClick(); idle()
        val dialog=ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
        dialog.findViewById<EditText>(R.id.etQuestionText).setText("Draft Q")
        for(id in listOf(R.id.etOptionA,R.id.etOptionB,R.id.etOptionC,R.id.etOptionD)) dialog.findViewById<EditText>(id).setText("Option")
        dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
        return dialog
    }
    @Test fun actualAddPendingDenialRetryAndSuccessKeepDraftAndRefreshOnlyOnConfirmation() {
        val c=open(listOf("manage_exams","manage_questions"))
        try {
            val dialog=beginValidAdd(c)
            val add=dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
            assertTrue(dialog.isShowing); assertFalse(add.isEnabled)
            assertEquals(1,EditExamBoundaryFixture.writes)
            add.performClick(); idle(); assertEquals(1,EditExamBoundaryFixture.writes)
            requireNotNull(EditExamBoundaryFixture.pendingWrite).resumeWith(Result.failure<Unit>(IllegalStateException("Permission revoked"))); idle()
            assertTrue(dialog.isShowing); assertTrue(add.isEnabled)
            assertEquals("Draft Q",dialog.findViewById<EditText>(R.id.etQuestionText).text.toString())
            val error=dialog.findViewById<TextView>(R.id.tvQuestionFormError)
            assertEquals(View.VISIBLE,error.visibility); assertTrue(error.text.isNotBlank())
            add.performClick(); idle(); assertEquals(2,EditExamBoundaryFixture.writes)
            val original=(EditExamBoundaryFixture.questions as List<*>).first() as Question
            EditExamBoundaryFixture.questions=listOf(original,original.copy(id="added",questionText="Draft Q"))
            requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
            assertFalse(dialog.isShowing)
            assertEquals("Questions (2)",c.get().findViewById<TextView>(R.id.tvQuestionsHeader).text.toString())
        } finally { close(c) }
    }
    @Test fun pendingAddWindowClosesOnDestroyAndLateResultDoesNotReloadOldHost() {
        val c=open(listOf("manage_exams","manage_questions"))
        val dialog=beginValidAdd(c)
        val readCount=EditExamBoundaryFixture.dataReads
        val pending=requireNotNull(EditExamBoundaryFixture.pendingWrite)
        close(c)
        assertFalse(dialog.isShowing)
        pending.resume(Unit); idle()
        assertFalse(dialog.isShowing)
        assertEquals(readCount,EditExamBoundaryFixture.dataReads)
    }

}
