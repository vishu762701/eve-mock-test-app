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
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.QuestionStatsRepository
import com.eve.app.databinding.ActivityTestBinding
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.util.AnalyticsHelper
import com.eve.app.util.Constants
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.UiState
import com.eve.app.util.isHardcodedAdmin
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val viewModel: TestViewModel by viewModels()

    private lateinit var examId: String
    private var timeLimit = 30
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
    private var questionStartTimeMs: Long = SystemClock.elapsedRealtime()
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
        topic = intent.getStringExtra(Constants.EXTRA_TOPIC).orEmpty()
        pyqYear = intent.getIntExtra(Constants.EXTRA_PYQ_YEAR, 0)
        pyqPaper = intent.getStringExtra(Constants.EXTRA_PYQ_PAPER).orEmpty()
        fromBookmark = intent.getBooleanExtra(Constants.EXTRA_FROM_BOOKMARK, false)
        initialQuestionId = intent.getStringExtra(Constants.EXTRA_INITIAL_QUESTION_ID)

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        lifecycleScope.launch {
            val isAdmin = currentUser?.let { user ->
                isHardcodedAdmin(user.email) || adminRepository.isAdmin(user.email)
            } ?: false
            isAdminUser = isAdmin
            viewModel.start(examId, timeLimit, topic, pyqYear, pyqPaper, isAdminUser, examName, fromBookmark)
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
                val now = SystemClock.elapsedRealtime()
                val spent = ((now - questionStartTimeMs) / 1000).coerceAtLeast(0)
                viewModel.recordQuestionTime(currentQuestionPosition, spent)
                currentQuestionPosition = position
                questionStartTimeMs = now
                updateNav(position)
                updatePalette(position)
                updateQuestionTimerDisplay(position)
            }
        })

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
                            com.eve.app.util.AppBulletin.showError(
                                this@TestActivity,
                                getString(com.eve.app.R.string.exam_already_attempted)
                            )
                            finish()
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
                        list,
                        getSelected = { viewModel.getAnswer(it) },
                        onSelect = { pos, letter ->
                            val now = SystemClock.elapsedRealtime()
                            val spent = ((now - questionStartTimeMs) / 1000).coerceAtLeast(1)
                            viewModel.recordQuestionTime(pos, spent)
                            questionStartTimeMs = now
                            viewModel.setAnswer(pos, letter)
                            updatePalette(pos)
                            updateQuestionTimerDisplay(pos)
                        },
                        getBookmarked = { viewModel.isBookmarked(it) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        isHindi = { LanguageManager.isHindi(this) }
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
                questionStartTimeMs = SystemClock.elapsedRealtime()
                updateQuestionTimerDisplay(currentQuestionPosition)
            }
        }
    }

    private fun renderTimer(seconds: Long) {
        if (seconds < 0) return
        val m = seconds / 60
        val s = seconds % 60
        binding.tvTimer.text = String.format("%02d:%02d", m, s)
        binding.circularTimerView.setTime(seconds, total = (timeLimit * 60L).coerceAtLeast(seconds))
        updateQuestionTimerDisplay(currentQuestionPosition)
    }

    private fun updateQuestionTimerDisplay(position: Int) {
        if (totalQuestions <= 0) return
        val baseSec = viewModel.getQuestionTime(position)
        val currentSec = ((SystemClock.elapsedRealtime() - questionStartTimeMs) / 1000).coerceAtLeast(0)
        val totalSec = baseSec + currentSec
        val m = totalSec / 60
        val s = totalSec % 60
        binding.tvQuestionTimer.text = String.format("%02d:%02d", m, s)
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
        val now = SystemClock.elapsedRealtime()
        val spent = ((now - questionStartTimeMs) / 1000).coerceAtLeast(0)
        viewModel.recordQuestionTime(currentQuestionPosition, spent)
        questionStartTimeMs = now
        viewModel.stopTimer()
        val items = viewModel.buildAnswerItems()
        val attemptName = sessionTitle()
        viewModel.saveAttempt(examId, attemptName, examCategory, items)

        for (item in items) {
            if (item.questionId.isNotBlank()) {
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
            correct = items.count { it.isCorrect },
            wrong = items.count { it.isAttempted && !it.isCorrect },
            unattempted = items.count { !it.isAttempted },
            total = items.size
        )

        ResultDataHolder.setAnswers(items)

        startActivity(
            Intent(this, ResultActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, examId)
                .putExtra(Constants.EXTRA_EXAM_NAME, attemptName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, examCategory)
        )
        finish()
    }

    private fun sessionTitle(): String = when {
        pyqYear > 0 -> {
            val paper = if (pyqPaper.isBlank()) "" else " • $pyqPaper"
            "$examName • PYQ $pyqYear$paper"
        }
        topic.isNotBlank() -> "$examName • $topic Practice"
        else -> examName
    }

    override fun onPause() {
        super.onPause()
        if (!submitted) {
            val now = SystemClock.elapsedRealtime()
            val spent = ((now - questionStartTimeMs) / 1000).coerceAtLeast(0)
            viewModel.recordQuestionTime(currentQuestionPosition, spent)
            questionStartTimeMs = now
        }
    }

    override fun onResume() {
        super.onResume()
        if (!submitted) {
            questionStartTimeMs = SystemClock.elapsedRealtime()
            updateQuestionTimerDisplay(currentQuestionPosition)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        if (!submitted) {
            val now = SystemClock.elapsedRealtime()
            val spent = ((now - questionStartTimeMs) / 1000).coerceAtLeast(0)
            viewModel.recordQuestionTime(currentQuestionPosition, spent)
            questionStartTimeMs = now
        }
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
