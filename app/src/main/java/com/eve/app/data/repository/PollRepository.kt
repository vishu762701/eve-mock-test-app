package com.eve.app.data.repository

import com.eve.app.data.model.Poll
import com.eve.app.data.model.PollVote
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PollRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val pollsCollection = firestore.collection("polls")

    suspend fun getPolls(): List<Poll> {
        val snapshot = pollsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Poll::class.java) }
            .sortedByDescending { it.isCurrentlyActive }
    }

    suspend fun getActivePoll(): Poll? {
        val snapshot = pollsCollection
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(Poll::class.java) }
            .firstOrNull { it.isCurrentlyActive }
    }

    suspend fun createPoll(
        question: String,
        options: List<String>,
        endsAt: Long,
        active: Boolean,
        createdBy: String
    ): String {
        val initialCounts = options.indices.associate { it.toString() to 0L }
        val poll = Poll(
            question = question.trim(),
            options = options.map { it.trim() }.filter { it.isNotBlank() },
            createdAt = System.currentTimeMillis(),
            endsAt = endsAt,
            active = active,
            createdBy = createdBy,
            voteCounts = initialCounts
        )
        val docRef = pollsCollection.document()
        docRef.set(poll).await()
        return docRef.id
    }

    suspend fun updatePollStatus(pollId: String, active: Boolean) {
        pollsCollection.document(pollId).update("active", active).await()
    }

    suspend fun deletePoll(pollId: String) {
        pollsCollection.document(pollId).delete().await()
    }

    suspend fun getUserVote(pollId: String, uid: String): Int? {
        val snap = pollsCollection.document(pollId)
            .collection("votes")
            .document(uid)
            .get()
            .await()
        return if (snap.exists()) {
            snap.getLong("optionIndex")?.toInt()
        } else {
            null
        }
    }

    suspend fun submitVote(pollId: String, uid: String, optionIndex: Int) {
        val pollRef = pollsCollection.document(pollId)
        val voteRef = pollRef.collection("votes").document(uid)

        firestore.runTransaction { transaction ->
            val voteSnap = transaction.get(voteRef)
            if (voteSnap.exists()) {
                throw IllegalStateException("You have already voted in this poll.")
            }

            val pollSnap = transaction.get(pollRef)
            if (!pollSnap.exists()) {
                throw IllegalStateException("Poll not found.")
            }

            val poll = pollSnap.toObject(Poll::class.java)
                ?: throw IllegalStateException("Invalid poll data.")

            if (!poll.isCurrentlyActive) {
                throw IllegalStateException("This poll is closed or has expired.")
            }

            // Record user vote
            val voteData = mapOf(
                "optionIndex" to optionIndex,
                "votedAt" to System.currentTimeMillis()
            )
            transaction.set(voteRef, voteData)

            // Atomically increment the vote count for this option
            transaction.update(pollRef, "voteCounts.$optionIndex", FieldValue.increment(1))
        }.await()
    }
}
