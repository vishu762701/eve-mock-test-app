package com.eve.app.data.repository

import android.util.Log
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.TestAttempt
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HistoryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    /**
     * Phase 23 security fix: pehle yahan client khud score calculate karke seedha
     * Firestore "attempts" collection me likh deta tha (rules sirf userId check karte
     * the) — matlab modified client fake score/answers bhej sakta tha jo Leaderboard aur
     * Phase 22 Analytics dono ko spoof kar deta. Ab sirf itna bhejte hain: kaunsa
     * question dikha aur kaunsa option select kiya. Score/correctness/leaderboard-worthy
     * sab kuch functions/index.js ka `submitAttempt` Cloud Function khud, real
     * "questions"/"daily_questions" documents se (Admin SDK) calculate karta hai —
     * client ki bheji hui score/correct/answers value ab kahin trust nahi hoti.
     *
     * Jaanbujh kar viewModelScope use nahi kiya: TestActivity submit() ke turant baad
     * finish() ho jaata hai jo viewModelScope cancel kar deta, aur network call beech
     * me hi kat jaata. Isliye ek repository-level scope use kiya hai jo Activity/ViewModel
     * ke saath cancel nahi hota (process zinda rehne tak). Yeh purani offline-persistence
     * jitna robust nahi hai (process kill/no-network par retries ke baad bhi fail ho sakta
     * hai aur wo attempt History/Leaderboard me kabhi nahi aayega), lekin Result screen
     * turant local calculation se hi dikhta hai isliye student block nahi hota.
     */
    private val submitScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveAttempt(
        examId: String,
        examName: String,
        category: String,
        displayName: String,
        items: List<AnswerItem>
    ) {
        val payload = hashMapOf(
            "examId" to examId,
            "examName" to examName,
            "category" to category,
            "displayName" to displayName,
            "answers" to items.map { a ->
                hashMapOf(
                    "questionId" to a.questionId,
                    "number" to a.number,
                    "selected" to a.selected,
                    "isBookmarked" to a.isBookmarked
                )
            }
        )

        submitScope.launch {
            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                try {
                    functions.getHttpsCallable("submitAttempt").call(payload).await()
                    return@launch
                } catch (e: Exception) {
                    Log.w("HistoryRepository", "submitAttempt try $attempt/$maxAttempts failed", e)
                    if (attempt < maxAttempts) delay(2000L * attempt)
                }
            }
            // Sab retries fail — yeh attempt History/Leaderboard/Analytics me nahi
            // dikhega. Result screen already dikh chuka hoga (local calculation se),
            // isliye student ko pata nahi chalega ki background save fail hua.
        }
    }

    /** Backward-compatible attempt check: lock first, then legacy attempts. */
    suspend fun hasAttempted(userId: String, examId: String): Boolean {
        if (userId.isBlank() || examId.isBlank()) return false
        val lockId = "${userId}_${examId}"
        if (db.collection("attempt_locks").document(lockId).get().await().exists()) return true
        return !db.collection("attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("examId", examId)
            .limit(1)
            .get().await().isEmpty
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
                questionId = map["questionId"] as? String ?: "",
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
