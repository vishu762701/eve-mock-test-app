package com.eve.app.data.repository

import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.remote.ApiClient

class AdminAnalyticsRepository {

    private val api = ApiClient.api

    suspend fun getExamAnalytics(): List<ExamAnalytics> = try {
        val res = api.getExamAnalytics()
        res.data?.sortedWith(
            compareByDescending<ExamAnalytics> { it.attemptCount }.thenBy { it.examName }
        ) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getQuestionAnalytics(examId: String? = null): List<QuestionAnalytics> = try {
        val res = api.getQuestionAnalytics(examId)
        res.data?.sortedWith(
            compareByDescending<QuestionAnalytics> { it.wrongRate }
                .thenByDescending { it.wrong }
                .thenBy { it.questionNumber }
        ) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
