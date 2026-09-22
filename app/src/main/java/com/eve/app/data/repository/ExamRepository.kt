package com.eve.app.data.repository

import com.eve.app.data.model.Exam
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

    suspend fun addExam(name: String, minutes: Int) {
        val data = hashMapOf(
            "examName" to name,
            "timeLimitMinutes" to minutes
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
            "correctAnswer" to q.correctAnswer
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
            "correctAnswer" to q.correctAnswer
        )
        db.collection("questions").document(q.id).set(data).await()
    }

    suspend fun deleteQuestion(questionId: String) {
        db.collection("questions").document(questionId).delete().await()
    }
}
