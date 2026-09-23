package com.eve.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.UserStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val repo = ExamRepository()
    private val adminRepo = AdminRepository()
    private val userStatsRepo = UserStatsRepository()

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _admins = MutableStateFlow<List<String>>(emptyList())
    val admins: StateFlow<List<String>> = _admins.asStateFlow()

    private val _totalUsers = MutableStateFlow<Long?>(null)
    val totalUsers: StateFlow<Long?> = _totalUsers.asStateFlow()

    private val _onlineUsers = MutableStateFlow<Long?>(null)
    val onlineUsers: StateFlow<Long?> = _onlineUsers.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Ek baar dikhane wala message (toast) */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadExams()
        loadAdmins()
        loadUserStats()
    }

    fun loadUserStats() {
        viewModelScope.launch {
            try {
                _totalUsers.value = userStatsRepo.getTotalUserCount()
                _onlineUsers.value = userStatsRepo.getOnlineUserCount()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to load user stats"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun loadExams() {
        viewModelScope.launch {
            try {
                _exams.value = repo.getExams()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to load exams"
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
                _message.value = e.message ?: "Failed to load questions"
            }
        }
    }

    fun addExam(name: String, minutes: Int, category: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.addExam(name, minutes, category)
                _message.value = "Exam added successfully"
                loadExams()
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to add exam"
            }
            _busy.value = false
        }
    }

    fun deleteExam(examId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.deleteExam(examId)
                _message.value = "Exam deleted successfully"
                loadExams()
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to delete exam"
            }
            _busy.value = false
        }
    }

    fun addQuestions(questions: List<Question>, onDone: () -> Unit) {
        if (questions.isEmpty()) {
            _message.value = "No valid questions found for upload"
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val n = repo.addQuestions(questions)
                _message.value = "$n questions uploaded successfully"
                questions.firstOrNull()?.examId?.let { loadQuestions(it) }
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Bulk upload failed (check permissions)"
            }
            _busy.value = false
        }
    }

    fun addQuestion(q: Question, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.addQuestion(q)
                _message.value = "Question uploaded successfully"
                loadQuestions(q.examId)
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Upload failed (check permissions)"
            }
            _busy.value = false
        }
    }

    fun updateQuestion(q: Question, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.updateQuestion(q)
                _message.value = "Question updated successfully"
                loadQuestions(q.examId)
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to update question"
            }
            _busy.value = false
        }
    }

    fun deleteQuestion(q: Question) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.deleteQuestion(q.id)
                _message.value = "Question deleted successfully"
                loadQuestions(q.examId)
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to delete question"
            }
            _busy.value = false
        }
    }

    fun loadAdmins() {
        viewModelScope.launch {
            try {
                _admins.value = adminRepo.getDynamicAdmins()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to load admins"
            }
        }
    }

    fun addAdmin(email: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                adminRepo.addAdmin(email)
                _message.value = "Admin added successfully"
                loadAdmins()
                onDone()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to add admin"
            }
            _busy.value = false
        }
    }

    fun removeAdmin(email: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                adminRepo.removeAdmin(email)
                _message.value = "Admin removed successfully"
                loadAdmins()
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed to remove admin"
            }
            _busy.value = false
        }
    }
}
