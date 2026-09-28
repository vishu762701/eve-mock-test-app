package com.eve.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.data.repository.PinnedExamsRepository
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repo = ExamRepository()
    private val pinnedRepo = PinnedExamsRepository()
    private val feedbackRepo = FeedbackRepository()
    private val historyRepo = com.eve.app.data.repository.HistoryRepository()

    private data class AttemptInfo(
        val ids: Set<String> = emptySet(),
        val map: Map<String, com.eve.app.data.model.TestAttempt> = emptyMap()
    )

    private val _examState = MutableStateFlow<UiState<List<Exam>>>(UiState.Loading)
    private val _attemptInfo = MutableStateFlow(AttemptInfo())
    private val _pinnedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _feedbackPosts = MutableStateFlow<List<FeedbackPost>>(emptyList())
    private val _selectedCategory = MutableStateFlow(Constants.CATEGORY_ALL)

    private var pinnedObserverJob: Job? = null
    private var feedbackObserverJob: Job? = null

    val state: StateFlow<UiState<HomeUiData>> =
        combine(_examState, _selectedCategory, _attemptInfo, _pinnedIds, _feedbackPosts) { examState, selected, attemptInfo, pinned, feedbackPosts ->
            when (examState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(examState.message)
                is UiState.Success -> UiState.Success(buildUiData(examState.data, selected, attemptInfo.ids, attemptInfo.map, pinned, feedbackPosts))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch {
            _examState.value = UiState.Loading
            _examState.value = try { UiState.Success(repo.getExams()) }
            catch (e: Exception) { UiState.Error(e.message ?: "Failed to load exams") }
        }
        viewModelScope.launch {
            _feedbackPosts.value = feedbackRepo.getFeedbackPosts()
        }
    }

    companion object {
        private val _clearedAttemptedIds = mutableSetOf<String>()
        private val _submittedAttemptedIds = mutableSetOf<String>()
        private val _cachedSubmittedAttempts = mutableMapOf<String, com.eve.app.data.model.TestAttempt>()

        fun markAttemptCleared(examId: String) {
            _clearedAttemptedIds.add(examId)
            _submittedAttemptedIds.remove(examId)
            _cachedSubmittedAttempts.remove(examId)
        }
        fun isAttemptCleared(examId: String): Boolean = _clearedAttemptedIds.contains(examId)
        fun clearAttemptCleared(examId: String) {
            _clearedAttemptedIds.remove(examId)
        }

        fun markAttemptSubmitted(examId: String, attempt: com.eve.app.data.model.TestAttempt? = null) {
            _clearedAttemptedIds.remove(examId)
            _submittedAttemptedIds.add(examId)
            if (attempt != null) {
                _cachedSubmittedAttempts[examId] = attempt
            }
        }
        fun isAttemptSubmitted(examId: String): Boolean = _submittedAttemptedIds.contains(examId)
        fun getCachedAttempt(examId: String): com.eve.app.data.model.TestAttempt? = _cachedSubmittedAttempts[examId]
    }

    fun dropAttemptedId(examId: String) {
        markAttemptCleared(examId)
        val current = _attemptInfo.value
        _attemptInfo.value = AttemptInfo(
            ids = current.ids - examId,
            map = current.map - examId
        )
    }

    fun loadForUser(userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            _examState.value = UiState.Loading
            try {
                val exams = repo.getExams()
                _examState.value = UiState.Success(exams)
                if (isAdmin) {
                    _attemptInfo.value = AttemptInfo()
                } else {
                    val locks = repo.getAttemptedExamIds(userId).filter { !isAttemptCleared(it) }
                    val attempts = historyRepo.getAttempts(userId).filter { !isAttemptCleared(it.examId) }
                    val ids = (locks + attempts.map { it.examId } + _submittedAttemptedIds).filter { !isAttemptCleared(it) }
                    val map = attempts.associateBy { it.examId }.toMutableMap()
                    _cachedSubmittedAttempts.forEach { (k, v) ->
                        if (!map.containsKey(k) && !isAttemptCleared(k)) {
                            map[k] = v
                        }
                    }
                    _attemptInfo.value = AttemptInfo(ids.toSet(), map)
                }
            } catch (e: Exception) {
                _examState.value = UiState.Error(e.message ?: "Failed to load exams")
            }
        }

        // Real-time listener for pinned exams
        pinnedObserverJob?.cancel()
        pinnedObserverJob = viewModelScope.launch {
            pinnedRepo.observePinnedExamIds(userId).collect { pins ->
                _pinnedIds.value = pins
            }
        }

        // Real-time listener for published feedback posts
        feedbackObserverJob?.cancel()
        feedbackObserverJob = viewModelScope.launch {
            feedbackRepo.observeFeedbackPosts().collect { posts ->
                _feedbackPosts.value = posts
            }
        }
    }

    fun togglePin(userId: String, examId: String, currentlyPinned: Boolean) {
        viewModelScope.launch {
            pinnedRepo.togglePin(userId, examId, currentlyPinned)
        }
    }

    fun deleteFeedbackPost(postId: String) {
        viewModelScope.launch {
            feedbackRepo.deleteFeedbackPost(postId)
        }
    }

    fun selectCategory(category: String) { _selectedCategory.value = category }

    private fun buildUiData(
        all: List<Exam>,
        selected: String,
        attempted: Set<String>,
        attemptsMap: Map<String, com.eve.app.data.model.TestAttempt>,
        pinned: Set<String>,
        feedbackPosts: List<FeedbackPost>
    ): HomeUiData {
        val categories = listOf(Constants.CATEGORY_ALL) + all.map { it.categoryOrOther }.distinct().sorted()
        val effectiveSelected = if (selected in categories) selected else Constants.CATEGORY_ALL
        val filtered = if (effectiveSelected == Constants.CATEGORY_ALL) all else all.filter { it.categoryOrOther == effectiveSelected }

        val pinnedExams = filtered.filter { it.id in pinned }.sortedBy { it.examName }
        val unpinnedExams = filtered.filter { it.id !in pinned }

        val items = mutableListOf<HomeListItem>()

        if (effectiveSelected == Constants.CATEGORY_ALL) {
            // Task B: Published feedback posts appear in reverse-chronological order along with other home content
            if (feedbackPosts.isNotEmpty()) {
                val sortedPosts = feedbackPosts.sortedByDescending { it.timestamp }
                items.addAll(sortedPosts.map { HomeListItem.FeedbackPostRow(it) })
            }

            if (pinnedExams.isNotEmpty()) {
                items.add(HomeListItem.Header("Pinned Tests"))
                items.addAll(pinnedExams.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = true, attempt = attemptsMap[it.id]) })
            }

            unpinnedExams.groupBy { it.categoryOrOther }.toSortedMap().forEach { (category, exams) ->
                items.add(HomeListItem.Header(category))
                items.addAll(exams.sortedBy { it.examName }.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = false, attempt = attemptsMap[it.id]) })
            }
        } else {
            if (pinnedExams.isNotEmpty()) {
                items.addAll(pinnedExams.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = true, attempt = attemptsMap[it.id]) })
            }
            items.addAll(unpinnedExams.sortedBy { it.examName }.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = false, attempt = attemptsMap[it.id]) })
        }

        return HomeUiData(categories, effectiveSelected, items)
    }
}
