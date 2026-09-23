package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.data.model.Question
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ExamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun attemptLockId(userId: String, examId: String): String =
        "${userId}_${examId}"

    /**
     * Returns exams already completed by this user. The deterministic lock is checked first;
     * the legacy attempts query keeps old users backward-compatible.
     */
    suspend fun getAttemptedExamIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()

        val locks = db.collection("attempt_locks")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents
            .mapNotNull { it.getString("examId") }
            .toMutableSet()

        val legacy = db.collection("attempts")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents
            .mapNotNull { it.getString("examId") }

        locks.addAll(legacy)
        return locks
    }

    /** Test start se pehle deterministic server-side lock ko check karta hai. */
    suspend fun hasAttemptLock(userId: String, examId: String): Boolean {
        if (userId.isBlank() || examId.isBlank()) return false
        val lock = db.collection("attempt_locks").document(attemptLockId(userId, examId)).get().await()
        if (lock.exists()) return true

        // Purane attempts ke liye graceful backward compatibility.
        return !db.collection("attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("examId", examId)
            .limit(1)
            .get().await().isEmpty
    }

    suspend fun getExams(): List<Exam> =
        db.collection("exams").get().await().documents.mapNotNull { doc ->
            doc.toObject(Exam::class.java)?.copy(id = doc.id)
        }.sortedBy { it.examName }

    suspend fun getQuestions(examId: String): List<Question> =
        db.collection("questions").whereEqualTo("examId", examId).get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Question::class.java)?.copy(id = doc.id)
            }

    /** Phase 19 gap-fix: mock test me PYQ tagged questions mix nahi hone chahiye. */
    suspend fun getMockQuestions(examId: String): List<Question> =
        getQuestions(examId).filter { !it.isPyq }

    suspend fun getQuestionsForTopic(examId: String, topic: String): List<Question> =
        getQuestions(examId).filter { !it.isPyq && it.topic.trim().equals(topic.trim(), ignoreCase = true) }

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
        db.collection("questions").add(questionMap(q)).await()
    }

    /** Phase 21: 500-per-batch Firestore writes. 100 questions ~1-2 batches. */
    suspend fun addQuestions(questions: List<Question>): Int {
        if (questions.isEmpty()) return 0
        questions.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { q ->
                val ref = db.collection("questions").document()
                batch.set(ref, questionMap(q))
            }
            batch.commit().await()
        }
        return questions.size
    }

    private fun questionMap(q: Question): HashMap<String, Any> = hashMapOf(
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
