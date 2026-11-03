package com.eve.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.data.model.RankInfo
import com.eve.app.data.repository.LeaderboardRepository
import com.eve.app.util.UiState
import com.eve.app.util.SubmissionRetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {

    private val repo = LeaderboardRepository()
    private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid
    private var fetchJob: Job? = null
    private fun ownsLeaderboard(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(
            originalOwnerUid, FirebaseAuth.getInstance().currentUser?.uid)

    private val _state = MutableStateFlow<UiState<List<LeaderboardEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<LeaderboardEntry>>> = _state.asStateFlow()

    private val _rankInfo = MutableStateFlow<RankInfo?>(null)
    val rankInfo: StateFlow<RankInfo?> = _rankInfo.asStateFlow()

    fun load(examId: String) {
        fetchJob?.cancel()
        val userId = originalOwnerUid
        if (!ownsLeaderboard() || userId.isNullOrBlank()) {
            _rankInfo.value = null
            _state.value = UiState.Error("Sign in with the account that opened Leaderboard")
            return
        }
        fetchJob = viewModelScope.launch {
            _state.value = UiState.Loading
            _rankInfo.value = null
            try {
                // The top list is public to signed-in students, but the rank card is private.
                val top = repo.getTopScorers(examId)
                if (!ownsLeaderboard()) return@launch
                _state.value = UiState.Success(top)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (!ownsLeaderboard()) return@launch
                _state.value = UiState.Error(e.message ?: "Failed to load leaderboard")
            }
            // Never change the original owner after waiting for top scorers.
            try {
                val rank = repo.getUserRank(examId, userId)
                if (!ownsLeaderboard()) return@launch
                _rankInfo.value = rank
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (ownsLeaderboard()) _rankInfo.value = null
            }
        }
    }
}
