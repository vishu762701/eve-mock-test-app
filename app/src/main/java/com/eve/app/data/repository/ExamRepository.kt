package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.model.PyqSet
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repository for exams, questions, syllabus, and AI-generated tests via Cloudflare Worker API.
 * Uploads syllabus PDFs to Supabase Storage via Worker and manages metadata in D1.
 */
class ExamRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    /**
     * Returns exams already completed by this user.
     */
    suspend fun getAttemptedExamIds(userId: String): Set<String> {
        return try {
            val res = api.getAttemptLocks()
            (res.data ?: emptyList()).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /** Test start se pehle deterministic server-side lock ko check karta hai. */
    suspend fun hasAttemptLock(userId: String, examId: String): Boolean {
        if (examId.isBlank()) return false
        return try {
            val res = api.checkAttemptLock(examId)
            res.data?.hasLock == true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getExams(): List<Exam> {
        return try {
            val res = api.getExams()
            (res.data ?: emptyList()).sortedBy { it.examName }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getQuestions(examId: String): List<Question> {
        return try {
            val res = api.getQuestions(examId)
            res.data ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Phase 19 gap-fix: mock test me PYQ tagged questions mix nahi hone chahiye. */
    suspend fun getMockQuestions(examId: String): List<Question> =
        getQuestions(examId).filter { !it.isPyq }

    suspend fun getQuestionsForTopic(examId: String, topic: String): List<Question> =
        getQuestions(examId).filter { !it.isPyq && it.topic.trim().equals(topic.trim(), ignoreCase = true) }

    /**
     * Phase 19: PYQ questions.
     */
    suspend fun getPyqQuestions(examId: String, year: Int, paper: String = ""): List<Question> =
        getQuestions(examId).filter { q ->
            q.isPyq && q.pyqYear == year && (paper.isBlank() || q.pyqPaper.trim().equals(paper, ignoreCase = true))
        }

    suspend fun getPyqSets(exam: Exam): List<PyqSet> {
        val grouped = getQuestions(exam.id)
            .filter { it.isPyq && it.pyqYear > 0 }
            .groupBy { it.pyqYear to it.pyqPaper.trim() }
        return grouped.map { (key, list) ->
            val (year, paper) = key
            PyqSet(
                examId = exam.id,
                examName = exam.examName,
                category = exam.categoryOrOther,
                year = year,
                paper = paper,
                questionCount = list.size,
                // Full-size paper = exam ka official time; chhota set = 1 min/Q.
                timeLimitMinutes = if (list.size >= 20) exam.timeLimitMinutes.coerceAtLeast(1)
                else list.size.coerceAtLeast(1)
            )
        }.sortedWith(compareByDescending<PyqSet> { it.year }.thenBy { it.paper.lowercase() })
    }

    suspend fun addExam(
        name: String,
        minutes: Int,
        category: String,
        testNumber: String = "Test 1",
        questionCount: Int = 20,
        autoGenEnabled: Boolean = true,
        autoGenTime: String = "00:00",
        timezone: String = "Asia/Kolkata",
        generationPrompt: String = "",
        imageUrl: String = ""
    ): String {
        val trimmed = name.trim()
        val data = mapOf(
            "examName" to trimmed,
            "timeLimitMinutes" to minutes,
            "category" to category.trim(),
            "testNumber" to testNumber,
            "questionCount" to questionCount,
            "autoGenerationEnabled" to autoGenEnabled,
            "autoGenTime" to autoGenTime,
            "timezone" to timezone,
            "generationPrompt" to generationPrompt,
            "syllabusUrl" to "",
            "syllabusFileName" to "",
            "imageUrl" to imageUrl
        )
        val res = api.createExam(data)
        if (!res.success || res.data == null) {
            throw Exception(res.error ?: "Failed to create exam")
        }
        return res.data["id"] ?: ""
    }

    suspend fun updateExamImage(examId: String, imageUrl: String) {
        val res = api.updateExamImage(examId, mapOf("imageUrl" to imageUrl))
        if (!res.success) throw Exception(res.error ?: "Failed to update exam image")
    }

    suspend fun renameExam(examId: String, newName: String) {
        val trimmed = newName.trim()
        val res = api.renameExam(examId, mapOf("examName" to trimmed))
        if (!res.success) throw Exception(res.error ?: "Failed to rename exam")
    }

    suspend fun deleteExam(examId: String) {
        val res = api.deleteExam(examId)
        if (!res.success) throw Exception(res.error ?: "Failed to delete exam")
    }

    suspend fun updateExamFullSettings(
        examId: String,
        examName: String,
        testNumber: String,
        questionCount: Int,
        autoGenEnabled: Boolean,
        autoGenTime: String,
        syllabusUrl: String,
        syllabusFileName: String,
        generationPrompt: String
    ) {
        val data = mapOf<String, Any>(
            "examName" to examName.trim(),
            "testNumber" to testNumber.trim(),
            "questionCount" to questionCount,
            "autoGenerationEnabled" to autoGenEnabled,
            "autoGenTime" to autoGenTime.trim(),
            "syllabusUrl" to syllabusUrl,
            "syllabusFileName" to syllabusFileName,
            "generationPrompt" to generationPrompt.trim()
        )
        val res = api.updateExam(examId, data)
        if (!res.success) throw Exception(res.error ?: "Failed to update exam settings")
    }

    suspend fun uploadSyllabusPdf(examId: String, fileName: String, bytes: ByteArray): String {
        val reqBody = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
        val res = api.uploadSyllabus(
            id = examId,
            fileName = fileName,
            contentType = "application/pdf",
            body = reqBody
        )
        if (!res.success || res.data == null) {
            throw Exception(res.error ?: "Failed to upload syllabus")
        }
        return res.data.syllabusUrl
    }

    suspend fun removeSyllabusPdf(examId: String, syllabusUrl: String) {
        val res = api.removeSyllabus(examId)
        if (!res.success) throw Exception(res.error ?: "Failed to remove syllabus")
    }

    suspend fun addQuestion(q: Question) {
        val res = api.addQuestion(questionMap(q))
        if (!res.success) throw Exception(res.error ?: "Failed to add question")
    }

    suspend fun addQuestions(questions: List<Question>): Int {
        if (questions.isEmpty()) return 0
        val payload = mapOf("questions" to questions.map { questionMap(it) })
        val res = api.addQuestionsBatch(payload)
        if (!res.success) throw Exception(res.error ?: "Failed to add questions batch")
        return questions.size
    }

    private fun questionMap(q: Question): Map<String, Any> = mapOf(
        "examId" to q.examId,
        "questionText" to q.questionText,
        "optionA" to q.optionA,
        "optionB" to q.optionB,
        "optionC" to q.optionC,
        "optionD" to q.optionD,
        "correctAnswer" to q.correctAnswer,
        "explanation" to q.explanation,
        "topic" to q.topic,
        "isPyq" to q.isPyq,
        "pyqYear" to q.pyqYear,
        "pyqPaper" to q.pyqPaper,
        "questionTextHi" to q.questionTextHi,
        "optionAHi" to q.optionAHi,
        "optionBHi" to q.optionBHi,
        "optionCHi" to q.optionCHi,
        "optionDHi" to q.optionDHi,
        "explanationHi" to q.explanationHi
    )

    suspend fun updateQuestion(q: Question) {
        val res = api.updateQuestion(q.id, questionMap(q))
        if (!res.success) throw Exception(res.error ?: "Failed to update question")
    }

    suspend fun deleteQuestion(questionId: String) {
        val res = api.deleteQuestion(questionId)
        if (!res.success) throw Exception(res.error ?: "Failed to delete question")
    }

    suspend fun updateExamAiSettings(
        examId: String,
        syllabus: String,
        questionCount: Int,
        customPromptNotes: String,
        autoGenerationEnabled: Boolean
    ) {
        val data = mapOf<String, Any>(
            "syllabus" to syllabus,
            "questionCount" to questionCount,
            "customPromptNotes" to customPromptNotes,
            "autoGenerationEnabled" to autoGenerationEnabled
        )
        val res = api.updateExam(examId, data)
        if (!res.success) throw Exception(res.error ?: "Failed to update AI settings")
    }

    suspend fun getGeneratedTests(examId: String? = null): List<GeneratedTest> {
        return try {
            val res = api.getGeneratedTests(examId)
            (res.data ?: emptyList()).sortedByDescending { it.generatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveGeneratedTests(examId: String): List<GeneratedTest> {
        return getGeneratedTests(examId).filter { it.isLive }
    }

    suspend fun updateGeneratedTestStatus(testId: String, status: String) {
        val res = api.updateGeneratedTestStatus(testId, mapOf("status" to status))
        if (!res.success) throw Exception(res.error ?: "Failed to update generated test status")
    }

    suspend fun deleteGeneratedTest(testId: String) {
        val res = api.deleteGeneratedTest(testId)
        if (!res.success) throw Exception(res.error ?: "Failed to delete generated test")
    }
}
