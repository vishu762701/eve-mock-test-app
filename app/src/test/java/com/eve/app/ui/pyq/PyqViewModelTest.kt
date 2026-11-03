package com.eve.app.ui.pyq

import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.util.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PyqViewModelTest {
    private val examA = Exam(id = "exam-A", examName = "Exam A")
    private val examB = Exam(id = "exam-B", examName = "Exam B")

    private fun paper(exam: Exam) = PyqSet(
        examId = exam.id,
        examName = exam.examName,
        category = exam.categoryOrOther,
        year = 2024,
        paper = "Paper 1",
        questionCount = 2,
        timeLimitMinutes = 2
    )

    @Test
    fun newerSelectionWinsWhenOlderTransportCompletesLast() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val a = CompletableDeferred<List<PyqSet>>()
            val b = CompletableDeferred<List<PyqSet>>()
            val vm = PyqViewModel(
                fetchExams = { listOf(examA, examB) },
                fetchSets = { if (it.id == examA.id) withContext(NonCancellable) { a.await() } else b.await() }
            )
            runCurrent()
            vm.loadSets(examA)
            runCurrent()
            vm.loadSets(examB)
            runCurrent()
            b.complete(listOf(paper(examB)))
            advanceUntilIdle()
            assertEquals("exam-B", vm.selectedExamId)
            assertEquals(listOf(paper(examB)), (vm.sets.value as UiState.Success).data)

            a.complete(listOf(paper(examA)))
            advanceUntilIdle()
            assertEquals(listOf(paper(examB)), (vm.sets.value as UiState.Success).data)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelledOlderErrorCannotOverwriteCurrentSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val a = CompletableDeferred<List<PyqSet>>()
            val b = CompletableDeferred<List<PyqSet>>()
            val vm = PyqViewModel(
                fetchExams = { listOf(examA, examB) },
                fetchSets = { if (it.id == examA.id) withContext(NonCancellable) { a.await() } else b.await() }
            )
            runCurrent()
            vm.loadSets(examA)
            runCurrent()
            vm.loadSets(examB)
            runCurrent()
            b.complete(listOf(paper(examB)))
            advanceUntilIdle()
            a.completeExceptionally(IllegalStateException("stale failed response"))
            advanceUntilIdle()
            assertEquals(listOf(paper(examB)), (vm.sets.value as UiState.Success).data)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun repeatedSelectionDoesNotIssueDuplicateReadAndClearInvalidatesResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var reads = 0
            val pending = CompletableDeferred<List<PyqSet>>()
            val vm = PyqViewModel(
                fetchExams = { listOf(examA) },
                fetchSets = { reads++; pending.await() }
            )
            runCurrent()
            vm.loadSets(examA)
            vm.loadSets(examA)
            runCurrent()
            assertEquals(1, reads)
            vm.clearSelection()
            pending.complete(listOf(paper(examA)))
            advanceUntilIdle()
            assertEquals(null, vm.selectedExamId)
            assertTrue((vm.sets.value as UiState.Success).data.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mismatchedExamResponseFailsClosed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = PyqViewModel(
                fetchExams = { listOf(examA) },
                fetchSets = { listOf(paper(examB)) }
            )
            runCurrent()
            vm.loadSets(examA)
            advanceUntilIdle()
            assertTrue(vm.sets.value is UiState.Error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
