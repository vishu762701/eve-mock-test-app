package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import kotlin.coroutines.resume
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.*
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** Real review Activities, fresh gate, row callbacks and native editor fields. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
 EditExamBoundaryFixture.ExamBoundary::class,QuestionReviewBoundaryFixture.AnalyticsBoundary::class,
 QuestionReviewBoundaryFixture.ReportsBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class QuestionReviewActivityTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        val wrong=Question(id="wrong",examId="exam",questionText="Same text",optionA="WRONG A",optionB="B",optionC="C",optionD="D",correctAnswer="A")
        val target=wrong.copy(id="target",optionA="TARGET A",correctAnswer="B",topic="Original topic")
        EditExamBoundaryFixture.exams=listOf(Exam(id="exam",examName="Exam"))
        EditExamBoundaryFixture.questions=listOf(wrong,target)
        QuestionReviewBoundaryFixture.exams=listOf(ExamAnalytics(examId="exam",examName="Exam",attemptCount=5,uniqueUsers=2))
        QuestionReviewBoundaryFixture.analytics=listOf(QuestionAnalytics(id="analytics-row",questionId="target",examId="exam",questionText="Same text",attempts=5))
        QuestionReviewBoundaryFixture.reports=listOf(AggregatedFlaggedQuestion("target","exam","Exam","Same text",1,emptyList(),emptyList(),listOf("flag"),1))
        QuestionReviewBoundaryFixture.dismissSuccess=true; QuestionReviewBoundaryFixture.dismissCalls=0
    }
    @After fun after() { ProfileEditorAuthFixture.reset(); EditExamBoundaryFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun exercise(type:Class<out EveBaseActivity>,canEdit:Boolean=true,block:(EveBaseActivity,View)->Unit) {
        val entry=if(type==AdminAnalyticsActivity::class.java)"view_analytics_logs" else "moderate_feedback"
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf(entry)+if(canEdit)listOf("manage_questions")else emptyList())
        val controller=Robolectric.buildActivity(type)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible(); idle()
        try {
            val rv=controller.get().findViewById<RecyclerView>(if(type==AdminAnalyticsActivity::class.java)R.id.rvQuestions else R.id.rvFlaggedQuestions)
            @Suppress("UNCHECKED_CAST") val adapter=rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
            assertEquals(1,adapter.itemCount)
            val holder=adapter.createViewHolder(rv,0); adapter.bindViewHolder(holder,0)
            block(controller.get(),holder.itemView)
        } finally {
            ShadowDialog.getLatestDialog()?.dismiss()
            controller.pause().stop().destroy(); idle()
            assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
        }
    }
    private fun verifyTarget(type:Class<out EveBaseActivity>)=exercise(type) { _,row ->
        row.findViewById<View>(R.id.btnEditQuestion).performClick(); idle()
        val dialog=requireNotNull(ShadowDialog.getLatestDialog())
        assertEquals("TARGET A",dialog.findViewById<EditText>(R.id.etOptionA).text.toString())
    }
    @Test fun analyticsAuthoritativeIdWinsOverEarlierSameTextQuestion()=verifyTarget(AdminAnalyticsActivity::class.java)
    @Test fun flaggedAuthoritativeIdWinsOverEarlierSameTextQuestion()=verifyTarget(FlaggedQuestionsActivity::class.java)
    private fun verifyReadOnly(type:Class<out EveBaseActivity>)=exercise(type,false) { _,row ->
        val edit=row.findViewById<View>(R.id.btnEditQuestion)
        assertEquals(View.GONE,edit.visibility)
        edit.performClick(); idle()
        assertEquals(0,EditExamBoundaryFixture.questionReads)
        assertNull(ShadowDialog.getLatestDialog())
        if(type==FlaggedQuestionsActivity::class.java) assertEquals(View.VISIBLE,row.findViewById<View>(R.id.btnDismissFlag).visibility)
    }
    @Test fun analyticsViewRightDoesNotGrantQuestionEditing()=verifyReadOnly(AdminAnalyticsActivity::class.java)
    @Test fun reportModerationRightDoesNotGrantQuestionEditing()=verifyReadOnly(FlaggedQuestionsActivity::class.java)
    private fun openEditor(row:View):com.eve.app.designsystem.TelegramNativeAlertDialog {
        row.findViewById<View>(R.id.btnEditQuestion).performClick(); idle()
        return ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
    }
    @Test fun unknownKnownIdDoesNotManufactureAnEditorFromMatchingText() {
        EditExamBoundaryFixture.questions=listOf((EditExamBoundaryFixture.questions as List<*>).first())
        for(type in listOf(AdminAnalyticsActivity::class.java,FlaggedQuestionsActivity::class.java)) {
            exercise(type) { _,row ->
                row.findViewById<View>(R.id.btnEditQuestion).performClick(); idle()
                assertNull(ShadowDialog.getLatestDialog()); assertEquals(0,EditExamBoundaryFixture.writes)
            }
        }
    }
    @Test fun idlessLegacyUniqueTextStillOpensTheCorrectExistingQuestion() {
        val target=(EditExamBoundaryFixture.questions as List<*>)[1] as Question
        EditExamBoundaryFixture.questions=listOf(target)
        QuestionReviewBoundaryFixture.analytics=listOf(QuestionAnalytics(id="legacy-row",examId="exam",questionText="Same text",attempts=5))
        val report=(QuestionReviewBoundaryFixture.reports as List<*>).first() as AggregatedFlaggedQuestion
        QuestionReviewBoundaryFixture.reports=listOf(report.copy(questionId=""))
        for(type in listOf(AdminAnalyticsActivity::class.java,FlaggedQuestionsActivity::class.java)) {
            exercise(type) { _,row -> assertEquals("TARGET A",openEditor(row).findViewById<EditText>(R.id.etOptionA).text.toString()) }
        }
    }
    @Test fun invalidEditPreservesDraftAndDoesNotSendAnIncompleteQuestion()=exercise(AdminAnalyticsActivity::class.java) { _,row ->
        val dialog=openEditor(row)
        dialog.findViewById<EditText>(R.id.etOptionA).setText("")
        dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
        assertTrue(dialog.isShowing); assertEquals(0,EditExamBoundaryFixture.writes)
        assertEquals(View.VISIBLE,dialog.findViewById<View>(R.id.tvQuestionFormError).visibility)
        assertNull("View/Question rights do not authorize Exam Editor navigation",dialog.getActionButton(DialogInterface.BUTTON_NEUTRAL))
    }
    @Test fun analyticsSavePreservesTargetAndMetadataAndClosesOnlyAfterConfirmation() {
        val rows=EditExamBoundaryFixture.questions as List<*>
        val target=(rows[1] as Question).copy(isPyq=true,pyqYear=2024,pyqPaper="Paper",questionTextHi="हिन्दी प्रश्न")
        EditExamBoundaryFixture.questions=listOf(rows[0],target)
        exercise(AdminAnalyticsActivity::class.java) { _,row ->
            val dialog=openEditor(row)
            dialog.findViewById<EditText>(R.id.etQuestionText).setText("Edited text")
            val save=dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
            save.performClick(); idle()
            assertTrue(dialog.isShowing); assertFalse(save.isEnabled)
            val updated=requireNotNull(EditExamBoundaryFixture.lastUpdatedQuestion)
            assertEquals("target",updated.id); assertEquals("B",updated.correctAnswer)
            assertEquals("Edited text",updated.questionText); assertEquals("Original topic",updated.topic)
            assertTrue(updated.isPyq); assertEquals(2024,updated.pyqYear); assertEquals("हिन्दी प्रश्न",updated.questionTextHi)
            save.performClick(); assertEquals(1,EditExamBoundaryFixture.writes)
            requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
            assertFalse(dialog.isShowing); assertEquals(0,QuestionReviewBoundaryFixture.dismissCalls)
        }
    }
    @Test fun failedReportResolutionCannotCloseAsFullySuccessfulEdit()=exercise(FlaggedQuestionsActivity::class.java) { _,row ->
        QuestionReviewBoundaryFixture.dismissSuccess=false
        val dialog=openEditor(row)
        dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
        assertEquals("target",EditExamBoundaryFixture.lastUpdatedQuestion.id)
        requireNotNull(EditExamBoundaryFixture.pendingWrite).resume(Unit); idle()
        assertEquals(1,QuestionReviewBoundaryFixture.dismissCalls)
        assertTrue(dialog.isShowing)
        assertTrue(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.isEnabled)
        assertTrue(dialog.findViewById<TextView>(R.id.tvQuestionFormError).text.toString().contains("reports"))
    }

    @Test fun staleSaveCallbackCannotWriteAfterIdentityReplacement() {
        for(type in listOf(AdminAnalyticsActivity::class.java,FlaggedQuestionsActivity::class.java)) {
            ProfileEditorAuthFixture.setOwner("owner-a")
            exercise(type) { host,row ->
                val dialog=openEditor(row)
                ProfileEditorAuthFixture.setOwner("owner-b"); idle()
                assertTrue(host.isFinishing)
                dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
                assertEquals(0,EditExamBoundaryFixture.writes)
            }
        }
    }
    @Test fun missingCorrectAnswerRequiresExplicitSelectionInsteadOfAssumingA() {
        val target=(EditExamBoundaryFixture.questions as List<*>)[1] as Question
        EditExamBoundaryFixture.questions=listOf(target.copy(correctAnswer=""))
        exercise(AdminAnalyticsActivity::class.java) { _,row ->
            val dialog=openEditor(row)
            assertEquals(-1,dialog.findViewById<android.widget.RadioGroup>(R.id.rgCorrectAnswer).checkedRadioButtonId)
            dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
            assertTrue(dialog.isShowing); assertEquals(0,EditExamBoundaryFixture.writes)
            assertTrue(dialog.findViewById<TextView>(R.id.tvQuestionFormError).text.toString().contains("correct option"))
        }
    }

    @Test fun analyticsDoesNotPresentUnfetchedCrashTelemetryAsMeasuredData()=exercise(AdminAnalyticsActivity::class.java) { host,_ ->
        assertFalse(host.findViewById<TextView>(R.id.tvCrashFreePercentage).text.toString().contains("99.8"))
        assertFalse(host.findViewById<TextView>(R.id.tvTopCrashIssues).text.toString().contains("0 fatal crashes"))
        assertEquals(View.VISIBLE,host.findViewById<View>(R.id.btnOpenCrashlyticsConsole).visibility)
    }

    @Test @Config(qualifiers="hi-rIN-ldrtl-night")
    fun analyticsUnavailableTelemetryUsesHindiNightResources()=exercise(AdminAnalyticsActivity::class.java) { host,_ ->
        assertEquals(host.getString(R.string.analytics_telemetry_unavailable),host.findViewById<TextView>(R.id.tvCrashFreePercentage).text.toString())
        assertEquals(host.getString(R.string.analytics_telemetry_unavailable_detail),host.findViewById<TextView>(R.id.tvTopCrashIssues).text.toString())
        assertTrue(host.findViewById<TextView>(R.id.tvCrashFreePercentage).text.toString().contains("उपलब्ध"))
    }

}
