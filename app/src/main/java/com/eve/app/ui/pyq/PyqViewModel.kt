package com.eve.app.ui.pyq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The selected exam is owned here. A cancelled or non-cooperative older response
 * must never publish a paper list/error for a newer spinner selection.
 */
class PyqViewModel(
    private val fetchExams: suspend () -> List<Exam> = { ExamRepository().getExams() },
    private val fetchSets: suspend (Exam) -> List<PyqSet> = { ExamRepository().getPyqSets(it) }
) : ViewModel() {
    private val _exams = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    val exams: StateFlow<UiState<List<Exam>>> = _exams.asStateFlow()

    private val _sets = MutableStateFlow<UiState<List<PyqSet>>>(UiState.Success(emptyList()))
    val sets: StateFlow<UiState<List<PyqSet>>> = _sets.asStateFlow()

    var selectedExamId: String? = null
        private set

    private var generation = 0L
    private var setsJob: Job? = null

    init {
        loadExams()
    }

    fun loadExams() = viewModelScope.launch {
        _exams.value = UiState.Loading
        _exams.value = try {
            UiState.Success(fetchExams())
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to load exams")
        }
    }

    fun clearSelection() {
        ++generation
        setsJob?.cancel()
        setsJob = null
        selectedExamId = null
        _sets.value = UiState.Success(emptyList())
    }

    fun loadSets(exam: Exam) {
        if (exam.id.isBlank()) {
            clearSelection()
            return
        }
        // Spinner rebind/recreation must not restart an active or completed request.
        if (selectedExamId == exam.id &&
            (_sets.value is UiState.Loading || _sets.value is UiState.Success)) return

        val request = ++generation
        setsJob?.cancel()
        selectedExamId = exam.id
        _sets.value = UiState.Loading
        setsJob = viewModelScope.launch {
            try {
                val result = fetchSets(exam)
                // Reject malformed/cross-exam data instead of silently relabeling it.
                if (result.any { it.examId != exam.id }) {
                    throw IllegalStateException("PYQ response does not match selected exam")
                }
                if (request == generation && selectedExamId == exam.id) {
                    _sets.value = UiState.Success(result)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (request == generation && selectedExamId == exam.id) {
                    _sets.value = UiState.Error(e.message ?: "Failed to load PYQs")
                }
            }
        }
    }
}
