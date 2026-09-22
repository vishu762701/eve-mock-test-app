package com.eve.app.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.PerformanceData
import com.eve.app.data.model.ScorePoint
import com.eve.app.data.model.TestAttempt
import com.eve.app.data.model.TopicStat
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 17: Performance Analytics. Koi naya Firestore collection nahi chahiye — Test History
 * (Phase 10) ke saare attempts `HistoryRepository` se load karke client-side aggregate karte
 * hain: score/accuracy trend, aur har answer ke saath save hue `topic` (Phase 17 se pehle ke
 * attempts me blank hoga, wo "General" bucket me chala jaata hai) se topic-wise accuracy.
 */
class PerformanceViewModel : ViewModel() {

    private val repo = HistoryRepository()

    private val _state = MutableStateFlow<UiState<PerformanceData>>(UiState.Loading)
    val state: StateFlow<UiState<PerformanceData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _state.value = UiState.Success(PerformanceData.EMPTY)
            return
        }
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(compute(repo.getAttempts(userId)))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Performance data load nahi hui")
            }
        }
    }

    private fun compute(attempts: List<TestAttempt>): PerformanceData {
        val totalAttempts = attempts.size
        val totalQuestions = attempts.sumOf { it.total }
        val totalCorrect = attempts.sumOf { it.correct }
        val overallAccuracy =
            if (totalQuestions == 0) 0 else ((totalCorrect * 100.0) / totalQuestions).roundToInt()

        // Chronological (oldest -> newest) taaki graph left-se-right progress dikhaye;
        // sirf last 15 taaki chart bahut zyada attempts me bhi crowded na ho.
        val trend = attempts.sortedBy { it.timestamp }.takeLast(15).map { a ->
            val pct = if (a.total == 0) 0 else ((a.correct * 100.0) / a.total).roundToInt()
            ScorePoint(timestamp = a.timestamp, examName = a.examName, percent = pct)
        }

        val topicTotals = LinkedHashMap<String, IntArray>() // topic -> [correct, total]
        attempts.forEach { attempt ->
            attempt.answers.forEach { ans ->
                val topic = ans.topic.ifBlank { "General" }
                val bucket = topicTotals.getOrPut(topic) { intArrayOf(0, 0) }
                if (ans.isCorrect) bucket[0]++
                bucket[1]++
            }
        }
        val topicStats = topicTotals.map { (topic, bucket) -> TopicStat(topic, bucket[0], bucket[1]) }
            .sortedBy { it.accuracy }

        return PerformanceData(totalAttempts, totalQuestions, overallAccuracy, trend, topicStats)
    }
}
