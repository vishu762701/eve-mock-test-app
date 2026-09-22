package com.eve.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.ExamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val repo = ExamRepository()

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Ek baar dikhane wala message (toast) */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadExams()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun loadExams() {
        viewModelScope.launch {
            try {
                _exams.value = repo.getExams()
            } catch (e: Exception) {
                _message.value = e.message ?: "Exams load nahi hue"
            }
        }
    }

    fun addExam(name: String, minutes: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.addExam(name, minutes)
                _message.value = "Exam add ho gaya"
                loadExams()
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Exam add fail"
            }
            _busy.value = false
        }
    }

    fun addQuestion(q: Question, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.addQuestion(q)
                _message.value = "Question upload ho gaya"
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Upload fail (Firestore rules check karo)"
            }
            _busy.value = false
        }
    }
}
