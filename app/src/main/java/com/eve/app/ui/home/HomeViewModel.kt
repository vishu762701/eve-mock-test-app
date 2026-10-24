package com.eve.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.data.repository.PinnedExamsRepository
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

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
    private val _targetExamIds = MutableStateFlow<Set<String>>(TargetExamsBottomSheet.getTargetExamIds(com.eve.app.EveApplication.instance))
    private val _feedbackPosts = MutableStateFlow<List<FeedbackPost>>(emptyList())
    private val _selectedCategory = MutableStateFlow(Constants.CATEGORY_ALL)

    private var pinnedObserverJob: Job? = null
    private var feedbackObserverJob: Job? = null
    private var allLoadedExams: List<Exam> = emptyList()
    private var hasLoadedOnce = false
    private var loadJob: Job? = null
    private var lastUserLoaded: String? = null
    private var lastAdminLoaded: Boolean? = null
    private var lastLoadedTime: Long = 0L

    private val cacheFile: java.io.File
        get() = java.io.File(com.eve.app.EveApplication.instance.filesDir, "cached_exams.json")

    private fun loadCachedExams(): List<Exam> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            val json = cacheFile.readText()
            val type = object : com.google.gson.reflect.TypeToken<List<Exam>>() {}.type
            com.google.gson.Gson().fromJson<List<Exam>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCachedExams(exams: List<Exam>) {
        try {
            val json = com.google.gson.Gson().toJson(exams)
            cacheFile.writeText(json)
        } catch (_: Exception) {}
    }

    fun refreshTargetExams() {
        _targetExamIds.value = TargetExamsBottomSheet.getTargetExamIds(com.eve.app.EveApplication.instance)
    }

    fun hasSubExams(examId: String): Boolean = allLoadedExams.any { it.parentExamId == examId }
    fun getSubExams(examId: String): List<Exam> = allLoadedExams.filter { it.parentExamId == examId }.sortedBy { it.examName }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val stackTrace = Log.getStackTraceString(throwable)
        Log.e("EVE_STARTUP", "Uncaught exception in HomeViewModel viewModelScope:\n$stackTrace", throwable)
        Log.e("HomeViewModel", "Uncaught exception in viewModelScope:\n$stackTrace", throwable)
    }

    val state: StateFlow<UiState<HomeUiData>> =
        combine(
            combine(_examState, _selectedCategory, _attemptInfo) { es, cat, att -> Triple(es, cat, att) },
            combine(_pinnedIds, _targetExamIds, _feedbackPosts) { pin, tgt, fp -> Triple(pin, tgt, fp) }
        ) { (examState, selected, attemptInfo), (pinned, targetIds, feedbackPosts) ->
            when (examState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(examState.message)
                is UiState.Success -> UiState.Success(buildUiData(examState.data, selected, attemptInfo.ids, attemptInfo.map, pinned, targetIds, feedbackPosts))
            }
        }.stateIn(viewModelScope + coroutineExceptionHandler, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        val cached = loadCachedExams()
        if (cached.isNotEmpty()) {
            allLoadedExams = cached
            hasLoadedOnce = true
            _examState.value = UiState.Success(cached)
        }
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch(coroutineExceptionHandler) {
            if (!hasLoadedOnce && allLoadedExams.isEmpty()) {
                _examState.value = UiState.Loading
            }
            try {
                val exams = repo.getExams()
                allLoadedExams = exams
                hasLoadedOnce = true
                saveCachedExams(exams)
                _examState.value = UiState.Success(exams)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!hasLoadedOnce) {
                    _examState.value = UiState.Error(e.toUserFriendlyMessage())
                }
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
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

    fun loadForUser(userId: String, isAdmin: Boolean, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && lastUserLoaded == userId && lastAdminLoaded == isAdmin && (now - lastLoadedTime < 10_000L) && hasLoadedOnce) {
            if (pinnedObserverJob?.isActive != true) {
                pinnedObserverJob?.cancel()
                pinnedObserverJob = viewModelScope.launch(coroutineExceptionHandler) {
                    pinnedRepo.observePinnedExamIds(userId).collect { pins ->
                        _pinnedIds.value = pins
                    }
                }
            }
            return
        }
        lastUserLoaded = userId
        lastAdminLoaded = isAdmin
        lastLoadedTime = now

        loadJob?.cancel()
        loadJob = viewModelScope.launch(coroutineExceptionHandler) {
            if (!hasLoadedOnce && allLoadedExams.isEmpty()) {
                _examState.value = UiState.Loading
            }
            try {
                val exams = repo.getExams()
                allLoadedExams = exams
                hasLoadedOnce = true
                saveCachedExams(exams)
                _examState.value = UiState.Success(exams)
                if (isAdmin) {
                    _attemptInfo.value = AttemptInfo()
                } else {
                    val locks = try {
                        repo.getAttemptedExamIds(userId).filter { !isAttemptCleared(it) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // optional: failure is fine
                        emptyList()
                    }
                    val attempts = try {
                        historyRepo.getAttempts(userId).filter { !isAttemptCleared(it.examId) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // optional: failure is fine
                        emptyList()
                    }
                    val ids = (locks + attempts.map { it.examId } + _submittedAttemptedIds).filter { !isAttemptCleared(it) }
                    val map = attempts.associateBy { it.examId }.toMutableMap()
                    _cachedSubmittedAttempts.forEach { (k, v) ->
                        if (!map.containsKey(k) && !isAttemptCleared(k)) {
                            map[k] = v
                        }
                    }
                    _attemptInfo.value = AttemptInfo(ids.toSet(), map)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!hasLoadedOnce) {
                    _examState.value = UiState.Error(e.toUserFriendlyMessage())
                }
            }
        }

        // Real-time listener for pinned exams
        pinnedObserverJob?.cancel()
        pinnedObserverJob = viewModelScope.launch(coroutineExceptionHandler) {
            pinnedRepo.observePinnedExamIds(userId).collect { pins ->
                _pinnedIds.value = pins
            }
        }

        // Real-time listener for published feedback posts
        feedbackObserverJob?.cancel()
        feedbackObserverJob = viewModelScope.launch(coroutineExceptionHandler) {
            feedbackRepo.observeFeedbackPosts().collect { posts ->
                _feedbackPosts.value = posts
            }
        }
    }

    fun togglePin(userId: String, examId: String, currentlyPinned: Boolean) {
        viewModelScope.launch(coroutineExceptionHandler) {
            pinnedRepo.togglePin(userId, examId, currentlyPinned)
        }
    }

    fun deleteFeedbackPost(postId: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            deleteFeedbackPostConfirmed(postId)
        }
    }

    suspend fun deleteFeedbackPostConfirmed(postId: String) {
        feedbackRepo.deleteFeedbackPost(postId).getOrThrow()
        _feedbackPosts.value = _feedbackPosts.value.filterNot { it.id == postId }
    }

    fun selectCategory(category: String) { _selectedCategory.value = category }

    private fun buildUiData(
        all: List<Exam>,
        selected: String,
        attempted: Set<String>,
        attemptsMap: Map<String, com.eve.app.data.model.TestAttempt>,
        pinned: Set<String>,
        targetIds: Set<String>,
        feedbackPosts: List<FeedbackPost>
    ): HomeUiData {
        val mainExams = all.filter { it.isMainExam }
        val categories = HomeAppearance.categories(mainExams.map { it.categoryOrOther })
        val effectiveSelected = if (selected in categories) selected else Constants.CATEGORY_ALL
        val filtered = if (effectiveSelected == Constants.CATEGORY_ALL) mainExams else mainExams.filter { it.categoryOrOther == effectiveSelected }

        val pinnedExams = filtered.filter { it.id in pinned }.sortedBy { it.examName }
        val unpinnedExams = filtered.filter { it.id !in pinned }

        val items = mutableListOf<HomeListItem>()

        val lastExamId = com.eve.app.EveApplication.instance
            .getSharedPreferences("eve_app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("last_exam_id", null)
        val continueExam = if (!lastExamId.isNullOrBlank()) all.find { it.id == lastExamId } else null

        if (effectiveSelected == Constants.CATEGORY_ALL) {
            if (continueExam != null) {
                items.add(HomeListItem.Header("Continue: ${continueExam.examName}"))
                items.add(HomeListItem.ExamRow(continueExam, continueExam.id in attempted, isPinned = continueExam.id in pinned, attempt = attemptsMap[continueExam.id]))
            }

            // Task B: Published feedback posts appear in reverse-chronological order along with other home content
            if (feedbackPosts.isNotEmpty()) {
                val sortedPosts = feedbackPosts.sortedByDescending { it.timestamp }
                items.addAll(sortedPosts.map { HomeListItem.FeedbackPostRow(it) })
            }

            if (pinnedExams.isNotEmpty()) {
                items.add(HomeListItem.Header("Pinned Tests"))
                items.addAll(pinnedExams.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = true, attempt = attemptsMap[it.id]) })
            }

            val targetExams = unpinnedExams.filter { it.id in targetIds }.sortedBy { it.examName }
            if (targetExams.isNotEmpty()) {
                items.add(HomeListItem.Header("My Target Exams"))
                items.addAll(targetExams.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = false, attempt = attemptsMap[it.id]) })
            }

            val remainingExams = unpinnedExams.filter { it.id !in targetIds }
            remainingExams.groupBy { it.categoryOrOther }.toSortedMap().forEach { (category, exams) ->
                items.add(HomeListItem.Header(category))
                items.addAll(exams.sortedBy { it.examName }.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = false, attempt = attemptsMap[it.id]) })
            }
        } else {
            if (pinnedExams.isNotEmpty()) {
                items.addAll(pinnedExams.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = true, attempt = attemptsMap[it.id]) })
            }
            val targetExams = unpinnedExams.filter { it.id in targetIds }.sortedBy { it.examName }
            items.addAll(targetExams.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = false, attempt = attemptsMap[it.id]) })
            val remainingExams = unpinnedExams.filter { it.id !in targetIds }
            items.addAll(remainingExams.sortedBy { it.examName }.map { HomeListItem.ExamRow(it, it.id in attempted, isPinned = false, attempt = attemptsMap[it.id]) })
        }

        return HomeUiData(categories, effectiveSelected, items)
    }
}
