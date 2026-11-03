package com.eve.app.ui.result

import com.eve.app.data.model.AnswerItem
import com.google.firebase.auth.FirebaseAuth

/** Existing bounded in-memory handoff; owner/exam tags prevent stale or foreign result reads. */
object ResultDataHolder {
    private data class Payload(val uid: String, val examId: String, val items: List<AnswerItem>)
    private var answers: Payload? = null
    private var detailItems: Payload? = null
    private fun current(uid: String) = uid.isNotBlank() && FirebaseAuth.getInstance().currentUser?.uid == uid
    private fun Payload.matches(uid: String, exam: String) = current(uid) && this.uid == uid && examId == exam && exam.isNotBlank()

    @Synchronized
    fun setAnswers(items: List<AnswerItem>, userId: String, examId: String): Boolean {
        if (!current(userId) || examId.isBlank()) return false
        val payload = Payload(userId, examId, items.toList())
        answers = payload; detailItems = payload
        return true
    }
    @Synchronized
    fun setDetailItems(items: List<AnswerItem>, userId: String, examId: String): Boolean {
        if (!current(userId) || examId.isBlank()) return false
        detailItems = Payload(userId, examId, items.toList())
        return true
    }
    @Synchronized
    fun getDetailItems(userId: String, examId: String): List<AnswerItem> =
        detailItems?.takeIf { it.matches(userId, examId) }?.items.orEmpty()
    @Synchronized
    fun consumeAnswers(userId: String, examId: String): List<AnswerItem> {
        val result = answers?.takeIf { it.matches(userId, examId) } ?: return emptyList()
        answers = null
        return result.items
    }
    @Synchronized
    fun clear(userId: String, examId: String) {
        // A finishing old screen cannot erase a newer account/exam's handoff.
        if (answers?.uid == userId && answers?.examId == examId) answers = null
        if (detailItems?.uid == userId && detailItems?.examId == examId) detailItems = null
    }
}
