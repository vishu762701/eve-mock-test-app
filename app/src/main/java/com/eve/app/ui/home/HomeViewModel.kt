package com.eve.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repo = ExamRepository()
    private val _examState = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    private val _attemptedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedCategory = MutableStateFlow(Constants.CATEGORY_ALL)

    val state: StateFlow<UiState<HomeUiData>> =
        combine(_examState, _selectedCategory, _attemptedIds) { examState, selected, attempted ->
            when (examState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(examState.message)
                is UiState.Success -> UiState.Success(buildUiData(examState.data, selected, attempted))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch {
            _examState.value = UiState.Loading
            _examState.value = try { UiState.Success(repo.getExams()) }
            catch (e: Exception) { UiState.Error(e.message ?: "Failed to load exams") }
        }
    }

    fun loadForUser(userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            _examState.value = UiState.Loading
            try {
                _examState.value = UiState.Success(repo.getExams())
                _attemptedIds.value = if (isAdmin) emptySet() else repo.getAttemptedExamIds(userId)
            } catch (e: Exception) {
                _examState.value = UiState.Error(e.message ?: "Failed to load exams")
            }
        }
    }

    fun selectCategory(category: String) { _selectedCategory.value = category }

    private fun buildUiData(all: List<Exam>, selected: String, attempted: Set<String>): HomeUiData {
        val categories = listOf(Constants.CATEGORY_ALL) + all.map { it.categoryOrOther }.distinct().sorted()
        val effectiveSelected = if (selected in categories) selected else Constants.CATEGORY_ALL
        val filtered = if (effectiveSelected == Constants.CATEGORY_ALL) all else all.filter { it.categoryOrOther == effectiveSelected }
        val items = if (effectiveSelected == Constants.CATEGORY_ALL) {
            filtered.groupBy { it.categoryOrOther }.toSortedMap().flatMap { (category, exams) ->
                listOf(HomeListItem.Header(category)) + exams.sortedBy { it.examName }
                    .map { HomeListItem.ExamRow(it, it.id in attempted) }
            }
        } else {
            filtered.sortedBy { it.examName }.map { HomeListItem.ExamRow(it, it.id in attempted) }
        }
        return HomeUiData(categories, effectiveSelected, items)
    }
}
