package com.eve.app.ui.test

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.Question
import com.eve.app.data.repository.BookmarkRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.util.AttemptKey
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.eve.app.data.remote.toUserFriendlyMessage
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestViewModel : ViewModel() {

    private val repo = ExamRepository()
    private val historyRepo = HistoryRepository()
    private val bookmarkRepo = BookmarkRepository()

    private val _questions = MutableStateFlow<UiState<List<Question>>>(UiState.Loading)
    val questions: StateFlow<UiState<List<Question>>> = _questions.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(-1L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    /** Timer 0 par pahunchte hi true -> auto submit */
    private val _timeUp = MutableStateFlow(false)
    val timeUp: StateFlow<Boolean> = _timeUp.asStateFlow()

    private val _alreadyAttempted = MutableStateFlow(false)
    val alreadyAttempted: StateFlow<Boolean> = _alreadyAttempted.asStateFlow()

    // position -> "A".."D". Swipe karne par selection yahin se wapas milta hai.
    private val answers = mutableMapOf<Int, String>()

    // position -> time taken in seconds
    private val timeTaken = mutableMapOf<Int, Long>()

    // position -> bookmarked? "Review ke liye flag" state, swipe karne par bhi yaad rehta hai.
    private val bookmarks = mutableMapOf<Int, Boolean>()

    private val visited = mutableSetOf<Int>()
    private val marked = mutableSetOf<Int>()

    private val _bookmarksSynced = MutableStateFlow(false)
    val bookmarksSynced: StateFlow<Boolean> = _bookmarksSynced.asStateFlow()

    private var cachedBookmarkedIds: Set<String> = emptySet()
    private var currentExamName: String = ""
    private var started = false
    private var timerJob: Job? = null

    private var initialTimeLimitSeconds: Long = 0L
    private var initialStartedAt: Long = 0L
    var clientAttemptId: String = java.util.UUID.randomUUID().toString()
        private set
    var currentUserId: String = ""
        private set
    var currentExamId: String = ""
        private set
    var restoredQuestionIndex: Int = 0
        private set

    fun start(
        examId: String,
        timeLimitMinutes: Int,
        topic: String = "",
        pyqYear: Int = 0,
        pyqPaper: String = "",
        isAdmin: Boolean = false,
        examName: String = "",
        fromBookmark: Boolean = false,
        testId: String = ""
    ) {
        if (started) return
        started = true
        _timeUp.value = false
        currentExamName = examName
        val effectiveExamId = if (testId.isNotBlank() && !examId.contains(AttemptKey.SEP)) {
            AttemptKey.forTest(examId, testId)
        } else {
            examId
        }
        currentExamId = effectiveExamId

        val user = FirebaseAuth.getInstance().currentUser
        if (currentUserId.isNotBlank() && currentUserId != user?.uid) {
            started = false
            _questions.value = UiState.Error("Sign in with the account that started this test")
            return
        }
        currentUserId = user?.uid.orEmpty()
        if (user != null) {
            viewModelScope.launch {
                bookmarkRepo.observeBookmarkIds(user.uid).collect { ids ->
                    cachedBookmarkedIds = ids
                    val curList = (_questions.value as? UiState.Success)?.data
                    if (curList != null) {
                        curList.forEachIndexed { idx, q ->
                            val qId = bookmarkRepo.getStableId(q, idx + 1)
                            bookmarks[idx] = ids.contains(qId)
                        }
                        _bookmarksSynced.value = true
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                val isStandardMock = topic.isBlank() && pyqYear == 0 && !fromBookmark
                val canAttempt = com.eve.app.util.AttemptLimitManager.canAttempt(com.eve.app.EveApplication.instance, effectiveExamId)
                if (!isAdmin && user != null && effectiveExamId.isNotBlank() && isStandardMock && !canAttempt) {
                    _alreadyAttempted.value = true
                    started = false
                    return@launch
                }

                val sessionStore = com.eve.app.data.local.TestSessionStore(com.eve.app.EveApplication.instance)
                var existingSession = sessionStore.getSession(effectiveExamId)
                if (existingSession != null) {
                    restoredQuestionIndex = existingSession.currentQuestionIndex
                    if (existingSession.clientAttemptId.isNotBlank()) clientAttemptId = existingSession.clientAttemptId
                }

                var remainingSec = timeLimitMinutes * 60L
                var serverQuestions: List<Question>? = null

                if (isStandardMock && user != null && effectiveExamId.isNotBlank()) {
                    try {
                        val startRes = com.eve.app.data.remote.ApiClient.api.startAttempt(mapOf("examId" to effectiveExamId, "resumeAttemptId" to existingSession?.clientAttemptId.orEmpty()))
                        if (startRes.success && startRes.data != null) {
                            val d = startRes.data
                            if (d.alreadySubmitted) {
                                sessionStore.clearSession(effectiveExamId, currentUserId)
                                clientAttemptId = java.util.UUID.randomUUID().toString()
                                answers.clear(); timeTaken.clear(); visited.clear(); marked.clear()
                                started = false
                                _questions.value = UiState.Error("This saved test was already submitted. Open History for the confirmed result, or Retry to start a new attempt.")
                                return@launch
                            }
                            d.clientAttemptId?.takeIf { it.isNotBlank() }?.let { clientAttemptId = it }
                            initialStartedAt = d.startedAt
                            initialTimeLimitSeconds = d.timeLimitSeconds
                            if (d.remainingSeconds != null && d.remainingSeconds >= 0) {
                                remainingSec = d.remainingSeconds
                            } else if (d.timeLimitSeconds > 0) {
                                val elapsed = (d.serverNow - d.startedAt) / 1000
                                remainingSec = (d.timeLimitSeconds - elapsed).coerceAtLeast(0)
                            }
                            if (!d.questions.isNullOrEmpty()) {
                                serverQuestions = d.questions
                            }
                        }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 409 && !isAdmin) {
                            val canStillAttempt = com.eve.app.util.AttemptLimitManager.canAttempt(com.eve.app.EveApplication.instance, examId)
                            if (!canStillAttempt) {
                                _alreadyAttempted.value = true
                                started = false
                                return@launch
                            }
                        }
                        if (e.code() == 401 || e.code() == 403 || e.code() == 404 || e.code() == 409) {
                            started = false
                            _questions.value = UiState.Error(e.toUserFriendlyMessage())
                            return@launch
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (existingSession != null && existingSession.elapsedSeconds > 0) {
                            remainingSec = (timeLimitMinutes * 60L - existingSession.elapsedSeconds).coerceAtLeast(0)
                        }
                    }
                } else if (existingSession != null && existingSession.elapsedSeconds > 0) {
                    remainingSec = (timeLimitMinutes * 60L - existingSession.elapsedSeconds).coerceAtLeast(0)
                }

                val list = serverQuestions ?: when {
                    // Phase 19: PYQ paper — original order preserve (shuffle nahi), taaki
                    // admin jaisa upload kiya waisa paper feel rahe.
                    pyqYear > 0 -> repo.getPyqQuestions(examId, pyqYear, pyqPaper)
                    topic.isNotBlank() -> repo.getQuestionsForTopic(examId, topic).shuffled().take(10)
                    AttemptKey.generatedTestId(effectiveExamId) != null -> {
                        val genTestId = AttemptKey.generatedTestId(effectiveExamId).orEmpty()
                        val sourceExamId = AttemptKey.sourceExamId(effectiveExamId)
                        currentExamId = effectiveExamId
                        val test = if (genTestId.isNotBlank()) repo.getGeneratedTest(genTestId) else null
                        if (test != null && test.isLive && test.questions.isNotEmpty()) {
                            test.questions.mapIndexed { idx, gq ->
                                Question(
                                    id = AttemptKey.canonicalQuestionId(test.id, idx),
                                    examId = sourceExamId,
                                    questionText = gq.questionText,
                                    optionA = gq.optionA,
                                    optionB = gq.optionB,
                                    optionC = gq.optionC,
                                    optionD = gq.optionD,
                                    correctAnswer = gq.correctAnswer,
                                    explanation = gq.explanation
                                )
                            }
                        } else {
                            emptyList()
                        }
                    }
                    testId.isNotBlank() -> {
                        val test = repo.getGeneratedTest(testId)
                        if (test != null && test.isLive && test.questions.isNotEmpty()) {
                            currentExamId = AttemptKey.forTest(examId, test.id)
                            test.questions.mapIndexed { idx, gq ->
                                Question(
                                    id = AttemptKey.canonicalQuestionId(test.id, idx),
                                    examId = examId,
                                    questionText = gq.questionText,
                                    optionA = gq.optionA,
                                    optionB = gq.optionB,
                                    optionC = gq.optionC,
                                    optionD = gq.optionD,
                                    correctAnswer = gq.correctAnswer,
                                    explanation = gq.explanation
                                )
                            }
                        } else {
                            emptyList()
                        }
                    }
                    else -> {
                        val mockList = repo.getMockQuestions(examId)
                        if (mockList.isNotEmpty()) {
                            mockList.shuffled()
                        } else {
                            val liveTests = repo.getLiveGeneratedTests(examId)
                            val latestLiveSummary = liveTests.firstOrNull()
                            val latestLive = if (latestLiveSummary != null) repo.getGeneratedTest(latestLiveSummary.id) else null
                            if (latestLive != null && latestLive.questions.isNotEmpty()) {
                                currentExamId = AttemptKey.forTest(examId, latestLive.id)
                                latestLive.questions.mapIndexed { idx, gq ->
                                    Question(
                                        id = AttemptKey.canonicalQuestionId(latestLive.id, idx),
                                        examId = examId,
                                        questionText = gq.questionText,
                                        optionA = gq.optionA,
                                        optionB = gq.optionB,
                                        optionC = gq.optionC,
                                        optionD = gq.optionD,
                                        correctAnswer = gq.correctAnswer,
                                        explanation = gq.explanation
                                    )
                                }
                            } else {
                                emptyList()
                            }
                        }
                    }
                }

                if (list.isEmpty()) {
                    started = false
                    _questions.value = UiState.Error("No questions found for this test. Please try again later.")
                    return@launch
                }

                if (isStandardMock && user != null && currentExamId != effectiveExamId) {
                    // A plain exam can resolve to its latest generated test; start that exact key.
                    existingSession = sessionStore.getSession(currentExamId)
                    val startResponse = com.eve.app.data.remote.ApiClient.api.startAttempt(mapOf("examId" to currentExamId, "resumeAttemptId" to existingSession?.clientAttemptId.orEmpty()))
                    val session = startResponse.data
                    check(startResponse.success && session != null) { startResponse.error ?: "Could not start this test" }
                    if (session.alreadySubmitted) {
                        sessionStore.clearSession(currentExamId, currentUserId)
                        clientAttemptId = java.util.UUID.randomUUID().toString()
                        answers.clear(); timeTaken.clear(); visited.clear(); marked.clear()
                        throw IllegalStateException("This saved test was already submitted. Open History for the confirmed result, or Retry to start a new attempt.")
                    }
                    initialStartedAt = session.startedAt
                    initialTimeLimitSeconds = session.timeLimitSeconds
                    remainingSec = session.remainingSeconds ?: session.timeLimitSeconds
                    existingSession = sessionStore.getSession(currentExamId)
                    session.clientAttemptId?.takeIf { it.isNotBlank() }?.let { clientAttemptId = it }
                }
                val restoredSession = existingSession
                if (restoredSession != null) {
                    list.forEachIndexed { idx, q ->
                        val sel = restoredSession.answers[q.id]
                        if (!sel.isNullOrEmpty()) answers[idx] = sel
                        val time = restoredSession.questionTimes[q.id]
                        if (time != null && time > 0) timeTaken[idx] = time
                        if (restoredSession.visitedQuestions.contains(q.id)) visited.add(idx)
                        if (restoredSession.markedQuestions.contains(q.id)) marked.add(idx)
                    }
                }

                list.forEachIndexed { idx, q ->
                    val qId = bookmarkRepo.getStableId(q, idx + 1)
                    bookmarks[idx] = cachedBookmarkedIds.contains(qId)
                }
                _questions.value = UiState.Success(list)
                if (cachedBookmarkedIds.isNotEmpty()) {
                    _bookmarksSynced.value = true
                }
                startTimer(remainingSec)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                started = false
                _questions.value = UiState.Error(e.toUserFriendlyMessage())
            }
        }
    }

    fun retry(
        examId: String,
        timeLimitMinutes: Int,
        topic: String = "",
        pyqYear: Int = 0,
        pyqPaper: String = "",
        isAdmin: Boolean = false,
        examName: String = "",
        fromBookmark: Boolean = false,
        testId: String = ""
    ) {
        _questions.value = UiState.Loading
        _alreadyAttempted.value = false
        started = false
        start(examId, timeLimitMinutes, topic, pyqYear, pyqPaper, isAdmin, examName, fromBookmark, testId)
    }

    private fun startTimer(totalSeconds: Long) {
        val endAt = SystemClock.elapsedRealtime() + totalSeconds * 1000
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val leftMs = endAt - SystemClock.elapsedRealtime()
                val left = ((leftMs + 999) / 1000).coerceAtLeast(0)
                _remainingSeconds.value = left
                if (left <= 0) {
                    _timeUp.value = true
                    break
                }
                delay(500)
            }
        }
    }

    fun resumeTimerAfterFailure() {
        if (_remainingSeconds.value > 0) startTimer(_remainingSeconds.value)
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun getAnswer(position: Int): String = answers[position] ?: ""

    fun setAnswer(position: Int, letter: String) {
        if (letter.isEmpty()) answers.remove(position) else answers[position] = letter
    }

    fun recordQuestionTime(position: Int, seconds: Long) {
        val current = timeTaken[position] ?: 0L
        timeTaken[position] = current + seconds
    }

    fun addQuestionSecond(position: Int) {
        val current = timeTaken[position] ?: 0L
        timeTaken[position] = current + 1L
    }

    fun getQuestionTime(position: Int): Long = timeTaken[position] ?: 0L

    fun isBookmarked(position: Int): Boolean = bookmarks[position] ?: false

    fun toggleBookmark(position: Int) {
        val current = isBookmarked(position)
        bookmarks[position] = !current
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val list = (_questions.value as? UiState.Success)?.data ?: return
        val q = list.getOrNull(position) ?: return
        viewModelScope.launch {
            bookmarkRepo.toggleBookmark(
                userId = user.uid,
                question = q,
                examName = currentExamName,
                questionNumber = position + 1,
                currentlyBookmarked = current
            )
        }
    }

    fun markVisited(position: Int) {
        visited.add(position)
    }

    fun isVisited(position: Int): Boolean = visited.contains(position)

    fun toggleMark(position: Int) {
        if (marked.contains(position)) marked.remove(position) else marked.add(position)
    }

    fun isMarked(position: Int): Boolean = marked.contains(position)

    fun getAnsweredIndices(): Set<Int> = answers.filterValues { it.isNotEmpty() }.keys.toSet()
    fun getVisitedIndices(): Set<Int> = visited.toSet()
    fun getMarkedIndices(): Set<Int> = marked.toSet()

    fun saveCurrentSession(examId: String, currentQuestionIndex: Int = 0) {
        if (currentUserId.isBlank()) return
        val list = (_questions.value as? UiState.Success)?.data ?: return
        val ansMap = mutableMapOf<String, String>()
        val timeMap = mutableMapOf<String, Long>()
        val visSet = mutableSetOf<String>()
        val markSet = mutableSetOf<String>()
        list.forEachIndexed { idx, q ->
            answers[idx]?.let { if (it.isNotEmpty()) ansMap[q.id] = it }
            timeTaken[idx]?.let { if (it > 0) timeMap[q.id] = it }
            if (visited.contains(idx)) visSet.add(q.id)
            if (marked.contains(idx)) markSet.add(q.id)
        }
        val session = com.eve.app.data.local.TestSession(
            attemptKey = currentExamId.ifBlank { examId },
            clientAttemptId = clientAttemptId,
            userId = currentUserId,
            answers = ansMap,
            questionTimes = timeMap,
            visitedQuestions = visSet,
            markedQuestions = markSet,
            elapsedSeconds = timeTaken.values.sum(),
            timeLimitSeconds = initialTimeLimitSeconds,
            startedAt = initialStartedAt,
            currentQuestionIndex = currentQuestionIndex,
            lastSavedAt = System.currentTimeMillis()
        )
        com.eve.app.data.local.TestSessionStore(com.eve.app.EveApplication.instance).saveSession(session)
    }

    suspend fun pauseSession(examId: String, currentQuestionIndex: Int) {
        saveCurrentSession(examId, currentQuestionIndex)
        if (currentUserId != FirebaseAuth.getInstance().currentUser?.uid) return
        try {
            kotlinx.coroutines.withTimeoutOrNull(5_000) {
                com.eve.app.data.remote.ApiClient.api.pauseAttempt(mapOf("examId" to currentExamId.ifBlank { examId }))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Answers remain local; an offline client cannot confirm a server timer pause.
        }
    }

    fun clearSession(examId: String) {
        com.eve.app.data.local.TestSessionStore(com.eve.app.EveApplication.instance).clearSession(examId, currentUserId)
    }

    fun buildAnswerItems(): ArrayList<AnswerItem> {
        val list = (questions.value as? UiState.Success)?.data ?: emptyList()
        val items = ArrayList<AnswerItem>()
        list.forEachIndexed { index, q ->
            val selected = getAnswer(index)
            items.add(
                AnswerItem(
                    questionId = q.id,
                    number = index + 1,
                    questionText = q.questionText,
                    questionTextHi = q.questionTextHi,
                    selected = selected,
                    selectedText = if (selected.isEmpty()) "" else q.optionText(selected),
                    selectedTextHi = if (selected.isEmpty()) "" else q.optionTextHi(selected),
                    correct = "",
                    correctText = "",
                    correctTextHi = "",
                    explanation = "",
                    explanationHi = "",
                    isBookmarked = isBookmarked(index),
                    topic = q.topic,
                    timeTakenSeconds = getQuestionTime(index),
                    optionA = q.optionA,
                    optionB = q.optionB,
                    optionC = q.optionC,
                    optionD = q.optionD,
                    optionAHi = q.optionAHi,
                    optionBHi = q.optionBHi,
                    optionCHi = q.optionCHi,
                    optionDHi = q.optionDHi
                )
            )
        }
        return items
    }

    /**
     * Test History (Phase 10): submit hote hi attempt server se save karo, logged in ho tabhi.
     * Phase 23: userId ab bheja hi nahi jaata — submitAttempt Cloud Function apne auth
     * context (request.auth.uid) se hi decide karta hai, isliye client isko spoof nahi
     * kar sakta.
     */
    fun saveAttempt(examId: String, examName: String, category: String, items: List<AnswerItem>) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val displayName = user.displayName?.ifBlank { null } ?: "Student"
        historyRepo.saveAttempt(examId, examName, category, displayName, items)
    }

    suspend fun saveAttemptSync(examId: String, examName: String, category: String, items: List<AnswerItem>): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val displayName = user.displayName?.ifBlank { null } ?: "Student"
        return historyRepo.submitAttemptSync(examId, examName, category, displayName, items)
    }

    fun writeToBundle(bundle: android.os.Bundle, currentIndex: Int = 0) {
        val answersKeys = answers.keys.toIntArray()
        val answersVals = answersKeys.map { answers[it] ?: "" }.toTypedArray()
        bundle.putIntArray("key_answers_keys", answersKeys)
        bundle.putStringArray("key_answers_vals", answersVals)

        val timeKeys = timeTaken.keys.toIntArray()
        val timeVals = timeKeys.map { timeTaken[it] ?: 0L }.toLongArray()
        bundle.putIntArray("key_time_keys", timeKeys)
        bundle.putLongArray("key_time_vals", timeVals)

        val bookmarkKeys = bookmarks.keys.toIntArray()
        val bookmarkVals = bookmarkKeys.map { bookmarks[it] ?: false }.toBooleanArray()
        bundle.putIntArray("key_bm_keys", bookmarkKeys)
        bundle.putBooleanArray("key_bm_vals", bookmarkVals)

        bundle.putIntArray("key_visited", visited.toIntArray())
        bundle.putIntArray("key_marked", marked.toIntArray())
        bundle.putLong("key_remaining_seconds", _remainingSeconds.value)
        bundle.putString("key_client_attempt_id", clientAttemptId)
        bundle.putString("key_attempt_owner", currentUserId)
        bundle.putInt("key_current_q_index", currentIndex)
    }

    fun restoreFromBundle(bundle: android.os.Bundle) {
        if (!com.eve.app.util.SubmissionRetryPolicy.ownsSubmission(bundle.getString("key_attempt_owner"), FirebaseAuth.getInstance().currentUser?.uid)) return
        val answersKeys = bundle.getIntArray("key_answers_keys")
        val answersVals = bundle.getStringArray("key_answers_vals")
        if (answersKeys != null && answersVals != null && answersKeys.size == answersVals.size) {
            for (i in answersKeys.indices) {
                answers[answersKeys[i]] = answersVals[i]
            }
        }

        val timeKeys = bundle.getIntArray("key_time_keys")
        val timeVals = bundle.getLongArray("key_time_vals")
        if (timeKeys != null && timeVals != null && timeKeys.size == timeVals.size) {
            for (i in timeKeys.indices) {
                timeTaken[timeKeys[i]] = timeVals[i]
            }
        }

        val bmKeys = bundle.getIntArray("key_bm_keys")
        val bmVals = bundle.getBooleanArray("key_bm_vals")
        if (bmKeys != null && bmVals != null && bmKeys.size == bmVals.size) {
            for (i in bmKeys.indices) {
                bookmarks[bmKeys[i]] = bmVals[i]
            }
        }

        bundle.getIntArray("key_visited")?.forEach { visited.add(it) }
        bundle.getIntArray("key_marked")?.forEach { marked.add(it) }

        val rem = bundle.getLong("key_remaining_seconds", -1L)
        if (rem > 0) {
            _remainingSeconds.value = rem
        }
        bundle.getString("key_client_attempt_id")?.let {
            if (it.isNotBlank()) clientAttemptId = it
        }
        restoredQuestionIndex = bundle.getInt("key_current_q_index", 0)
    }
}
