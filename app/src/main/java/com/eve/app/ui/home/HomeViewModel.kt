package com.eve.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repo = ExamRepository()

    private val _state = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Exam>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repo.getExams())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Exams load nahi hue")
            }
        }
    }
}
