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
import kotlinx.coroutines.launch

class PracticeViewModel : ViewModel() {
    private val repository = ExamRepository()
    private val _exams = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    val exams: StateFlow<UiState<List<Exam>>> = _exams.asStateFlow()
    private val _topics = MutableStateFlow<UiState<List<PracticeTopic>>>(UiState.Success(emptyList()))
    val topics: StateFlow<UiState<List<PracticeTopic>>> = _topics.asStateFlow()

    init { loadExams() }

    fun loadExams() = viewModelScope.launch {
        _exams.value = UiState.Loading
        _exams.value = try { UiState.Success(repository.getExams()) }
        catch (e: Exception) { UiState.Error(e.message ?: "Exams load nahi hue") }
    }

    fun loadTopics(examId: String) = viewModelScope.launch {
        if (examId.isBlank()) return@launch
        _topics.value = UiState.Loading
        _topics.value = try {
            val topics = repository.getQuestions(examId)
                .map { it.topic.trim() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .map { PracticeTopic(it.key, it.value) }
                .sortedBy { it.name.lowercase() }
            UiState.Success(topics)
        } catch (e: Exception) { UiState.Error(e.message ?: "Topics load nahi hue") }
    }
}
