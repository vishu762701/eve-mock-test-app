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

enum class AnalyticsTimeRange(val days: Int, val label: String) {
    LAST_7_DAYS(7, "Last 7 days"),
    LAST_30_DAYS(30, "Last 30 days"),
    ALL_TIME(0, "All time")
}

data class AdminAnalyticsUi(
    val exams: List<ExamAnalytics>,
    val questions: List<QuestionAnalytics>,
    val timeRange: AnalyticsTimeRange = AnalyticsTimeRange.ALL_TIME,
    val selectedExamId: String = ""
)

class AdminAnalyticsViewModel : ViewModel() {
    private val repo = AdminAnalyticsRepository()
    private val _state = MutableStateFlow<UiState<AdminAnalyticsUi>>(UiState.Loading)
    val state: StateFlow<UiState<AdminAnalyticsUi>> = _state.asStateFlow()

    private var currentTimeRange: AnalyticsTimeRange = AnalyticsTimeRange.ALL_TIME
    private var currentExamId: String = ""

    fun load(timeRange: AnalyticsTimeRange = currentTimeRange, examId: String = currentExamId) {
        currentTimeRange = timeRange
        currentExamId = examId
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val exams = repo.getExamAnalytics(timeRange.days)
                val questions = repo.getQuestionAnalytics(examId.ifBlank { null })
                UiState.Success(AdminAnalyticsUi(exams, questions, timeRange, examId))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load analytics")
            }
        }
    }
}
