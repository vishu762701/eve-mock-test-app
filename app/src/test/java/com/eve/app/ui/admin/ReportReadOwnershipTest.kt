package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import kotlin.coroutines.resume
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ActivityController

/** Actual protected Activity, native filter callbacks and report transport order. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
 QuestionReviewBoundaryFixture.ReportsBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class ReportReadOwnershipTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("moderate_feedback"))
        QuestionReviewBoundaryFixture.holdReportReads=true; QuestionReviewBoundaryFixture.reportReads.clear()
    }
    @After fun after() { QuestionReviewBoundaryFixture.holdReportReads=false; ProfileEditorAuthFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun exercise(block:(ActivityController<FlaggedQuestionsActivity>)->Unit) {
        val c=Robolectric.buildActivity(FlaggedQuestionsActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve); c.setup().visible(); idle()
        try { block(c) } finally { if (!c.get().isDestroyed) c.pause().stop().destroy(); idle() }
    }
    private fun answer(index:Int,text:String) {
        val read=QuestionReviewBoundaryFixture.reportReads[index]
        read.reply.resume(listOf(AggregatedFlaggedQuestion(text,"exam","Exam",text,1,emptyList(),emptyList(),listOf(text),1,
            reportType=if(read.technical)"technical" else "content"))); idle()
    }
    private fun text(a:FlaggedQuestionsActivity):String {
        val rv=a.findViewById<RecyclerView>(R.id.rvFlaggedQuestions)
        @Suppress("UNCHECKED_CAST") val adapter=rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        assertEquals(1,adapter.itemCount)
        val holder=adapter.createViewHolder(rv,0); adapter.bindViewHolder(holder,0)
        return holder.itemView.findViewById<TextView>(R.id.tvQuestionText).text.toString()
    }
    @Test fun authorizedCreatedActivityDefersFirstReadUntilStartedWithoutDroppingIt() {
        val c = Robolectric.buildActivity(FlaggedQuestionsActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve)
        try {
            c.create(); idle()
            assertEquals("No privileged read before STARTED", 0,
                QuestionReviewBoundaryFixture.reportReads.size)
            c.start().resume().visible(); idle()
            assertEquals("First authorized read must run exactly once", 1,
                QuestionReviewBoundaryFixture.reportReads.size)
        } finally {
            c.pause().stop().destroy(); idle()
        }
    }

    @Test fun oldTypeCannotReplaceLatestRows()=exercise { c ->
        c.get().findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        answer(1,"Technical"); answer(0,"Content")
        assertEquals("Technical",text(c.get()))
    }
    @Test fun lateOldFailureCannotHideCurrentLoading()=exercise { c ->
        c.get().findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        QuestionReviewBoundaryFixture.reportReads[0].reply.resumeWith(Result.failure(Exception("Old failed"))); idle()
        assertEquals(View.VISIBLE,c.get().findViewById<View>(R.id.progressBar).visibility)
    }
    @Test fun switchingTypeImmediatelyRemovesOldActionableRows()=exercise { c ->
        answer(0,"Content")
        c.get().findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        assertEquals(0,c.get().findViewById<RecyclerView>(R.id.rvFlaggedQuestions).adapter!!.itemCount)
    }
    @Test fun currentTypeIsSingleChoiceAndSelectedTapDoesNotReload()=exercise { c ->
        val a=c.get()
        a.findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        assertEquals(2,QuestionReviewBoundaryFixture.reportReads.size)
        a.findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        val group=a.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupReportType)
        assertEquals(listOf(R.id.chipTechnicalIssues),group.checkedChipIds)
        assertEquals(2,QuestionReviewBoundaryFixture.reportReads.size)
        a.findViewById<View>(R.id.chipContentIssues).performClick(); idle()
        assertEquals(listOf(false,true,false),QuestionReviewBoundaryFixture.reportReads.map {it.technical})
    }
    @Test fun oldBoundDismissCallbackCannotActAfterTypeChanges()=exercise { c ->
        answer(0,"Content")
        val rv=c.get().findViewById<RecyclerView>(R.id.rvFlaggedQuestions)
        @Suppress("UNCHECKED_CAST") val adapter=rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        val old=adapter.createViewHolder(rv,0); adapter.bindViewHolder(old,0)
        c.get().findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        old.itemView.findViewById<View>(R.id.btnDismissFlag).performClick(); idle()
        assertNull(org.robolectric.shadows.ShadowDialog.getLatestDialog())
    }
    @Test fun savedTypeSelectsTheFirstRecreatedRequest()=exercise { c ->
        c.get().findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        val state=android.os.Bundle(); c.saveInstanceState(state)
        QuestionReviewBoundaryFixture.reportReads.clear()
        val recreated=Robolectric.buildActivity(FlaggedQuestionsActivity::class.java)
        recreated.get().setTheme(R.style.Theme_Eve); recreated.setup(state).visible(); idle()
        try {
            assertTrue(QuestionReviewBoundaryFixture.reportReads.isNotEmpty())
            assertTrue(QuestionReviewBoundaryFixture.reportReads.all {it.technical})
            assertEquals(R.id.chipTechnicalIssues,recreated.get().findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupReportType).checkedChipId)
        } finally { recreated.pause().stop().destroy(); idle() }
    }
    @Test fun staleEmptyResultCannotReplaceCurrentRowsOrClaimSystemsOperational()=exercise { c ->
        c.get().findViewById<View>(R.id.chipTechnicalIssues).performClick(); idle()
        answer(1,"Technical")
        QuestionReviewBoundaryFixture.reportReads[0].reply.resume(emptyList<AggregatedFlaggedQuestion>()); idle()
        assertEquals("Technical",text(c.get()))
        assertEquals(View.GONE,c.get().findViewById<View>(R.id.tvEmpty).visibility)
        c.get().findViewById<View>(R.id.btnRefresh).performClick(); idle()
        QuestionReviewBoundaryFixture.reportReads[2].reply.resume(emptyList<AggregatedFlaggedQuestion>()); idle()
        assertEquals(c.get().getString(R.string.admin_reports_empty_technical),c.get().findViewById<TextView>(R.id.tvEmpty).text.toString())
    }

    @Test fun revokedModerationCannotDismissAReportAfterDialogOpens()=exercise { c ->
        answer(0, "Content")
        val rv = c.get().findViewById<RecyclerView>(R.id.rvFlaggedQuestions)
        @Suppress("UNCHECKED_CAST")
        val list = rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        val row = list.createViewHolder(rv, 0)
        list.bindViewHolder(row, 0)
        row.itemView.findViewById<View>(R.id.btnDismissFlag).performClick()
        idle()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
            as com.eve.app.designsystem.TelegramNativeAlertDialog
        val prior = QuestionReviewBoundaryFixture.dismissCalls
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test", rights = emptyList())
        dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
        idle()
        assertEquals(prior, QuestionReviewBoundaryFixture.dismissCalls)
        dialog.dismiss()
    }

    @Test fun stoppedHostClearsPrivilegedRowsAndRechecksServerRightsOnResume()=exercise { c ->
        answer(0, "Content")
        val list = c.get().findViewById<RecyclerView>(R.id.rvFlaggedQuestions)
        assertEquals(1, list.adapter!!.itemCount)
        val before = EditExamBoundaryFixture.freshReads
        c.pause().stop()
        assertEquals(0, list.adapter!!.itemCount)
        EditExamBoundaryFixture.permissions = AdminMeData(
            email = "owner@example.test", rights = emptyList())
        c.start().resume().visible()
        idle()
        assertTrue(c.get().isFinishing)
        assertEquals(before + 1, EditExamBoundaryFixture.freshReads)
    }

    @Test fun stoppedRowCannotOpenDismissalAfterLifecycleInvalidation()=exercise { c ->
        answer(0, "Content")
        val rv = c.get().findViewById<RecyclerView>(R.id.rvFlaggedQuestions)
        @Suppress("UNCHECKED_CAST")
        val list = rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        val row = list.createViewHolder(rv, 0)
        list.bindViewHolder(row, 0)
        val dismissed = QuestionReviewBoundaryFixture.dismissCalls
        c.pause().stop()
        row.itemView.findViewById<View>(R.id.btnDismissFlag).performClick()
        idle()
        assertEquals(dismissed, QuestionReviewBoundaryFixture.dismissCalls)
    }

    @Test fun destroyedHostRejectsLateTransportData()=exercise { c ->
        val adapter=c.get().findViewById<RecyclerView>(R.id.rvFlaggedQuestions).adapter!!
        c.pause().stop().destroy(); idle()
        answer(0,"Late")
        assertEquals(0,adapter.itemCount)
        assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
    }

}
