package com.eve.app.ui.admin

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.EditText
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.eve.app.util.Constants
import kotlin.coroutines.resume
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
 EditExamBoundaryFixture.ExamBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class ExamMetadataSaveTest {
    private val exam=Exam(id="exam",examName="Confirmed",questionCount=100,negativeMarkingText="1/3",negativeMarkingValue=1.0/3,
        parentExamId="parent",publishMode="live",cutoffs=mapOf("General" to 70.0))
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_exams"))
        EditExamBoundaryFixture.exams=listOf(exam)
        EditExamBoundaryFixture.questions=listOf(Question(id="question",examId="exam"))
    }
    @After fun after() { ProfileEditorAuthFixture.reset(); EditExamBoundaryFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun open(state:android.os.Bundle?=null,id:String="exam"):ActivityController<EditExamActivity> {
        val intent=Intent(RuntimeEnvironment.getApplication(),EditExamActivity::class.java)
            .putExtra(Constants.EXTRA_EXAM_ID,id).putExtra(Constants.EXTRA_EXAM_NAME,"Intent title")
        val c=Robolectric.buildActivity(EditExamActivity::class.java,intent)
        c.get().setTheme(R.style.Theme_Eve)
        if(state==null)c.setup()else c.setup(state)
        c.visible(); idle(); return c
    }
    private fun close(c:ActivityController<EditExamActivity>) { if(!c.get().isDestroyed)c.pause().stop().destroy(); idle() }
    private fun exercise(block:(ActivityController<EditExamActivity>)->Unit) {
        val c=open(); try { block(c) } finally { close(c) }
    }
    @Test fun pendingMetadataCannotSubmitFabricatedDefaults() {
        EditExamBoundaryFixture.holdExamRead=true
        exercise { c ->
            c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
            assertEquals(0,EditExamBoundaryFixture.examWrites)
            assertFalse(c.get().findViewById<View>(R.id.etExamName).isEnabled)
        }
    }
    @Test fun missingExamCannotSubmitFabricatedDefaults() {
        EditExamBoundaryFixture.exams=emptyList<Exam>()
        exercise { c ->
            c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
            assertEquals(0,EditExamBoundaryFixture.examWrites)
        }
    }
    @Test fun editingMetadataPreservesPlannedCountAndUnshownSettings()=exercise { c ->
        c.get().findViewById<EditText>(R.id.etExamName).setText("Renamed")
        c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
        val sent=requireNotNull(EditExamBoundaryFixture.lastUpdatedExam)
        assertEquals(100,sent.questionCount); assertEquals("1/3",sent.negativeMarkingText)
        assertEquals("parent",sent.parentExamId); assertEquals("live",sent.publishMode)
        assertEquals("Renamed",sent.examName)
    }
    @Test fun pendingSaveIsSingleAndFailureRetainsDraftForRetry()=exercise { c ->
        val button=c.get().findViewById<View>(R.id.btnSaveExamDetails)
        c.get().findViewById<EditText>(R.id.etExamName).setText("Draft name")
        button.performClick(); idle(); button.performClick(); idle()
        assertEquals(1,EditExamBoundaryFixture.examWrites)
        assertFalse(button.isEnabled)
        requireNotNull(EditExamBoundaryFixture.pendingExamWrite).resumeWith(Result.failure(Exception("Denied"))); idle()
        assertTrue(button.isEnabled)
        assertEquals("Draft name",c.get().findViewById<EditText>(R.id.etExamName).text.toString())
        button.performClick(); idle(); assertEquals(2,EditExamBoundaryFixture.examWrites)
        requireNotNull(EditExamBoundaryFixture.pendingExamWrite).resume(Unit); idle()
        assertTrue(button.isEnabled)
    }
    @Test fun staleSaveButtonCannotWriteAfterAccountReplacement()=exercise { c ->
        ProfileEditorAuthFixture.setOwner("other"); idle()
        assertTrue(c.get().isFinishing)
        c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.examWrites)
    }
    @Test fun existingCustomCategoryDoesNotSilentlyBecomeAPreset() {
        EditExamBoundaryFixture.exams=listOf(exam.copy(category="Regional Custom"))
        exercise { c ->
            c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
            assertEquals("Regional Custom",EditExamBoundaryFixture.lastUpdatedExam.category)
        }
    }
    @Test fun invalidCutoffsAndMinutesDoNotSendAMutation()=exercise { c ->
        val cutoff=c.get().findViewById<EditText>(R.id.etCutoffGeneral)
        for(value in listOf("-1","NaN","Infinity","invalid")) {
            cutoff.setText(value); c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
            assertEquals(0,EditExamBoundaryFixture.examWrites)
        }
        cutoff.setText("70")
        c.get().findViewById<EditText>(R.id.etExamMinutes).setText("601")
        c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.examWrites)
    }
    private fun savedDraft():android.os.Bundle {
        val old=open(); val state=android.os.Bundle()
        try {
            old.get().findViewById<EditText>(R.id.etExamName).setText("Restored draft")
            old.get().findViewById<EditText>(R.id.etExamMinutes).setText("55")
            old.get().findViewById<EditText>(R.id.etCutoffGeneral).setText("75")
            old.saveInstanceState(state)
        } finally { close(old) }
        return state
    }
    @Test fun sameOwnerDraftSurvivesLateFreshMetadataWithoutBeingOverwritten() {
        val state=savedDraft(); EditExamBoundaryFixture.holdExamRead=true
        val c=open(state)
        try {
            assertFalse(c.get().findViewById<View>(R.id.btnSaveExamDetails).isEnabled)
            assertEquals("Restored draft",c.get().findViewById<EditText>(R.id.etExamName).text.toString())
            requireNotNull(EditExamBoundaryFixture.pendingExamRead).resume(listOf(exam.copy(examName="Server now"))); idle()
            assertEquals("Restored draft",c.get().findViewById<EditText>(R.id.etExamName).text.toString())
            assertTrue(c.get().findViewById<View>(R.id.btnSaveExamDetails).isEnabled)
            c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
            val sent=requireNotNull(EditExamBoundaryFixture.lastUpdatedExam)
            assertEquals("Restored draft",sent.examName); assertEquals(55,sent.timeLimitMinutes)
            assertEquals(75.0,sent.cutoffs["General"]!!,0.0)
        } finally { close(c) }
    }
    @Test fun anotherAccountCannotRestoreMetadataDraftOrIssueFreshReads() {
        val state=savedDraft(); ProfileEditorAuthFixture.setOwner("replacement"); EditExamBoundaryFixture.reset()
        val c=open(state)
        try { assertTrue(c.get().isFinishing); assertEquals(0,EditExamBoundaryFixture.freshReads); assertEquals(0,EditExamBoundaryFixture.dataReads) }
        finally { close(c) }
    }
    @Test fun anotherExamCannotRestoreThePreviousExamDraft() {
        val state=savedDraft(); EditExamBoundaryFixture.reset()
        val c=open(state,"other")
        try { assertTrue(c.get().isFinishing); assertEquals(0,EditExamBoundaryFixture.freshReads); assertEquals(0,EditExamBoundaryFixture.dataReads) }
        finally { close(c) }
    }
    @Test fun unverifiableLegacyRestorationFailsClosed() {
        val c=open(android.os.Bundle())
        try { assertTrue(c.get().isFinishing); assertEquals(0,EditExamBoundaryFixture.dataReads) }
        finally { close(c) }
    }
    @Test fun destroyedHostDoesNotReenableAStaleSaveButtonOnLateReply()=exercise { c ->
        val button=c.get().findViewById<View>(R.id.btnSaveExamDetails)
        button.performClick(); idle()
        val reply=requireNotNull(EditExamBoundaryFixture.pendingExamWrite)
        close(c); reply.resume(Unit); idle()
        assertFalse(button.isEnabled); assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
    }

    private fun replaceOwnerBeforeListenerDelivery() {
        val queued=ProfileEditorAuthFixture.listeners.toList()
        ProfileEditorAuthFixture.listeners.clear()
        ProfileEditorAuthFixture.setOwner("replacement")
        ProfileEditorAuthFixture.listeners.addAll(queued)
    }
    @Test fun saveRechecksCurrentUidBeforeQueuedAuthListenerFinishesTheHost()=exercise { c ->
        replaceOwnerBeforeListenerDelivery()
        assertFalse("Listener delivery is intentionally still queued",c.get().isFinishing)
        c.get().findViewById<View>(R.id.btnSaveExamDetails).performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.examWrites)
    }
    @Test fun metadataReplyCannotEnableEditingForReplacedUidBeforeListenerDelivery() {
        EditExamBoundaryFixture.holdExamRead=true
        exercise { c ->
            replaceOwnerBeforeListenerDelivery()
            requireNotNull(EditExamBoundaryFixture.pendingExamRead).resume(listOf(exam)); idle()
            assertFalse(c.get().findViewById<View>(R.id.btnSaveExamDetails).isEnabled)
            assertEquals(0,EditExamBoundaryFixture.questionReads)
        }
    }

    @Test fun retainedDeleteConfirmationRejectsReplacedUidBeforeListenerDelivery() {
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_exams","manage_questions"))
        exercise { c ->
            val rv=c.get().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvQuestions)
            val adapter=rv.adapter as QuestionManageAdapter
            val holder=adapter.onCreateViewHolder(rv,0); adapter.onBindViewHolder(holder,0)
            holder.itemView.findViewById<View>(R.id.btnDelete).performClick(); idle()
            val dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
            replaceOwnerBeforeListenerDelivery()
            dialog.getActionButton(android.content.DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
            assertEquals(0,EditExamBoundaryFixture.writes)
            dialog.dismiss()
        }
    }

    @Test fun lateQuestionSaveCannotRebindDataForAReplacedUid() {
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_exams","manage_questions"))
        EditExamBoundaryFixture.questions=listOf(Question(id="question",examId="exam",questionText="Original Q",optionA="A",optionB="B",optionC="C",optionD="D",correctAnswer="A"))
        exercise { c ->
            val rv=c.get().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvQuestions)
            val adapter=rv.adapter as QuestionManageAdapter
            val old=adapter.onCreateViewHolder(rv,0); adapter.onBindViewHolder(old,0)
            old.itemView.findViewById<View>(R.id.btnEdit).performClick(); idle()
            val dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
            dialog.findViewById<EditText>(R.id.etQuestionText).setText("Edited Q")
            dialog.getActionButton(android.content.DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
            assertEquals(1,EditExamBoundaryFixture.writes)
            replaceOwnerBeforeListenerDelivery()
            requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
            val current=adapter.onCreateViewHolder(rv,0); adapter.onBindViewHolder(current,0)
            assertEquals("Q1. Original Q",current.itemView.findViewById<android.widget.TextView>(R.id.tvQuestionText).text.toString())
            dialog.dismiss()
        }
    }

}
