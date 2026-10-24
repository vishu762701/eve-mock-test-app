package com.eve.app.data.repository

import com.eve.app.data.model.QuestionStat
import com.eve.app.data.remote.ApiClient

class QuestionStatsRepository {

    suspend fun getQuestionStat(questionId: String): QuestionStat? {
        if (questionId.isBlank()) return null
        return getQuestionStats(listOf(questionId))[questionId]
    }

    suspend fun getQuestionStatsForExam(examId: String): Map<String, QuestionStat> {
        if (examId.isBlank()) return emptyMap()
        return try {
            val questionsRes = ApiClient.api.getQuestions(examId)
            val questions = questionsRes.data ?: emptyList()
            val ids = questions.map { it.id }.filter { it.isNotBlank() }
            getQuestionStats(ids)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun getQuestionStats(questionIds: List<String>): Map<String, QuestionStat> {
        val nonBlank = questionIds.filter { it.isNotBlank() }.distinct()
        if (nonBlank.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, QuestionStat>()
        try {
            // Max 50 per request
            val chunks = nonBlank.chunked(50)
            for (chunk in chunks) {
                val idsParam = chunk.joinToString(",")
                val res = ApiClient.api.getQuestionStats(idsParam)
                val statsMap = res.data ?: emptyMap()
                for ((id, dto) in statsMap) {
                    val totalAttempts = dto.attempts.toLong()
                    val correctAttempts = dto.correct.toLong()
                    val avgTime = dto.avgTimeSeconds.toDouble()
                    val totalTime = (dto.avgTimeSeconds * dto.attempts).toLong()
                    result[id] = QuestionStat(
                        questionId = id,
                        examId = "",
                        totalAttempts = totalAttempts,
                        correctAttempts = correctAttempts,
                        totalTimeSeconds = totalTime,
                        avgTimeSeconds = avgTime
                    )
                }
            }
        } catch (_: Exception) {
        }
        return result
    }
}
