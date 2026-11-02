package com.eve.app.ui.admin

import android.app.Application
import android.app.Activity
import android.widget.Spinner
import com.eve.app.R
import com.eve.app.data.model.Exam
import org.robolectric.Robolectric
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.ExamRepository
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real admin state owner/repository; only transport timing and replies are controlled. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class QuestionListOwnershipTest {
    private class Harness {
        data class Read(val examId: String, val reply: Continuation<Any?>)
        val reads = mutableListOf<Read>()
        var write: Continuation<Any?>? = null
        private val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, args ->
            @Suppress("UNCHECKED_CAST")
            val reply = args!!.last() as Continuation<Any?>
            when (method.name) {
                "getQuestions" -> reads.add(Read(args.first() as String, reply))
                "addQuestion" -> write = reply
                else -> error("Unexpected request ${method.name}")
            }
            COROUTINE_SUSPENDED
        } as EveApiService
        val vm = AdminViewModel(ExamRepository(api))
        private val store = ViewModelStore().apply { put("admin", vm) }
        fun idle() = shadowOf(Looper.getMainLooper()).idle()
        fun load(id: String) { vm.loadQuestions(id); idle() }
        fun answer(index: Int, text: String) {
            val read = reads[index]
            read.reply.resume(ApiResponse(true, listOf(question(read.examId, text))))
            idle()
        }
        fun close() { store.clear(); idle() }
    }
    companion object {
        private fun question(exam: String, text: String) = Question(id=text, examId=exam,
            questionText=text, optionA="A", optionB="B", optionC="C", optionD="D", correctAnswer="A")
    }
    private fun exercise(block: Harness.() -> Unit) {
        val h = Harness()
        try { h.block() } finally { h.close() }
    }
    @Test fun olderExamCannotOverwriteNewSelection() = exercise {
        load("old"); load("current")
        answer(1, "Current question"); answer(0, "Obsolete question")
        assertEquals(listOf("Current question"), vm.questions.value.map { it.questionText })
    }
    @Test fun olderRefreshOfSameExamCannotOverwriteLatestRefresh() = exercise {
        load("exam"); load("exam")
        answer(1, "Latest edit"); answer(0, "Old edit")
        assertEquals("Latest edit", vm.questions.value.single().questionText)
    }
    @Test fun obsoleteFailureDoesNotPublishAnErrorForCurrentExam() = exercise {
        load("old"); load("current"); answer(1, "Current")
        reads[0].reply.resume(ApiResponse<List<Question>>(false, error="Old request denied")); idle()
        assertNull(vm.message.value)
        load("current")
        reads[2].reply.resume(ApiResponse<List<Question>>(false, error="Current request denied")); idle()
        assertEquals("Current request denied", vm.message.value)
        assertEquals("Current", vm.questions.value.single().questionText)
    }
    @Test fun clearingSelectionInvalidatesPendingRead() = exercise {
        load("old"); load(""); answer(0, "Old")
        assertTrue(vm.questions.value.isEmpty())
        assertEquals(1, reads.size)
    }
    @Test fun switchingExamImmediatelyRemovesOldActionableRows() = exercise {
        load("old"); answer(0, "Old")
        load("old")
        assertEquals("Old", vm.questions.value.single().questionText)
        load("current")
        assertTrue(vm.questions.value.isEmpty())
    }
    @Test fun completedOldExamMutationDoesNotSelectOrReloadOldExam() = exercise {
        load("old"); answer(0, "Old")
        vm.addQuestion(question("old", "Added")) {}; idle()
        load("current"); answer(1, "Current")
        requireNotNull(write).resume(ApiResponse<Any>(true)); idle()
        assertEquals(listOf("old", "current"), reads.map { it.examId })
        assertEquals("Current", vm.questions.value.single().questionText)
    }
    @Test fun confirmedWriteRefreshesTheStillSelectedExam() = exercise {
        load("exam"); answer(0, "Existing")
        vm.addQuestion(question("exam", "Added")) {}; idle()
        requireNotNull(write).resume(ApiResponse<Any>(true)); idle()
        assertEquals(listOf("exam", "exam"), reads.map { it.examId })
        assertEquals("Existing", vm.questions.value.single().questionText)
        answer(1, "Confirmed new list")
        assertEquals("Confirmed new list", vm.questions.value.single().questionText)
    }
    @Test fun cancelledReadDoesNotPublishAUserFacingFailure() = exercise {
        load("exam")
        reads[0].reply.resumeWith(Result.failure(kotlinx.coroutines.CancellationException("Stopped")))
        idle()
        assertNull(vm.message.value)
        assertTrue(vm.questions.value.isEmpty())
    }
    @Test fun actualSpinnerCallbacksPreserveIdentityAcrossReorderAndRecreation() = exercise {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        try {
            val spinner = Spinner(controller.get())
            controller.get().setContentView(spinner)
            val old = Exam(id="old", examName="Old")
            val current = Exam(id="current", examName="Current")
            bindQuestionExamChoices(spinner, listOf(old, current), vm); idle()
            assertEquals(listOf("old"), reads.map { it.examId })
            spinner.setSelection(1); idle()
            assertEquals("current", vm.questionExamId)
            assertEquals(listOf("old", "current"), reads.map { it.examId })
            spinner.setSelection(1); idle()
            assertEquals("Selected-item callback is a no-op", 2, reads.size)
            bindQuestionExamChoices(spinner, listOf(current, old), vm); idle()
            assertEquals(0, spinner.selectedItemPosition)
            assertEquals("current", vm.questionExamId)
            assertEquals(listOf("old", "current", "current"), reads.map { it.examId })
            // A newly created View binds the existing retained ViewModel's identity.
            val recreatedSpinner = Spinner(controller.get())
            controller.get().setContentView(recreatedSpinner)
            bindQuestionExamChoices(recreatedSpinner, listOf(old, current), vm); idle()
            assertEquals(1, recreatedSpinner.selectedItemPosition)
            assertEquals("current", vm.questionExamId)
            bindQuestionExamChoices(recreatedSpinner, listOf(old), vm); idle()
            assertEquals("old", vm.questionExamId)
            bindQuestionExamChoices(recreatedSpinner, emptyList(), vm); idle()
            assertEquals("", vm.questionExamId)
            answer(reads.lastIndex, "Removed exam's late result")
            assertTrue(vm.questions.value.isEmpty())
        } finally { controller.pause().stop().destroy() }
    }

}
