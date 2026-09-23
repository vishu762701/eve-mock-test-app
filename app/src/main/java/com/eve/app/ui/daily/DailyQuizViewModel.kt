package com.eve.app.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.DailyQuizDay
import com.eve.app.data.repository.DailyGkRepository
import com.eve.app.util.DateUtil
import com.eve.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DailyQuizViewModel : ViewModel() {

    private val repo = DailyGkRepository()

    private val _days = MutableStateFlow<UiState<List<DailyQuizDay>>>(UiState.Loading)
    val days: StateFlow<UiState<List<DailyQuizDay>>> = _days.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _days.value = UiState.Loading
            _days.value = try {
                UiState.Success(repo.getAvailableDays())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load daily quizzes")
            }
        }
    }

    fun today(days: List<DailyQuizDay>): DailyQuizDay? =
        days.find { it.date == DateUtil.todayIso() }
}
