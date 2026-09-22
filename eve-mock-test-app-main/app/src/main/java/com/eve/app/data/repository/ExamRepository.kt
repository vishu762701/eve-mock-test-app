package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.data.model.Question
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ExamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getExams(): List<Exam> =
        db.collection("exams").get().await().documents.mapNotNull { doc ->
            doc.toObject(Exam::class.java)?.copy(id = doc.id)
        }.sortedBy { it.examName }

    suspend fun getQuestions(examId: String): List<Question> =
        db.collection("questions").whereEqualTo("examId", examId).get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Question::class.java)?.copy(id = doc.id)
            }

    suspend fun getQuestionsForTopic(examId: String, topic: String): List<Question> =
        db.collection("questions")
            .whereEqualTo("examId", examId)
            .whereEqualTo("topic", topic)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Question::class.java)?.copy(id = doc.id) }

    /**
     * Phase 19: PYQ questions. Composite index avoid karne ke liye exam ke saare questions
     * load karke client-side filter — Practice topics jaisa hi, offline cache bhi kaam karta hai.
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

    suspend fun addExam(name: String, minutes: Int, category: String) {
        val data = hashMapOf(
            "examName" to name,
            "timeLimitMinutes" to minutes,
            "category" to category
        )
        db.collection("exams").add(data).await()
    }

    suspend fun deleteExam(examId: String) {
        // Exam ke saath uske saare questions bhi delete karo
        val questions = db.collection("questions").whereEqualTo("examId", examId).get().await()
        for (doc in questions.documents) {
            db.collection("questions").document(doc.id).delete().await()
        }
        db.collection("exams").document(examId).delete().await()
    }

    suspend fun addQuestion(q: Question) {
        val data = hashMapOf(
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
        db.collection("questions").add(data).await()
    }

    suspend fun updateQuestion(q: Question) {
        val data = hashMapOf(
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
        db.collection("questions").document(q.id).set(data).await()
    }

    suspend fun deleteQuestion(questionId: String) {
        db.collection("questions").document(questionId).delete().await()
    }
}
