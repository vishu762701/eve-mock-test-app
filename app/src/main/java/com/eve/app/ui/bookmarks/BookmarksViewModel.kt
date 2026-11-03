package com.eve.app.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.data.repository.BookmarkRepository
import com.eve.app.util.UiState
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BookmarksViewModel : ViewModel() {

    private val repository = BookmarkRepository()
    private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid
    private fun ownsOriginalAccount(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(
            originalOwnerUid, FirebaseAuth.getInstance().currentUser?.uid)

    private val _state = MutableStateFlow<UiState<List<BookmarkedQuestion>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BookmarkedQuestion>>> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        load()
    }

    fun load() {
        observeJob?.cancel()
        val uid = originalOwnerUid
        if (!ownsOriginalAccount() || uid.isNullOrBlank()) {
            _state.value = UiState.Error("Sign in with the account that opened Bookmarks")
            return
        }
        _state.value = UiState.Loading
        observeJob = viewModelScope.launch {
            repository.observeBookmarkedQuestions(uid)
                .catch { e ->
                    if (ownsOriginalAccount()) {
                        _state.value = UiState.Error(e.message ?: "Failed to load bookmarks")
                    }
                }
                .collect { list ->
                    if (!ownsOriginalAccount()) {
                        observeJob?.cancel()
                        return@collect
                    }
                    _state.value = UiState.Success(list)
                }
        }
    }

    fun unbookmark(questionId: String) {
        if (!ownsOriginalAccount()) return
        val uid = originalOwnerUid ?: return
        viewModelScope.launch {
            if (!ownsOriginalAccount()) return@launch
            repository.unbookmarkQuestion(uid, questionId)
        }
    }

    fun bookmark(bookmark: BookmarkedQuestion) {
        if (!ownsOriginalAccount()) return
        val uid = originalOwnerUid ?: return
        viewModelScope.launch {
            if (!ownsOriginalAccount()) return@launch
            repository.bookmarkQuestion(
                userId = uid,
                question = bookmark.toQuestion(),
                examName = bookmark.examName,
                questionNumber = bookmark.questionNumber
            )
        }
    }
}
