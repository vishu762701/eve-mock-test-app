package com.eve.app.ui.result

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.FlaggedQuestion
import com.eve.app.data.model.QuestionStat
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FlaggedQuestionRepository
import com.eve.app.data.repository.QuestionStatsRepository
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import com.eve.app.ui.common.ReportQuestionDialog
import com.eve.app.data.remote.ApiClient
import com.eve.app.ui.home.HomeViewModel
import com.eve.app.ui.home.MainActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.LanguageManager
import com.eve.app.util.SecurityHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResultActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModels()
    private lateinit var allItems: List<AnswerItem>
    private lateinit var binding: ActivityResultBinding

    private val adapter = AnswerAdapter(
        onReport = { item ->
            val examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
            val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME).orEmpty()
            ReportQuestionDialog.show(this, item, examId, examName)
        }
    )
    private lateinit var paletteAdapter: QuestionPaletteAdapter

    private val examRepo = ExamRepository()
    private val questionStatsRepo = QuestionStatsRepository()
    private val flaggedRepo = FlaggedQuestionRepository()
    private var examCutoffs: Map<String, Double> = emptyMap()
    private var loadedQuestionStats: Map<String, QuestionStat> = emptyMap()
    private var currentScore: Double = 0.0

    private var fromHistory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (viewModel.allItems.isEmpty()) {
            val incoming = ResultDataHolder.consumeAnswers()
            viewModel.initAnswers(incoming)
        }
        allItems = viewModel.allItems

        if (allItems.isEmpty()) {
            finish()
            return
        }

        fromHistory = intent.getBooleanExtra(Constants.EXTRA_FROM_HISTORY, false)
        val examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME)
        val attemptDate = intent.getStringExtra(Constants.EXTRA_ATTEMPT_DATE)

        if (fromHistory && !examName.isNullOrBlank()) {
            binding.tvResultSubtitle.visibility = View.VISIBLE
            binding.tvResultSubtitle.text = if (!attemptDate.isNullOrBlank()) {
                "$examName  •  $attemptDate"
            } else {
                examName
            }
            binding.btnHome.text = "Close"
        }

        val canReattempt = intent.getBooleanExtra(Constants.EXTRA_CAN_REATTEMPT, false)
        if (canReattempt) {
            binding.btnReattempt.visibility = View.VISIBLE
            binding.btnReattempt.setOnClickListener {
                showReattemptDialog(examId, examName ?: "this test")
            }
        }

        val total = allItems.size
        val correct = allItems.count { it.isCorrect }
        val unattempted = allItems.count { !it.isAttempted }
        val wrong = total - correct - unattempted
        currentScore = correct - wrong * Constants.NEGATIVE_MARK

        // Number count-up animation
        com.eve.app.util.NumberCountUpHelper.animateScoreCountUp(
            textView = binding.tvScore,
            targetScore = currentScore,
            total = total,
            durationMs = 950L,
            onFinished = {
                com.eve.app.util.NumberCountUpHelper.animateStatsCountUp(
                    textView = binding.tvStats,
                    correct = correct,
                    wrong = wrong,
                    unattempted = unattempted,
                    durationMs = 650L
                )
            }
        )

        // Accuracy Calculation
        val attempted = correct + wrong
        val accuracy = if (attempted > 0) (correct * 100.0 / attempted) else 0.0
        val accFormatted = String.format(java.util.Locale.US, "%.1f", accuracy)
        binding.tvAccuracy.text = "$accFormatted%"

        // Summary at top: accuracy %, avg time per question, fastest and slowest question
        val timedItems = allItems.filter { it.timeTakenSeconds > 0 }
        if (timedItems.isNotEmpty()) {
            val avgSeconds = kotlin.math.round(timedItems.map { it.timeTakenSeconds }.average()).toLong()
            val fastest = timedItems.minByOrNull { it.timeTakenSeconds }
            val slowest = timedItems.maxByOrNull { it.timeTakenSeconds }
            val timeParts = mutableListOf<String>()
            timeParts.add("Accuracy: $accFormatted%")
            timeParts.add("Avg: ${avgSeconds}s/q")
            if (fastest != null && slowest != null && fastest.number != slowest.number) {
                timeParts.add("Fastest: Q${fastest.number} (${fastest.timeTakenSeconds}s)")
                timeParts.add("Slowest: Q${slowest.number} (${slowest.timeTakenSeconds}s)")
            } else if (fastest != null) {
                timeParts.add("Fastest: Q${fastest.number} (${fastest.timeTakenSeconds}s)")
            }
            binding.tvTimeSummary.text = timeParts.joinToString("  •  ")
            binding.tvTimeSummary.visibility = View.VISIBLE
        } else {
            binding.tvTimeSummary.text = "Accuracy: $accFormatted%"
            binding.tvTimeSummary.visibility = View.VISIBLE
        }

        // Setup Answers RecyclerView
        binding.rvAnswers.layoutManager = LinearLayoutManager(this)
        binding.rvAnswers.adapter = adapter
        adapter.submit(allItems)
        adapter.setHindi(LanguageManager.isHindi(this))
        adapter.setShowTimeInsight(!fromHistory)

        LanguageManager.setupToggleButton(this, binding.btnLanguage) { hindi ->
            adapter.setHindi(hindi)
        }

        // Setup Question Palette Navigation (Item 6)
        setupQuestionPalette()

        // Setup Combinable Filters (Item 4)
        setupFilters(total, correct, wrong, unattempted)

        // Setup Cutoff comparison and Depth stats (Item 2)
        loadDepthStatsAndCutoffs(examId)

        if (examId.isNotBlank()) {
            binding.btnLeaderboard.visibility = View.VISIBLE
            binding.btnLeaderboard.setOnClickListener {
                startActivity(
                    Intent(this, LeaderboardActivity::class.java)
                        .putExtra(Constants.EXTRA_EXAM_ID, examId)
                        .putExtra(Constants.EXTRA_EXAM_NAME, examName)
                )
            }
        }

        binding.btnHome.setOnClickListener { close() }
    }

    private fun setupQuestionPalette() {
        val paletteItems = allItems.mapIndexed { index, item ->
            val state = when {
                item.isCorrect -> PaletteState.CORRECT
                item.isAttempted -> PaletteState.WRONG
                else -> PaletteState.UNATTEMPTED
            }
            PaletteItem(number = index + 1, state = state, isActive = (index == 0))
        }
        paletteAdapter = QuestionPaletteAdapter { pos ->
            binding.rvAnswers.smoothScrollToPosition(pos)
        }
        binding.rvResultPalette.adapter = paletteAdapter
        paletteAdapter.submit(paletteItems)
    }

    private fun setupFilters(total: Int, correct: Int, wrong: Int, unattempted: Int) {
        binding.chipAll.text = "All ($total)"
        binding.chipCorrect.text = "Correct ($correct)"
        binding.chipWrong.text = "Wrong ($wrong)"
        binding.chipNotAttempted.text = "Unattempted ($unattempted)"

        val filterChangeListener = {
            applyReviewFilters()
        }

        binding.chipAll.setOnClickListener {
            binding.chipCorrect.isChecked = false
            binding.chipWrong.isChecked = false
            binding.chipNotAttempted.isChecked = false
            binding.chipAll.isChecked = true
            applyReviewFilters()
        }

        binding.chipCorrect.setOnClickListener {
            binding.chipAll.isChecked = false
            filterChangeListener()
        }
        binding.chipWrong.setOnClickListener {
            binding.chipAll.isChecked = false
            filterChangeListener()
        }
        binding.chipNotAttempted.setOnClickListener {
            binding.chipAll.isChecked = false
            filterChangeListener()
        }
        binding.chipOvertimeOnly.setOnClickListener {
            filterChangeListener()
        }
    }

    private fun applyReviewFilters() {
        val showCorrect = binding.chipCorrect.isChecked
        val showWrong = binding.chipWrong.isChecked
        val showUnattempted = binding.chipNotAttempted.isChecked
        val onlyOvertime = binding.chipOvertimeOnly.isChecked

        val showAll = binding.chipAll.isChecked || (!showCorrect && !showWrong && !showUnattempted)

        val filtered = allItems.filter { item ->
            val matchesStatus = when {
                showAll -> true
                item.isCorrect && showCorrect -> true
                (item.isAttempted && !item.isCorrect) && showWrong -> true
                !item.isAttempted && showUnattempted -> true
                else -> false
            }
            if (!matchesStatus) return@filter false

            if (onlyOvertime) {
                val stat = loadedQuestionStats[item.questionId]
                val avg = stat?.calculatedAvgSeconds ?: 45
                item.timeTakenSeconds > avg
            } else {
                true
            }
        }
        adapter.submit(filtered)
        binding.rvAnswers.scrollToPosition(0)
    }

    private fun loadDepthStatsAndCutoffs(examId: String) {
        lifecycleScope.launch {
            // 1. Load Question Stats for Accuracy & Overtime
            try {
                val qIds = allItems.map { it.questionId }.filter { it.isNotBlank() }
                loadedQuestionStats = viewModel.getOrFetchQuestionStats(qIds)
                adapter.setQuestionStats(loadedQuestionStats)
            } catch (_: Exception) {}

            // 2. Load Cutoffs & Exam Info
            if (examId.isNotBlank()) {
                try {
                    val exam = examRepo.getExam(examId)
                    if (exam != null) {
                        examCutoffs = exam.cutoffs
                    }
                } catch (_: Exception) {}
            }
            setupCutoffSelector()

            // 3. Load Rank, Percentile, Best/Avg Score from Firestore attempts
            if (examId.isNotBlank()) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                    val snap = firestore.collection("attempts")
                        .whereEqualTo("examId", examId)
                        .get()
                        .await()

                    val allScores = snap.documents.mapNotNull { it.getDouble("score") }
                    val totalAttempts = maxOf(allScores.size, 1)

                    val higherScores = allScores.count { it > currentScore }
                    val rank = higherScores + 1
                    val lowerScores = allScores.count { it < currentScore }
                    val percentile = if (totalAttempts <= 1) 100.0 else ((lowerScores * 100.0) / (totalAttempts - 1))

                    binding.tvRank.text = "#$rank / $totalAttempts"
                    binding.tvPercentile.text = "${String.format("%.1f", percentile)}%"

                    // Student's historical best and average
                    val myPastScores = snap.documents
                        .filter { it.getString("userId") == currentUid }
                        .mapNotNull { it.getDouble("score") }

                    val best = if (myPastScores.isNotEmpty()) maxOf(myPastScores.maxOrNull() ?: currentScore, currentScore) else currentScore
                    val avg = if (myPastScores.isNotEmpty()) myPastScores.average() else currentScore

                    val bestStr = if (best % 1.0 == 0.0) best.toInt().toString() else String.format("%.1f", best)
                    val avgStr = if (avg % 1.0 == 0.0) avg.toInt().toString() else String.format("%.1f", avg)
                    binding.tvHistoryBestAvg.text = "Best: $bestStr  •  Avg: $avgStr"

                } catch (e: Exception) {
                    binding.tvRank.text = "#1 / 1"
                    binding.tvPercentile.text = "100.0%"
                    val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format("%.1f", currentScore)
                    binding.tvHistoryBestAvg.text = "Best: $sStr  •  Avg: $sStr"
                }
            } else {
                binding.tvRank.text = "#1 / 1"
                binding.tvPercentile.text = "100.0%"
                val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format("%.1f", currentScore)
                binding.tvHistoryBestAvg.text = "Best: $sStr  •  Avg: $sStr"
            }
        }
    }

    private fun setupCutoffSelector() {
        fun updateCutoffVerdict(category: String) {
            val cutoff = examCutoffs[category]
            if (cutoff != null) {
                binding.layoutCutoffComparison.visibility = View.VISIBLE
                binding.tvCutoffValue.text = "Cutoff: $cutoff"
                if (currentScore >= cutoff) {
                    binding.tvCutoffVerdict.text = "Above Cutoff (Qualified) ✓"
                    binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.success))
                } else {
                    binding.tvCutoffVerdict.text = "Below Cutoff ✗"
                    binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.error))
                }
            } else {
                // If no cutoff has been set for this category, omit comparison (don't show zero)
                binding.layoutCutoffComparison.visibility = View.GONE
            }
        }

        binding.chipGroupCutoffCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val cat = when (checkedIds.firstOrNull()) {
                binding.chipCatGeneral.id -> "General"
                binding.chipCatObc.id -> "OBC"
                binding.chipCatSc.id -> "SC"
                binding.chipCatSt.id -> "ST"
                binding.chipCatEws.id -> "EWS"
                else -> "General"
            }
            updateCutoffVerdict(cat)
        }

        // Initialize with General
        updateCutoffVerdict("General")
    }

    private fun showReattemptDialog(examId: String, examName: String) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Reattempt this test?")
            .setMessage("Your previous result for \"$examName\" (score, answers and rank) will be permanently deleted and replaced by your new attempt. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear & Reattempt", null)
            .create()

        dialog.setOnShowListener {
            val confirmBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            confirmBtn.setTextColor(ContextCompat.getColor(this, R.color.eve_status_error))

            confirmBtn.setOnClickListener {
                confirmBtn.isEnabled = false
                cancelBtn.isEnabled = false
                confirmBtn.text = "Resetting..."

                lifecycleScope.launch {
                    try {
                        val response = ApiClient.apiService.resetAttemptPost(mapOf("examId" to examId))
                        if (response.success) {
                            ResultDataHolder.clear()
                            HomeViewModel.markAttemptCleared(examId)
                            val timeLimit = intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 60)
                            val category = intent.getStringExtra(Constants.EXTRA_EXAM_CATEGORY).orEmpty()
                            val testIntent = Intent(this@ResultActivity, TestActivity::class.java).apply {
                                putExtra(Constants.EXTRA_EXAM_ID, examId)
                                putExtra(Constants.EXTRA_EXAM_NAME, examName)
                                putExtra(Constants.EXTRA_EXAM_CATEGORY, category)
                                putExtra(Constants.EXTRA_TIME_LIMIT, timeLimit)
                            }
                            dialog.dismiss()
                            startActivity(testIntent)
                            finish()
                        } else {
                            confirmBtn.isEnabled = true
                            cancelBtn.isEnabled = true
                            confirmBtn.text = "Clear & Reattempt"
                            AppBulletin.showError(this@ResultActivity, response.error ?: "Failed to reset attempt")
                        }
                    } catch (e: Exception) {
                        confirmBtn.isEnabled = true
                        cancelBtn.isEnabled = true
                        confirmBtn.text = "Clear & Reattempt"
                        AppBulletin.showError(this@ResultActivity, e.localizedMessage ?: "Failed to reset attempt")
                    }
                }
            }
        }

        dialog.show()
    }

    private fun close() {
        if (fromHistory) {
            finish()
            return
        }
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        close()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ResultDataHolder.clear()
        }
    }
}
