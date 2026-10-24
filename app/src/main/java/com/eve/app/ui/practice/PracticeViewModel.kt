package com.eve.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.PracticeTopic
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PracticeViewModel : ViewModel() {
    private val repository = ExamRepository()
    private val _exams = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    val exams: StateFlow<UiState<List<Exam>>> = _exams.asStateFlow()
    private val _topics = MutableStateFlow<UiState<List<PracticeTopic>>>(UiState.Success(emptyList()))
    val topics: StateFlow<UiState<List<PracticeTopic>>> = _topics.asStateFlow()
    private var topicsJob: Job? = null
    private var currentExamId: String? = null

    init { loadExams() }

    fun loadExams() = viewModelScope.launch {
        _exams.value = UiState.Loading
        _exams.value = try { UiState.Success(repository.getExams()) }
        catch (e: Exception) { UiState.Error(e.message ?: "Failed to load exams") }
    }

    fun loadTopics(examId: String) {
        // Batch 06 regression repair: never render results from a superseded exam.
        topicsJob?.cancel()
        currentExamId = examId.takeIf { it.isNotBlank() }
        if (examId.isBlank()) {
            _topics.value = UiState.Success(emptyList())
            topicsJob = null
            return
        }
        topicsJob = viewModelScope.launch {
            _topics.value = UiState.Loading
            try {
                val topics = repository.getMockQuestions(examId)
                    .map { it.topic.trim() }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .map { PracticeTopic(it.key, it.value) }
                    .sortedBy { it.name.lowercase() }
                if (isActive && currentExamId == examId) _topics.value = UiState.Success(topics)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive && currentExamId == examId) {
                    _topics.value = UiState.Error(e.message ?: "Failed to load topics")
                }
            }
        }
    }
}
