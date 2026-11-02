package com.eve.app.data.repository

import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService

class AdminAnalyticsRepository(private val api: EveApiService = ApiClient.api) {

    suspend fun getExamAnalytics(days: Int = 0): List<ExamAnalytics> {
        val res = api.getExamAnalytics(if (days > 0) days else null)
        if (!res.success || res.data == null) throw Exception(res.error ?: "Analytics response did not include data")
        return res.data.sortedWith(
            compareByDescending<ExamAnalytics> { it.attemptCount }.thenBy { it.examName }
        )
    }

    suspend fun getQuestionAnalytics(examId: String? = null): List<QuestionAnalytics> {
        val res = api.getQuestionAnalytics(examId)
        if (!res.success || res.data == null) throw Exception(res.error ?: "Analytics response did not include data")
        return res.data.sortedWith(
            compareByDescending<QuestionAnalytics> { it.wrongRate }
                .thenByDescending { it.wrong }
                .thenBy { it.questionNumber }
        )
    }
}
