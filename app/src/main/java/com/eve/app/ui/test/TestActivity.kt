package com.eve.app.ui.test

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.QuestionStatsRepository
import com.eve.app.databinding.ActivityTestBinding
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import com.eve.app.ui.common.ReportQuestionDialog
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.util.AnalyticsHelper
import com.eve.app.util.Constants
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.UiState
import com.eve.app.util.isHardcodedAdmin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestActivity : EveBaseActivity() {

    private lateinit var binding: ActivityTestBinding
    private val viewModel: TestViewModel by viewModels()

    private lateinit var examId: String
    private var timeLimit = 30
    private var negativeMarking = Constants.NEGATIVE_MARK
    private var examName = ""
    private var examCategory = ""
    private var topic = ""
    private var pyqYear = 0
    private var pyqPaper = ""
    private var totalQuestions = 0
    private var submitted = false
    private var adminRepository = AdminRepository()
    private var isAdminUser = false
    private var hasEmptyPlayed = false
    private var fromBookmark = false
    private var initialQuestionId: String? = null
    private var initialNavDone = false

    private lateinit var paletteAdapter: QuestionPaletteAdapter
    private var currentQuestionPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        currentQuestionPosition = savedInstanceState?.getInt("saved_question_position", 0) ?: 0
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.shimmerSkeletonTest.skeletonType = com.eve.app.ui.common.ShimmerSkeletonView.TYPE_QUESTION

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID) ?: ""
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME) ?: "Test"
        examCategory = intent.getStringExtra(Constants.EXTRA_EXAM_CATEGORY) ?: ""
        timeLimit = intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 30)
        negativeMarking = intent.getDoubleExtra(Constants.EXTRA_NEGATIVE_MARKING, Constants.NEGATIVE_MARK)
        topic = intent.getStringExtra(Constants.EXTRA_TOPIC).orEmpty()
        pyqYear = intent.getIntExtra(Constants.EXTRA_PYQ_YEAR, 0)
        pyqPaper = intent.getStringExtra(Constants.EXTRA_PYQ_PAPER).orEmpty()
        fromBookmark = intent.getBooleanExtra(Constants.EXTRA_FROM_BOOKMARK, false)
        initialQuestionId = intent.getStringExtra(Constants.EXTRA_INITIAL_QUESTION_ID)

        val testId = intent.getStringExtra(Constants.EXTRA_TEST_ID).orEmpty()

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        lifecycleScope.launch {
            val isAdmin = currentUser?.let { user ->
                isHardcodedAdmin(user.email) || adminRepository.isAdmin(user.email)
            } ?: false
            isAdminUser = isAdmin

            if (!isAdminUser && topic.isBlank() && pyqYear == 0 && examId.isNotBlank()) {
                val canAttempt = com.eve.app.util.AttemptLimitManager.canAttempt(this@TestActivity, examId)
                if (!canAttempt) {
                    AlertDialog.Builder(this@TestActivity)
                        .setTitle("Attempt Limit Reached")
                        .setMessage("You have reached the maximum limit of 3 attempts for this test. Normal users can attempt each test at most 3 times.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                    return@launch
                }
            }

            viewModel.start(examId, timeLimit, topic, pyqYear, pyqPaper, isAdminUser, examName, fromBookmark, testId)
        }
        // Phase 15: exam start event — is exam ko kitni baar attempt kiya gaya, yeh track karta hai
        AnalyticsHelper.logExamStart(this, examId, examName, examCategory)

        paletteAdapter = QuestionPaletteAdapter { pos ->
            binding.viewPager.setCurrentItem(pos, true)
        }
        binding.rvQuestionPalette.adapter = paletteAdapter

        binding.btnPrev.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
        binding.btnNext.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        }
        binding.btnSubmit.setOnClickListener { confirmSubmit() }
        binding.btnRetry.setOnClickListener { viewModel.retry(examId, timeLimit, topic, pyqYear, pyqPaper, isAdminUser, examName, fromBookmark) }

        LanguageManager.setupToggleButton(this, binding.btnLanguage) {
            binding.viewPager.adapter?.notifyDataSetChanged()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentQuestionPosition = position
                viewModel.markVisited(position)
                updateNav(position)
                updatePalette(position)
                (binding.viewPager.adapter as? QuestionAdapter)?.notifyItemChanged(
                    position,
                    QuestionAdapter.PAYLOAD_TIMER
                )
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var sessionSaveTicks = 0
                while (!submitted) {
                    delay(1000)
                    if (!submitted && totalQuestions > 0) {
                        val currentPos = binding.viewPager.currentItem
                        viewModel.addQuestionSecond(currentPos)
                        (binding.viewPager.adapter as? QuestionAdapter)?.notifyItemChanged(
                            currentPos,
                            QuestionAdapter.PAYLOAD_TIMER
                        )
                        sessionSaveTicks++
                        if (sessionSaveTicks >= 5) {
                            sessionSaveTicks = 0
                            viewModel.saveCurrentSession(examId)
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@TestActivity)
                    .setTitle("Test chhodna hai?")
                    .setMessage("Exit karne par aapka progress lost ho jayega.")
                    .setPositiveButton("Exit") { _, _ ->
                        viewModel.stopTimer()
                        finish()
                    }
                    .setNegativeButton("Continue", null)
                    .show()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.questions.collect { renderQuestions(it) } }
                launch { viewModel.remainingSeconds.collect { renderTimer(it) } }
                launch { viewModel.timeUp.collect { if (it) submit() } }
                launch {
                    viewModel.alreadyAttempted.collect { blocked ->
                        if (blocked && !isFinishing) {
                            lifecycleScope.launch {
                                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                val attempts = if (user != null) com.eve.app.data.repository.HistoryRepository().getAttempts(user.uid).filter { it.examId == examId } else emptyList()
                                val target = attempts.maxByOrNull { it.timestamp } ?: com.eve.app.ui.home.HomeViewModel.getCachedAttempt(examId)
                                if (target != null && target.answers.isNotEmpty()) {
                                    ResultDataHolder.setAnswers(target.answers)
                                    val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                    startActivity(
                                        Intent(this@TestActivity, ResultActivity::class.java)
                                            .putExtra(Constants.EXTRA_EXAM_ID, examId)
                                            .putExtra(Constants.EXTRA_EXAM_NAME, examName)
                                            .putExtra(Constants.EXTRA_ATTEMPT_DATE, dateFormat.format(java.util.Date(target.timestamp)))
                                            .putExtra(Constants.EXTRA_FROM_HISTORY, true)
                                            .putExtra(Constants.EXTRA_CAN_REATTEMPT, true)
                                            .putExtra(Constants.EXTRA_TIME_LIMIT, timeLimit)
                                            .putExtra(Constants.EXTRA_NEGATIVE_MARKING, negativeMarking)
                                            .putExtra(Constants.EXTRA_EXAM_CATEGORY, examCategory)
                                    )
                                    finish()
                                } else {
                                    com.eve.app.ui.common.CompletedExamBottomSheet.show(
                                        activity = this@TestActivity,
                                        examId = examId,
                                        examName = examName,
                                        attempt = target,
                                        onReattemptConfirmed = {
                                            val restartIntent = intent
                                            finish()
                                            startActivity(restartIntent)
                                        },
                                        onDismiss = {
                                            if (!isFinishing) {
                                                finish()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.bookmarksSynced.collect { synced ->
                        if (synced) {
                            binding.viewPager.adapter?.notifyDataSetChanged()
                        }
                    }
                }
                launch {
                    NetworkUtil.observe(this@TestActivity).collect { online ->
                        binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun renderQuestions(state: UiState<List<Question>>) {
        when (state) {
            is UiState.Loading -> {
                binding.shimmerSkeletonTest.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.GONE
            }
            is UiState.Error -> {
                binding.shimmerSkeletonTest.visibility = View.GONE
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setAnimation(com.eve.app.R.raw.error_404)
                binding.ivMessageIcon.playAnimation()
                if (NetworkUtil.isOnline(this)) {
                    binding.tvMessage.text = "Something went wrong"
                    binding.tvMessageSub.text = state.message
                } else {
                    binding.tvMessage.text = "No internet connection"
                    binding.tvMessageSub.text =
                        "Questions for this exam have not been cached yet. Please retry when internet connection is restored."
                }
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                val list = state.data
                totalQuestions = list.size
                if (list.isEmpty()) {
                    binding.shimmerSkeletonTest.visibility = View.GONE
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.GONE
                    hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(
                        binding.ivMessageIcon,
                        hasEmptyPlayed
                    )
                    binding.tvMessage.text = when {
                        fromBookmark -> "Bookmarked question is no longer available"
                        pyqYear > 0 -> "No PYQs found for this year or paper"
                        else -> "No questions found for this exam"
                    }
                    binding.tvMessageSub.text = when {
                        fromBookmark -> "This question may have been removed or updated"
                        pyqYear > 0 -> "Upload questions tagged with PYQ and year from Admin Dashboard"
                        else -> "Please ask an admin to add questions for this exam"
                    }
                    binding.tvTimer.text = "--:--"
                    return
                }
                hasEmptyPlayed = false
                binding.messageGroup.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                if (binding.viewPager.adapter == null) {
                    binding.viewPager.adapter = QuestionAdapter(
                        questions = list,
                        getSelected = { viewModel.getAnswer(it) },
                        onSelect = { pos, letter ->
                            viewModel.setAnswer(pos, letter)
                            viewModel.markVisited(pos)
                            updatePalette(pos)
                        },
                        getBookmarked = { viewModel.isBookmarked(it) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        isHindi = { LanguageManager.isHindi(this) },
                        onReport = { q ->
                            ReportQuestionDialog.show(this@TestActivity, q, com.eve.app.util.AttemptKey.sourceExamId(examId), examName)
                        },
                        getQuestionTime = { pos -> viewModel.getQuestionTime(pos) },
                        isMarked = { viewModel.isMarked(it) },
                        onToggleMark = { pos ->
                            viewModel.toggleMark(pos)
                            updatePalette(pos)
                        }
                    )
                }
                com.eve.app.util.ShimmerHelper.crossFade(binding.shimmerSkeletonTest, binding.viewPager)
                if (!initialNavDone && !initialQuestionId.isNullOrBlank()) {
                    initialNavDone = true
                    val targetIndex = list.indexOfFirst { it.id == initialQuestionId }
                    if (targetIndex >= 0) {
                        binding.viewPager.post {
                            binding.viewPager.setCurrentItem(targetIndex, false)
                        }
                    } else {
                        com.eve.app.util.AppBulletin.showError(
                            this@TestActivity,
                            "Bookmarked question was not found in this test."
                        )
                    }
                }
                viewModel.markVisited(binding.viewPager.currentItem)
                updateNav(binding.viewPager.currentItem)
                updatePalette(binding.viewPager.currentItem)
                currentQuestionPosition = binding.viewPager.currentItem
                (binding.viewPager.adapter as? QuestionAdapter)?.notifyItemChanged(
                    currentQuestionPosition,
                    QuestionAdapter.PAYLOAD_TIMER
                )
            }
        }
    }

    private fun renderTimer(seconds: Long) {
        if (seconds < 0) return
        val m = seconds / 60
        val s = seconds % 60
        binding.tvTimer.text = String.format("%02d:%02d", m, s)
        binding.circularTimerView.setTime(seconds, total = (timeLimit * 60L).coerceAtLeast(seconds))
    }

    private fun updateNav(position: Int) {
        if (totalQuestions == 0) return
        val title = sessionTitle()
        binding.tvProgress.text = "$title  •  Question ${position + 1} / $totalQuestions"
        binding.btnPrev.isEnabled = position > 0
        binding.btnNext.isEnabled = position < totalQuestions - 1
    }

    private fun confirmSubmit() {
        val counts = QuestionPaletteAdapter.calculateSubmitDialogCounts(
            totalQuestions = totalQuestions,
            answeredIndices = viewModel.getAnsweredIndices(),
            visitedIndices = viewModel.getVisitedIndices(),
            markedIndices = viewModel.getMarkedIndices()
        )
        val message = "Answered: ${counts.answered}\n" +
                "Not answered: ${counts.notAnswered}\n" +
                "Marked for review: ${counts.markedForReview}\n" +
                "Not visited: ${counts.notVisited}"

        AlertDialog.Builder(this)
            .setTitle("Submit test?")
            .setMessage(message)
            .setPositiveButton("Submit") { _, _ -> submit() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submit() {
        if (submitted) return
        submitted = true
        viewModel.stopTimer()
        val items = viewModel.buildAnswerItems()
        val attemptName = sessionTitle()
        val isStandardMock = topic.isBlank() && pyqYear == 0

        val rawAnswers = items.map {
            mapOf(
                "questionId" to it.questionId,
                "number" to it.number,
                "selected" to it.selected,
                "isBookmarked" to it.isBookmarked,
                "timeTakenSeconds" to it.timeTakenSeconds
            )
        }

        val targetExamId = if (viewModel.currentExamId.isNotBlank()) viewModel.currentExamId else examId

        val body = mutableMapOf<String, Any>(
            "examId" to targetExamId,
            "examName" to attemptName,
            "category" to examCategory,
            "clientAttemptId" to viewModel.clientAttemptId,
            "answers" to rawAnswers
        )
        if (topic.isNotBlank()) body["topic"] = topic
        if (pyqYear > 0) body["pyqYear"] = pyqYear
        if (pyqPaper.isNotBlank()) body["pyqPaper"] = pyqPaper

        lifecycleScope.launch {
            try {
                val response = com.eve.app.data.remote.ApiClient.api.submitAttempt(body)
                if (response.success && response.data != null) {
                    val graded = response.data
                    viewModel.clearSession(examId)
                    viewModel.clearSession(targetExamId)
                    try {
                        com.eve.app.data.local.PendingSubmissionStore(this@TestActivity).remove(viewModel.clientAttemptId)
                    } catch (_: Exception) {}
                    com.eve.app.util.HapticHelper.performSubmitSuccess(binding.root)

                    AnalyticsHelper.logExamSubmit(
                        context = this@TestActivity,
                        examId = targetExamId,
                        examName = attemptName,
                        category = examCategory,
                        correct = graded.correct,
                        wrong = graded.wrong,
                        unattempted = graded.unattempted,
                        total = graded.total
                    )

                    val questionList = (viewModel.questions.value as? com.eve.app.util.UiState.Success)?.data ?: emptyList()
                    val enrichedAnswers = graded.answers.map { ans ->
                        val matchingQ = questionList.find { it.id == ans.questionId || (ans.questionId.isBlank() && it.questionText == ans.questionText) }
                        if (matchingQ != null) {
                            ans.copy(
                                optionA = matchingQ.optionA,
                                optionB = matchingQ.optionB,
                                optionC = matchingQ.optionC,
                                optionD = matchingQ.optionD,
                                optionAHi = matchingQ.optionAHi,
                                optionBHi = matchingQ.optionBHi,
                                optionCHi = matchingQ.optionCHi,
                                optionDHi = matchingQ.optionDHi
                            )
                        } else {
                            ans
                        }
                    }

                    val localAttempt = com.eve.app.data.model.TestAttempt(
                        id = graded.attemptId,
                        userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                        examId = targetExamId,
                        examName = attemptName,
                        category = examCategory,
                        score = graded.score,
                        total = graded.total,
                        correct = graded.correct,
                        wrong = graded.wrong,
                        unattempted = graded.unattempted,
                        timeTakenSeconds = graded.timeTakenSeconds,
                        timestamp = System.currentTimeMillis(),
                        answers = enrichedAnswers
                    )
                    if (isStandardMock) {
                        com.eve.app.ui.home.HomeViewModel.markAttemptSubmitted(targetExamId, localAttempt)
                        com.eve.app.ui.home.HomeViewModel.markAttemptSubmitted(examId, localAttempt)
                        com.eve.app.util.AttemptLimitManager.recordAttempt(this@TestActivity, targetExamId)
                    }

                    ResultDataHolder.setAnswers(enrichedAnswers)

                    startActivity(
                        Intent(this@TestActivity, ResultActivity::class.java)
                            .putExtra(Constants.EXTRA_EXAM_ID, targetExamId)
                            .putExtra(Constants.EXTRA_EXAM_NAME, attemptName)
                            .putExtra(Constants.EXTRA_EXAM_CATEGORY, examCategory)
                            .putExtra(Constants.EXTRA_TIME_LIMIT, timeLimit)
                            .putExtra(Constants.EXTRA_NEGATIVE_MARKING, negativeMarking)
                            .putExtra(Constants.EXTRA_CAN_REATTEMPT, isStandardMock)
                            .putExtra("EXTRA_SCORE", graded.score)
                            .putExtra("EXTRA_TOTAL", graded.total)
                            .putExtra("EXTRA_CORRECT", graded.correct)
                            .putExtra("EXTRA_WRONG", graded.wrong)
                            .putExtra("EXTRA_UNATTEMPTED", graded.unattempted)
                            .putExtra("EXTRA_COUNTED", graded.counted)
                            .putExtra("EXTRA_TIME_TAKEN_SECONDS", graded.timeTakenSeconds)
                    )
                    finish()
                } else {
                    val errMsg = response.error ?: "Submission was rejected by the server"
                    if (!NetworkUtil.isOnline(this@TestActivity)) {
                        handleOfflineSubmit(attemptName, items)
                    } else {
                        showSubmissionError(errMsg, attemptName, items)
                    }
                }
            } catch (e: Exception) {
                if (e is java.io.IOException || !NetworkUtil.isOnline(this@TestActivity)) {
                    handleOfflineSubmit(attemptName, items)
                } else {
                    val errMsg = if (e is retrofit2.HttpException) {
                        try {
                            val errBody = e.response()?.errorBody()?.string()
                            val parsed = com.google.gson.JsonParser.parseString(errBody).asJsonObject
                            parsed.get("error")?.asString ?: "Server returned error ${e.code()}"
                        } catch (_: Exception) {
                            "Server returned error ${e.code()}"
                        }
                    } else {
                        e.localizedMessage ?: "Failed to submit test"
                    }
                    showSubmissionError(errMsg, attemptName, items)
                }
            }
        }
    }

    private fun showSubmissionError(message: String, attemptName: String, items: List<AnswerItem>) {
        submitted = false
        com.eve.app.util.HapticHelper.performSubmitFailure(binding.root)
        AlertDialog.Builder(this)
            .setTitle("Submission Failed")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                submit()
            }
            .setNegativeButton("Save Offline") { _, _ ->
                submitted = true
                handleOfflineSubmit(attemptName, items)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun handleOfflineSubmit(attemptName: String, items: List<AnswerItem>) {
        viewModel.clearSession(examId)
        com.eve.app.util.HapticHelper.performSubmitFailure(binding.root)

        val pending = com.eve.app.data.local.PendingSubmission(
            clientAttemptId = viewModel.clientAttemptId,
            examId = examId,
            examName = attemptName,
            category = examCategory,
            answers = items.map {
                com.eve.app.data.local.PendingAnswer(
                    questionId = it.questionId,
                    number = it.number,
                    selected = it.selected,
                    isBookmarked = it.isBookmarked,
                    timeTakenSeconds = it.timeTakenSeconds
                )
            },
            topic = topic.ifBlank { null },
            pyqYear = if (pyqYear > 0) pyqYear else null,
            pyqPaper = pyqPaper.ifBlank { null }
        )
        com.eve.app.data.local.PendingSubmissionStore(this).save(pending)
        com.eve.app.worker.SubmitWorker.enqueue(this, viewModel.clientAttemptId)
        if (topic.isBlank() && pyqYear == 0) {
            lifecycleScope.launch {
                com.eve.app.util.AttemptLimitManager.recordAttempt(this@TestActivity, examId)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Submission Saved Offline")
            .setMessage("Your test responses have been stored securely on your device. They will be submitted automatically when network connectivity is restored.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun sessionTitle(): String = when {
        pyqYear > 0 -> {
            val paper = if (pyqPaper.isBlank()) "" else " • $pyqPaper"
            "$examName • PYQ $pyqYear$paper"
        }
        topic.isNotBlank() -> "$examName • $topic Practice"
        else -> examName
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putInt("saved_question_position", currentQuestionPosition)
    }

    private fun updatePalette(activePosition: Int) {
        if (totalQuestions <= 0) return
        val items = (0 until totalQuestions).map { i ->
            val state = QuestionPaletteAdapter.mapPaletteState(
                answered = viewModel.getAnswer(i).isNotEmpty(),
                visited = viewModel.isVisited(i),
                marked = viewModel.isMarked(i)
            )
            PaletteItem(
                number = i + 1,
                state = state,
                isActive = (i == activePosition)
            )
        }
        paletteAdapter.submit(items)
        binding.rvQuestionPalette.scrollToPosition(activePosition)
    }
}
