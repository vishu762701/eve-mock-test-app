package com.eve.app.data.repository

import com.eve.app.data.model.Poll
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService

class PollRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    suspend fun getPolls(): List<Poll> {
        val res = api.getPolls()
        if (!res.success || res.data == null) {
            throw IllegalStateException(res.error ?: "Failed to load polls")
        }
        return res.data.sortedByDescending { it.isCurrentlyActive }
    }

    suspend fun getActivePoll(): Poll? {
        val res = api.getActivePoll()
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to load active poll")
        }
        return res.data?.takeIf { it.isCurrentlyActive }
    }

    suspend fun createPoll(
        question: String,
        options: List<String>,
        endsAt: Long,
        active: Boolean,
        createdBy: String,
        quizMode: Boolean = false,
        correctOptionIndex: Int = -1,
        explanation: String = ""
    ): String {
        val cleanedOptions = options.map { it.trim() }.filter { it.isNotBlank() }
        val body: MutableMap<String, Any> = mutableMapOf(
            "question" to question.trim(),
            "options" to cleanedOptions,
            "endsAt" to endsAt,
            "active" to active,
            "createdBy" to createdBy
        )
        if (quizMode) {
            body["quizMode"] = true
            body["correctOptionIndex"] = correctOptionIndex
            if (explanation.isNotBlank()) {
                body["explanation"] = explanation.trim()
            }
        }
        val res = api.createPoll(body)
        if (!res.success || res.data == null) {
            throw IllegalStateException(res.error ?: "Failed to create poll")
        }
        return res.data["id"]?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Poll creation could not be verified: missing server ID")
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
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to get user vote")
        }
        return res.data?.optionIndex
    }

    suspend fun submitVote(pollId: String, uid: String, optionIndex: Int) {
        val res = api.submitVote(pollId, mapOf("optionIndex" to optionIndex))
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to submit vote")
        }
    }
}
