package com.eve.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val repo = ExamRepository()
    private val adminRepo = AdminRepository()

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _admins = MutableStateFlow<List<String>>(emptyList())
    val admins: StateFlow<List<String>> = _admins.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Ek baar dikhane wala message (toast) */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadExams()
        loadAdmins()
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

    fun loadQuestions(examId: String) {
        if (examId.isEmpty()) {
            _questions.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _questions.value = repo.getQuestions(examId)
            } catch (e: Exception) {
                _message.value = e.message ?: "Questions load nahi hue"
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

    fun deleteExam(examId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.deleteExam(examId)
                _message.value = "Exam delete ho gaya"
                loadExams()
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Delete fail"
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
                loadQuestions(q.examId)
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Upload fail (Firestore rules check karo)"
            }
            _busy.value = false
        }
    }

    fun updateQuestion(q: Question, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.updateQuestion(q)
                _message.value = "Question update ho gaya"
                loadQuestions(q.examId)
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Update fail"
            }
            _busy.value = false
        }
    }

    fun deleteQuestion(q: Question) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.deleteQuestion(q.id)
                _message.value = "Question delete ho gaya"
                loadQuestions(q.examId)
            } catch (e: Exception) {
                _message.value = e.message ?: "Delete fail"
            }
            _busy.value = false
        }
    }

    fun loadAdmins() {
        viewModelScope.launch {
            try {
                _admins.value = adminRepo.getDynamicAdmins()
            } catch (e: Exception) {
                _message.value = e.message ?: "Admins load nahi hue"
            }
        }
    }

    fun addAdmin(email: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                adminRepo.addAdmin(email)
                _message.value = "Admin add ho gaya"
                loadAdmins()
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Admin add fail"
            }
            _busy.value = false
        }
    }

    fun removeAdmin(email: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                adminRepo.removeAdmin(email)
                _message.value = "Admin remove ho gaya"
                loadAdmins()
            } catch (e: Exception) {
                _message.value = e.message ?: "Remove fail"
            }
            _busy.value = false
        }
    }
}
