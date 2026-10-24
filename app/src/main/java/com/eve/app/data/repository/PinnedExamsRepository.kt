package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PinnedExamsRepository {

    private val api = ApiClient.api
    private val scope = CoroutineScope(Dispatchers.IO)
    private val pinnedIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    fun observePinnedExamIds(userId: String): Flow<Set<String>> {
        if (userId.isNotBlank()) {
            scope.launch {
                refreshPinnedExams()
            }
        }
        return pinnedIdsFlow.asStateFlow()
    }

    private suspend fun refreshPinnedExams(): Set<String> = try {
        val res = api.getPinnedExams()
        val set = res.data?.toSet() ?: emptySet()
        pinnedIdsFlow.value = set
        set
    } catch (_: Exception) {
        pinnedIdsFlow.value
    }

    suspend fun pinExam(userId: String, examId: String): Result<Unit> = runCatching {
        if (userId.isBlank() || examId.isBlank()) return@runCatching
        api.pinExam(examId)
        pinnedIdsFlow.value = pinnedIdsFlow.value + examId
    }

    suspend fun unpinExam(userId: String, examId: String): Result<Unit> = runCatching {
        if (userId.isBlank() || examId.isBlank()) return@runCatching
        api.unpinExam(examId)
        pinnedIdsFlow.value = pinnedIdsFlow.value - examId
    }

    suspend fun togglePin(userId: String, examId: String, currentlyPinned: Boolean): Result<Unit> {
        return if (currentlyPinned) {
            unpinExam(userId, examId)
        } else {
            pinExam(userId, examId)
        }
    }
}
