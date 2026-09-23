package com.eve.app.data.repository

import com.eve.app.data.model.DailyQuestion
import com.eve.app.data.model.DailyQuizDay
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DailyGkRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val col get() = db.collection("daily_questions")

    suspend fun getQuestionsForDate(date: String): List<DailyQuestion> =
        col.whereEqualTo("date", date).get().await().documents.mapNotNull { doc ->
            parse(doc.id, doc.data)
        }

    /**
     * Composite index avoid — saari daily questions load karke client-side group.
     * Volume chhota rehta hai (10 Q/day), offline cache bhi kaam karta hai.
     */
    suspend fun getAvailableDays(): List<DailyQuizDay> {
        val grouped = col.get().await().documents.mapNotNull { doc ->
            parse(doc.id, doc.data)
        }.groupBy { it.date }
        return grouped.map { (date, list) ->
            DailyQuizDay(date = date, questionCount = list.size)
        }.sortedByDescending { it.date }
    }

    suspend fun addQuestion(q: DailyQuestion) {
        col.add(toMap(q)).await()
    }

    suspend fun addQuestions(questions: List<DailyQuestion>): Int {
        if (questions.isEmpty()) return 0
        questions.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { q ->
                batch.set(col.document(), toMap(q))
            }
            batch.commit().await()
        }
        return questions.size
    }

    suspend fun updateQuestion(q: DailyQuestion) {
        col.document(q.id).set(toMap(q)).await()
    }

    suspend fun deleteQuestion(id: String) {
        col.document(id).delete().await()
    }

    private fun toMap(q: DailyQuestion): HashMap<String, Any> = hashMapOf(
        "date" to q.date,
        "questionText" to q.questionText,
        "optionA" to q.optionA,
        "optionB" to q.optionB,
        "optionC" to q.optionC,
        "optionD" to q.optionD,
        "correctAnswer" to q.correctAnswer,
        "explanation" to q.explanation,
        "topic" to q.topic,
        "questionTextHi" to q.questionTextHi,
        "optionAHi" to q.optionAHi,
        "optionBHi" to q.optionBHi,
        "optionCHi" to q.optionCHi,
        "optionDHi" to q.optionDHi,
        "explanationHi" to q.explanationHi
    )

    private fun parse(id: String, data: Map<String, Any>?): DailyQuestion? {
        if (data == null) return null
        val date = data["date"] as? String ?: return null
        return DailyQuestion(
            id = id,
            date = date,
            questionText = data["questionText"] as? String ?: "",
            optionA = data["optionA"] as? String ?: "",
            optionB = data["optionB"] as? String ?: "",
            optionC = data["optionC"] as? String ?: "",
            optionD = data["optionD"] as? String ?: "",
            correctAnswer = data["correctAnswer"] as? String ?: "",
            explanation = data["explanation"] as? String ?: "",
            topic = data["topic"] as? String ?: "",
            questionTextHi = data["questionTextHi"] as? String ?: "",
            optionAHi = data["optionAHi"] as? String ?: "",
            optionBHi = data["optionBHi"] as? String ?: "",
            optionCHi = data["optionCHi"] as? String ?: "",
            optionDHi = data["optionDHi"] as? String ?: "",
            explanationHi = data["explanationHi"] as? String ?: ""
        )
    }
}
