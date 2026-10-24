package com.eve.app.ui.pyq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PyqViewModel : ViewModel() {
    private val repository = ExamRepository()

    private val _exams = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    val exams: StateFlow<UiState<List<Exam>>> = _exams.asStateFlow()

    private val _sets = MutableStateFlow<UiState<List<PyqSet>>>(UiState.Success(emptyList()))
    val sets: StateFlow<UiState<List<PyqSet>>> = _sets.asStateFlow()

    init {
        loadExams()
    }

    fun loadExams() = viewModelScope.launch {
        _exams.value = UiState.Loading
        _exams.value = try {
            UiState.Success(repository.getExams())
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to load exams")
        }
    }

    fun loadSets(exam: Exam) = viewModelScope.launch {
        _sets.value = UiState.Loading
        _sets.value = try {
            UiState.Success(repository.getPyqSets(exam))
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to load PYQs")
        }
    }
}
