package com.eve.app.data.repository

import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.TestAttempt
import com.eve.app.util.Constants
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HistoryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Test submit hone ke turant baad call hota hai. Jaanbujh kar suspend nahi hai:
     * Firestore ka .add() call hote hi write local cache me turant ho jaata hai
     * (offline persistence EveApplication me on hai) aur background me server se sync
     * ho jaata hai — TestActivity finish() ho jaaye uske baad bhi yeh write lost nahi hota,
     * isliye coroutine scope (jo Activity/ViewModel ke saath cancel ho sakta hai) par depend
     * nahi karte.
     */
    fun saveAttempt(
        userId: String,
        displayName: String,
        examId: String,
        examName: String,
        category: String,
        items: List<AnswerItem>
    ) {
        val total = items.size
        val correct = items.count { it.isCorrect }
        val unattempted = items.count { !it.isAttempted }
        val wrong = total - correct - unattempted
        val score = correct - wrong * Constants.NEGATIVE_MARK

        val answersData = items.map { a ->
            hashMapOf(
                "number" to a.number,
                "questionText" to a.questionText,
                "selected" to a.selected,
                "selectedText" to a.selectedText,
                "correct" to a.correct,
                "correctText" to a.correctText,
                "explanation" to a.explanation,
                "isBookmarked" to a.isBookmarked,
                "topic" to a.topic,
                "questionTextHi" to a.questionTextHi,
                "selectedTextHi" to a.selectedTextHi,
                "correctTextHi" to a.correctTextHi,
                "explanationHi" to a.explanationHi
            )
        }

        val data = hashMapOf(
            "userId" to userId,
            "displayName" to displayName,
            "examId" to examId,
            "examName" to examName,
            "category" to category,
            "score" to score,
            "total" to total,
            "correct" to correct,
            "wrong" to wrong,
            "unattempted" to unattempted,
            "timestamp" to System.currentTimeMillis(),
            "answers" to answersData
        )
        db.collection("attempts").add(data)
    }

    /** Naye se purane order me — client side sort karte hain taaki composite index ki zaroorat na pade. */
    suspend fun getAttempts(userId: String): List<TestAttempt> =
        db.collection("attempts")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents.mapNotNull { doc -> parseAttempt(doc) }
            .sortedByDescending { it.timestamp }

    private fun parseAttempt(doc: DocumentSnapshot): TestAttempt? {
        val examId = doc.getString("examId") ?: return null
        val answersRaw = doc.get("answers") as? List<*> ?: emptyList<Any>()
        val answers = answersRaw.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            AnswerItem(
                number = (map["number"] as? Long)?.toInt() ?: 0,
                questionText = map["questionText"] as? String ?: "",
                selected = map["selected"] as? String ?: "",
                selectedText = map["selectedText"] as? String ?: "",
                correct = map["correct"] as? String ?: "",
                correctText = map["correctText"] as? String ?: "",
                explanation = map["explanation"] as? String ?: "",
                isBookmarked = map["isBookmarked"] as? Boolean ?: false,
                topic = map["topic"] as? String ?: "",
                questionTextHi = map["questionTextHi"] as? String ?: "",
                selectedTextHi = map["selectedTextHi"] as? String ?: "",
                correctTextHi = map["correctTextHi"] as? String ?: "",
                explanationHi = map["explanationHi"] as? String ?: ""
            )
        }
        return TestAttempt(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            displayName = doc.getString("displayName") ?: "",
            examId = examId,
            examName = doc.getString("examName") ?: "",
            category = doc.getString("category") ?: "",
            score = doc.getDouble("score") ?: 0.0,
            total = (doc.getLong("total") ?: 0L).toInt(),
            correct = (doc.getLong("correct") ?: 0L).toInt(),
            wrong = (doc.getLong("wrong") ?: 0L).toInt(),
            unattempted = (doc.getLong("unattempted") ?: 0L).toInt(),
            timestamp = doc.getLong("timestamp") ?: 0L,
            answers = answers
        )
    }
}
