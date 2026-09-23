package com.eve.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.DailyQuestion
import com.eve.app.data.repository.DailyGkRepository
import com.eve.app.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DailyAdminViewModel : ViewModel() {

    private val repo = DailyGkRepository()

    private val _questions = MutableStateFlow<List<DailyQuestion>>(emptyList())
    val questions: StateFlow<List<DailyQuestion>> = _questions.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    var currentDate: String = DateUtil.todayIso()
        private set

    init {
        load(currentDate)
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun load(date: String) {
        currentDate = date
        viewModelScope.launch {
            try {
                _questions.value = repo.getQuestionsForDate(date)
            } catch (e: Exception) {
                _message.value = e.message ?: "Questions load nahi hue"
            }
        }
    }

    fun add(q: DailyQuestion, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.addQuestion(q)
                _message.value = "Daily GK question upload ho gaya"
                load(q.date)
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Upload fail (rules deploy kiye? daily_questions write admin-only hai)"
            }
            _busy.value = false
        }
    }

    fun addAll(questions: List<DailyQuestion>, onDone: () -> Unit) {
        if (questions.isEmpty()) {
            _message.value = "Upload ke liye koi valid question nahi mila"
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val n = repo.addQuestions(questions)
                _message.value = "$n Daily GK questions upload ho gaye"
                questions.lastOrNull()?.date?.let { load(it) }
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Upload fail (rules deploy kiye? daily_questions write admin-only hai)"
            }
            _busy.value = false
        }
    }

    fun update(q: DailyQuestion, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.updateQuestion(q)
                _message.value = "Question update ho gaya"
                load(q.date)
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Update fail"
            }
            _busy.value = false
        }
    }

    fun delete(q: DailyQuestion) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.deleteQuestion(q.id)
                _message.value = "Question delete ho gaya"
                load(q.date)
            } catch (e: Exception) {
                _message.value = e.message ?: "Delete fail"
            }
            _busy.value = false
        }
    }
}
