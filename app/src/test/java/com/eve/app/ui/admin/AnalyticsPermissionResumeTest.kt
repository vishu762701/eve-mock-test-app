package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.Question
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import kotlin.coroutines.resume
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

/** Actual Analytics Activity and canonical question editor with controlled auth/network boundaries. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], application=Application::class,
    shadows=[ProfileEditorAuthFixture.ShadowAuth::class,
        EditExamBoundaryFixture.AdminBoundary::class,
        EditExamBoundaryFixture.ExamBoundary::class,
        QuestionReviewBoundaryFixture.AnalyticsBoundary::class],
    instrumentedPackages=["com.eve.app.data.repository"])
class AnalyticsPermissionResumeTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset()
        ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions = AdminMeData(
            email="owner@example.test",
            rights=listOf("view_analytics_logs", "manage_questions"))
        EditExamBoundaryFixture.exams = listOf(Exam(id="exam", examName="Exam"))
        EditExamBoundaryFixture.questions = listOf(
            Question(id="question-1", examId="exam", questionText="Question",
                optionA="A1", optionB="B1", optionC="C1", optionD="D1", correctAnswer="B"))
        QuestionReviewBoundaryFixture.exams = listOf(
            ExamAnalytics(examId="exam", examName="Exam", attemptCount=4, uniqueUsers=1))
        QuestionReviewBoundaryFixture.analytics = listOf(
            QuestionAnalytics(id="analytics-row", questionId="question-1",
                examId="exam", questionText="Question", attempts=4))
        QuestionReviewBoundaryFixture.analyticsDays.clear()
    }
    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset()
        EditExamBoundaryFixture.reset()
    }
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun open() = Robolectric.buildActivity(AdminAnalyticsActivity::class.java).apply {
        get().setTheme(R.style.Theme_Eve)
        setup().visible()
        idle()
    }
    private fun editorRow(activity: AdminAnalyticsActivity): View {
        val rv = activity.findViewById<RecyclerView>(R.id.rvQuestions)
        @Suppress("UNCHECKED_CAST")
        val adapter = rv.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        assertEquals(1, adapter.itemCount)
        val holder = adapter.createViewHolder(rv, 0)
        adapter.bindViewHolder(holder, 0)
        return holder.itemView
    }

    @Test fun stopAndResumeRechecksAnalyticsPermissionAndHidesOldPrivilegedRows() {
        val c = open()
        try {
            assertEquals(1, EditExamBoundaryFixture.freshReads)
            c.pause().stop()
            assertEquals(View.INVISIBLE,
                c.get().findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0).visibility)
            EditExamBoundaryFixture.permissions = AdminMeData(
                email="owner@example.test", rights=emptyList())
            c.start().resume().visible()
            idle()
            assertTrue(c.get().isFinishing)
            assertEquals(2, EditExamBoundaryFixture.freshReads)
        } finally { c.destroy(); idle() }
    }

    @Test fun revokedQuestionRightCannotDispatchMutationWhileAuditViewingRemainsAllowed() {
        val c = open()
        try {
            val row = editorRow(c.get())
            row.findViewById<View>(R.id.btnEditQuestion).performClick()
            idle()
            val dialog = ShadowDialog.getLatestDialog()
                as com.eve.app.designsystem.TelegramNativeAlertDialog
            assertTrue(dialog.isShowing)
            EditExamBoundaryFixture.permissions = AdminMeData(
                email="owner@example.test", rights=listOf("view_analytics_logs"))
            dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
            idle()
            assertEquals(0, EditExamBoundaryFixture.writes)
            assertTrue("An unconfirmed save must retain the draft", dialog.isShowing)
            assertTrue(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.isEnabled)
        } finally {
            ShadowDialog.getLatestDialog()?.dismiss()
            c.pause().stop().destroy(); idle()
        }
    }

    @Test fun suspendedPreflightCannotWriteAfterStopOrAccountReplacement() {
        val c = open()
        try {
            editorRow(c.get()).findViewById<View>(R.id.btnEditQuestion).performClick()
            idle()
            val dialog = ShadowDialog.getLatestDialog()
                as com.eve.app.designsystem.TelegramNativeAlertDialog
            EditExamBoundaryFixture.holdMembership = true
            dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
            idle()
            val pending = requireNotNull(EditExamBoundaryFixture.pendingMembership)
            assertEquals(0, EditExamBoundaryFixture.writes)
            c.pause().stop()
            ProfileEditorAuthFixture.setOwner("owner-b")
            pending.resume(true)
            idle()
            assertEquals(0, EditExamBoundaryFixture.writes)
            assertFalse(dialog.isShowing)
        } finally { c.destroy(); idle() }
    }
}
