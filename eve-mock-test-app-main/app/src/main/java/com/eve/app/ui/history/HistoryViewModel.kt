package com.eve.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _state = MutableStateFlow<UiState<List<TestAttempt>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TestAttempt>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _state.value = UiState.Success(emptyList())
            return
        }
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repo.getAttempts(userId))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "History load nahi hui")
            }
        }
    }
}
