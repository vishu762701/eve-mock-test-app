package com.eve.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.repository.AdminAnalyticsRepository
import com.eve.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminAnalyticsUi(
    val exams: List<ExamAnalytics>,
    val questions: List<QuestionAnalytics>,
    val selectedExamId: String = ""
)

class AdminAnalyticsViewModel : ViewModel() {
    private val repo = AdminAnalyticsRepository()
    private val _state = MutableStateFlow<UiState<AdminAnalyticsUi>>(UiState.Loading)
    val state: StateFlow<UiState<AdminAnalyticsUi>> = _state.asStateFlow()

    fun load(examId: String = "") {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val exams = repo.getExamAnalytics()
                val questions = repo.getQuestionAnalytics(examId.ifBlank { null })
                UiState.Success(AdminAnalyticsUi(exams, questions, examId))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load analytics")
            }
        }
    }
}
