package com.eve.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.data.model.RankInfo
import com.eve.app.data.repository.LeaderboardRepository
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {

    private val repo = LeaderboardRepository()

    private val _state = MutableStateFlow<UiState<List<LeaderboardEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<LeaderboardEntry>>> = _state.asStateFlow()

    private val _rankInfo = MutableStateFlow<RankInfo?>(null)
    val rankInfo: StateFlow<RankInfo?> = _rankInfo.asStateFlow()

    fun load(examId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _rankInfo.value = null
            _state.value = try {
                UiState.Success(repo.getTopScorers(examId))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Leaderboard load nahi hui")
            }

            // Apna rank alag se load karte hain taaki top-scorers list, rank query fail
            // ho jaaye (jaise abhi tak koi entry hi nahi bani) tab bhi turant dikh jaaye.
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            _rankInfo.value = try {
                repo.getUserRank(examId, userId)
            } catch (e: Exception) {
                null
            }
        }
    }
}
