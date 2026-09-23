package com.eve.app.data.repository

import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.QuestionAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AdminAnalyticsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getExamAnalytics(): List<ExamAnalytics> =
        db.collection("admin_analytics_exams")
            .get().await().documents.mapNotNull { d ->
                d.toObject(ExamAnalytics::class.java)?.copy(examId = d.id)
            }.sortedWith(compareByDescending<ExamAnalytics> { it.attemptCount }.thenBy { it.examName })

    suspend fun getQuestionAnalytics(examId: String? = null): List<QuestionAnalytics> {
        var query: Query = db.collection("admin_analytics_questions")
        if (!examId.isNullOrBlank()) query = query.whereEqualTo("examId", examId)
        return query.get().await().documents.mapNotNull { d ->
            d.toObject(QuestionAnalytics::class.java)?.copy(id = d.id)
        }.sortedWith(
            compareByDescending<QuestionAnalytics> { it.wrongRate }
                .thenByDescending { it.wrong }
                .thenBy { it.questionNumber }
        )
    }
}
