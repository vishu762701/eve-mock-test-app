package com.eve.app.ui.test

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.Question
import com.eve.app.data.repository.DailyGkRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestViewModel : ViewModel() {

    private val repo = ExamRepository()
    private val dailyRepo = DailyGkRepository()
    private val historyRepo = HistoryRepository()

    private val _questions = MutableStateFlow<UiState<List<Question>>>(UiState.Loading)
    val questions: StateFlow<UiState<List<Question>>> = _questions.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(-1L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    /** Timer 0 par pahunchte hi true -> auto submit */
    private val _timeUp = MutableStateFlow(false)
    val timeUp: StateFlow<Boolean> = _timeUp.asStateFlow()

    private val _alreadyAttempted = MutableStateFlow(false)
    val alreadyAttempted: StateFlow<Boolean> = _alreadyAttempted.asStateFlow()

    // position -> "A".."D". Swipe karne par selection yahin se wapas milta hai.
    private val answers = mutableMapOf<Int, String>()

    // position -> bookmarked? "Review ke liye flag" state, swipe karne par bhi yaad rehta hai.
    private val bookmarks = mutableMapOf<Int, Boolean>()

    private var started = false
    private var timerJob: Job? = null

    fun start(
        examId: String,
        timeLimitMinutes: Int,
        topic: String = "",
        pyqYear: Int = 0,
        pyqPaper: String = "",
        quizDate: String = "",
        isAdmin: Boolean = false
    ) {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                val isStandardMock = quizDate.isBlank() && topic.isBlank() && pyqYear == 0
                if (!isAdmin && user != null && examId.isNotBlank() && isStandardMock && historyRepo.hasAttempted(user.uid, examId)) {
                    _alreadyAttempted.value = true
                    started = false
                    return@launch
                }
                val list = when {
                    // Phase 20: Daily GK — date-tagged questions, order preserve
                    quizDate.isNotBlank() -> dailyRepo.getQuestionsForDate(quizDate).map { it.toQuestion() }
                    // Phase 19: PYQ paper — original order preserve (shuffle nahi), taaki
                    // admin jaisa upload kiya waisa paper feel rahe.
                    pyqYear > 0 -> repo.getPyqQuestions(examId, pyqYear, pyqPaper)
                    topic.isNotBlank() -> repo.getQuestionsForTopic(examId, topic).shuffled().take(10)
                    else -> repo.getMockQuestions(examId).shuffled()
                }
                _questions.value = UiState.Success(list)
                if (list.isNotEmpty()) startTimer(timeLimitMinutes * 60L)
            } catch (e: Exception) {
                started = false
                _questions.value = UiState.Error(e.message ?: "Questions load nahi hue")
            }
        }
    }

    fun retry(
        examId: String,
        timeLimitMinutes: Int,
        topic: String = "",
        pyqYear: Int = 0,
        pyqPaper: String = "",
        quizDate: String = "",
        isAdmin: Boolean = false
    ) {
        _questions.value = UiState.Loading
        _alreadyAttempted.value = false
        started = false
        start(examId, timeLimitMinutes, topic, pyqYear, pyqPaper, quizDate, isAdmin)
    }

    private fun startTimer(totalSeconds: Long) {
        val endAt = SystemClock.elapsedRealtime() + totalSeconds * 1000
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val leftMs = endAt - SystemClock.elapsedRealtime()
                val left = ((leftMs + 999) / 1000).coerceAtLeast(0)
                _remainingSeconds.value = left
                if (left <= 0) {
                    _timeUp.value = true
                    break
                }
                delay(500)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun getAnswer(position: Int): String = answers[position] ?: ""

    fun setAnswer(position: Int, letter: String) {
        if (letter.isEmpty()) answers.remove(position) else answers[position] = letter
    }

    fun isBookmarked(position: Int): Boolean = bookmarks[position] ?: false

    fun toggleBookmark(position: Int) {
        bookmarks[position] = !isBookmarked(position)
    }

    fun buildAnswerItems(): ArrayList<AnswerItem> {
        val list = (questions.value as? UiState.Success)?.data ?: emptyList()
        val items = ArrayList<AnswerItem>()
        list.forEachIndexed { index, q ->
            val selected = getAnswer(index)
            items.add(
                AnswerItem(
                    questionId = q.id,
                    number = index + 1,
                    questionText = q.questionText,
                    questionTextHi = q.questionTextHi,
                    selected = selected,
                    selectedText = if (selected.isEmpty()) "" else q.optionText(selected),
                    selectedTextHi = if (selected.isEmpty()) "" else q.optionTextHi(selected),
                    correct = q.correctAnswer,
                    correctText = q.optionText(q.correctAnswer),
                    correctTextHi = q.optionTextHi(q.correctAnswer),
                    explanation = q.explanation,
                    explanationHi = q.explanationHi,
                    isBookmarked = isBookmarked(index),
                    topic = q.topic
                )
            )
        }
        return items
    }

    /**
     * Test History (Phase 10): submit hote hi attempt server se save karo, logged in ho tabhi.
     * Phase 23: userId ab bheja hi nahi jaata — submitAttempt Cloud Function apne auth
     * context (request.auth.uid) se hi decide karta hai, isliye client isko spoof nahi
     * kar sakta.
     */
    fun saveAttempt(examId: String, examName: String, category: String, items: List<AnswerItem>) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val displayName = user.displayName?.ifBlank { null } ?: "Student"
        historyRepo.saveAttempt(examId, examName, category, displayName, items)
    }
}
