package com.eve.app.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.data.repository.BookmarkRepository
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BookmarksViewModel : ViewModel() {

    private val repository = BookmarkRepository()

    private val _state = MutableStateFlow<UiState<List<BookmarkedQuestion>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BookmarkedQuestion>>> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        load()
    }

    fun load() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _state.value = UiState.Success(emptyList())
            return
        }

        observeJob?.cancel()
        _state.value = UiState.Loading
        observeJob = viewModelScope.launch {
            repository.observeBookmarkedQuestions(user.uid)
                .catch { e ->
                    _state.value = UiState.Error(e.message ?: "Failed to load bookmarks")
                }
                .collect { list ->
                    _state.value = UiState.Success(list)
                }
        }
    }

    fun unbookmark(questionId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            repository.unbookmarkQuestion(user.uid, questionId)
        }
    }
}
