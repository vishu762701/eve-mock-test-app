package com.eve.app.data.repository

import com.eve.app.data.model.Poll
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService

class PollRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    suspend fun getPolls(): List<Poll> {
        val res = api.getPolls()
        return (res.data ?: emptyList()).sortedByDescending { it.isCurrentlyActive }
    }

    suspend fun getActivePoll(): Poll? {
        val res = api.getActivePoll()
        return res.data?.takeIf { it.isCurrentlyActive }
    }

    suspend fun createPoll(
        question: String,
        options: List<String>,
        endsAt: Long,
        active: Boolean,
        createdBy: String
    ): String {
        val cleanedOptions = options.map { it.trim() }.filter { it.isNotBlank() }
        val body = mapOf(
            "question" to question.trim(),
            "options" to cleanedOptions,
            "endsAt" to endsAt,
            "active" to active,
            "createdBy" to createdBy
        )
        val res = api.createPoll(body)
        if (!res.success || res.data == null) {
            throw IllegalStateException(res.error ?: "Failed to create poll")
        }
        return res.data["id"] ?: ""
    }

    suspend fun updatePollStatus(pollId: String, active: Boolean) {
        val res = api.updatePollStatus(pollId, mapOf("active" to active))
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to update poll status")
        }
    }

    suspend fun deletePoll(pollId: String) {
        val res = api.deletePoll(pollId)
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to delete poll")
        }
    }

    suspend fun getUserVote(pollId: String, uid: String): Int? {
        val res = api.getUserVote(pollId)
        return if (res.success) res.data?.optionIndex else null
    }

    suspend fun submitVote(pollId: String, uid: String, optionIndex: Int) {
        val res = api.submitVote(pollId, mapOf("optionIndex" to optionIndex))
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to submit vote")
        }
    }
}
