package com.eve.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Task 4: Repository to manage pinned exams per user in Firestore.
 * Path: users/{userId}/pinned_exams/{examId}
 */
class PinnedExamsRepository {

    private val db = FirebaseFirestore.getInstance()

    fun observePinnedExamIds(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userId)
            .collection("pinned_exams")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val set = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                trySend(set)
            }

        awaitClose { listener.remove() }
    }

    suspend fun pinExam(userId: String, examId: String): Result<Unit> = runCatching {
        if (userId.isBlank() || examId.isBlank()) return@runCatching
        db.collection("users")
            .document(userId)
            .collection("pinned_exams")
            .document(examId)
            .set(hashMapOf("pinnedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun unpinExam(userId: String, examId: String): Result<Unit> = runCatching {
        if (userId.isBlank() || examId.isBlank()) return@runCatching
        db.collection("users")
            .document(userId)
            .collection("pinned_exams")
            .document(examId)
            .delete()
            .await()
    }

    suspend fun togglePin(userId: String, examId: String, currentlyPinned: Boolean): Result<Unit> {
        return if (currentlyPinned) {
            unpinExam(userId, examId)
        } else {
            pinExam(userId, examId)
        }
    }
}
