package com.eve.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.util.SubmissionRetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.eve.app.data.model.TestAttempt
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    private val repo = HistoryRepository()
    private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid
    private var loadJob: Job? = null
    private fun ownsOriginalAccount(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(
            originalOwnerUid, FirebaseAuth.getInstance().currentUser?.uid)

    private val _state = MutableStateFlow<UiState<List<TestAttempt>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TestAttempt>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        val userId = originalOwnerUid
        if (!ownsOriginalAccount() || userId.isNullOrBlank()) {
            _state.value = UiState.Error("Sign in with the account that opened History")
            return
        }
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val attempts = repo.getAttempts(userId)
                if (!ownsOriginalAccount()) return@launch
                _state.value = UiState.Success(attempts)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (!ownsOriginalAccount()) return@launch
                _state.value = UiState.Error(e.message ?: "Failed to load history")
            }
        }
    }
}
