package com.eve.app.ui.result

import androidx.lifecycle.ViewModel
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.QuestionStat
import com.eve.app.data.repository.QuestionStatsRepository

/**
 * ViewModel for [ResultActivity] that preserves the submitted [AnswerItem] list
 * and caches community question stats across configuration changes.
 */
class ResultViewModel : ViewModel() {

    private val questionStatsRepo = QuestionStatsRepository()

    var allItems: List<AnswerItem> = emptyList()
        private set

    var questionStats: Map<String, QuestionStat>? = null
        private set

    fun initAnswers(answers: List<AnswerItem>) {
        if (allItems.isEmpty() && answers.isNotEmpty()) {
            allItems = answers
        }
    }

    fun updateAnswers(answers: List<AnswerItem>) {
        allItems = answers
    }

    suspend fun getOrFetchQuestionStats(qIds: List<String>): Map<String, QuestionStat> {
        questionStats?.let { return it }
        val loaded = try {
            questionStatsRepo.getQuestionStats(qIds)
        } catch (_: Exception) {
            emptyMap()
        }
        questionStats = loaded
        return loaded
    }
}

