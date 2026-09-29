package com.eve.app.ui.test

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

class TestActivity : AppCompatActivity() {

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
    private val questionStatsRepo = QuestionStatsRepository()

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
                while (!submitted) {
                    delay(1000)
                    if (!submitted && totalQuestions > 0) {
                        val currentPos = binding.viewPager.currentItem
                        viewModel.addQuestionSecond(currentPos)
                        (binding.viewPager.adapter as? QuestionAdapter)?.notifyItemChanged(
                            currentPos,
                            QuestionAdapter.PAYLOAD_TIMER
                        )
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
                            updatePalette(pos)
                        },
                        getBookmarked = { viewModel.isBookmarked(it) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        isHindi = { LanguageManager.isHindi(this) },
                        onReport = { q ->
                            ReportQuestionDialog.show(this@TestActivity, q, com.eve.app.util.AttemptKey.sourceExamId(examId), examName)
                        },
                        getQuestionTime = { pos -> viewModel.getQuestionTime(pos) }
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
        val items = viewModel.buildAnswerItems()
        val unattempted = items.count { !it.isAttempted }
        AlertDialog.Builder(this)
            .setTitle("Submit test?")
            .setMessage("Unattempted questions: $unattempted")
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

        val total = items.size
        val correct = items.count { it.isCorrect }
        val wrong = items.count { it.isAttempted && !it.isCorrect }
        val unattempted = items.count { !it.isAttempted }
        val score = correct.toDouble()
        val localAttempt = com.eve.app.data.model.TestAttempt(
            id = "local_${System.currentTimeMillis()}",
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            examId = examId,
            examName = attemptName,
            category = examCategory,
            score = score,
            total = total,
            correct = correct,
            wrong = wrong,
            unattempted = unattempted,
            timestamp = System.currentTimeMillis(),
            answers = items
        )
        if (isStandardMock) {
            com.eve.app.ui.home.HomeViewModel.markAttemptSubmitted(examId, localAttempt)
        }

        for (item in items) {
            if (item.questionId.isNotBlank() && item.isAttempted) {
                questionStatsRepo.recordQuestionAttempt(
                    questionId = item.questionId,
                    examId = examId,
                    isCorrect = item.isCorrect,
                    timeSeconds = item.timeTakenSeconds
                )
            }
        }

        // Phase 15: exam submit event + score summary (average score / weak exams Console me dikhenge)
        AnalyticsHelper.logExamSubmit(
            context = this,
            examId = examId,
            examName = attemptName,
            category = examCategory,
            correct = correct,
            wrong = wrong,
            unattempted = unattempted,
            total = total
        )

        ResultDataHolder.setAnswers(items)

        lifecycleScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    viewModel.saveAttemptSync(examId, attemptName, examCategory, items)
                }
            } catch (_: Exception) {
                viewModel.saveAttempt(examId, attemptName, examCategory, items)
            }

            startActivity(
                Intent(this@TestActivity, ResultActivity::class.java)
                    .putExtra(Constants.EXTRA_EXAM_ID, examId)
                    .putExtra(Constants.EXTRA_EXAM_NAME, attemptName)
                    .putExtra(Constants.EXTRA_EXAM_CATEGORY, examCategory)
                    .putExtra(Constants.EXTRA_TIME_LIMIT, timeLimit)
                    .putExtra(Constants.EXTRA_NEGATIVE_MARKING, negativeMarking)
                    .putExtra(Constants.EXTRA_CAN_REATTEMPT, isStandardMock)
            )
            finish()
        }
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
            val ans = viewModel.getAnswer(i)
            val state = if (ans.isNotEmpty()) PaletteState.ANSWERED else PaletteState.UNATTEMPTED
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
