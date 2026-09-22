package com.eve.app.ui.test

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.Question
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestViewModel : ViewModel() {

    private val repo = ExamRepository()

    private val _questions = MutableStateFlow<UiState<List<Question>>>(UiState.Loading)
    val questions: StateFlow<UiState<List<Question>>> = _questions.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(-1L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    /** Timer 0 par pahunchte hi true -> auto submit */
    private val _timeUp = MutableStateFlow(false)
    val timeUp: StateFlow<Boolean> = _timeUp.asStateFlow()

    // position -> "A".."D". Swipe karne par selection yahin se wapas milta hai.
    private val answers = mutableMapOf<Int, String>()

    private var started = false
    private var timerJob: Job? = null

    fun start(examId: String, timeLimitMinutes: Int) {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val list = repo.getQuestions(examId).shuffled()
                _questions.value = UiState.Success(list)
                if (list.isNotEmpty()) startTimer(timeLimitMinutes * 60L)
            } catch (e: Exception) {
                started = false
                _questions.value = UiState.Error(e.message ?: "Questions load nahi hue")
            }
        }
    }

    fun retry(examId: String, timeLimitMinutes: Int) {
        _questions.value = UiState.Loading
        start(examId, timeLimitMinutes)
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

    fun buildAnswerItems(): ArrayList<AnswerItem> {
        val list = (questions.value as? UiState.Success)?.data ?: emptyList()
        val items = ArrayList<AnswerItem>()
        list.forEachIndexed { index, q ->
            val selected = getAnswer(index)
            items.add(
                AnswerItem(
                    number = index + 1,
                    questionText = q.questionText,
                    selected = selected,
                    selectedText = if (selected.isEmpty()) "" else q.optionText(selected),
                    correct = q.correctAnswer,
                    correctText = q.optionText(q.correctAnswer)
                )
            )
        }
        return items
    }
}
