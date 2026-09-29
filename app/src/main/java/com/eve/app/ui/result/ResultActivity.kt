package com.eve.app.ui.result

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.QuestionStat
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.QuestionStatsRepository
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import com.eve.app.ui.common.ReportQuestionDialog
import com.eve.app.ui.home.HomeViewModel
import com.eve.app.ui.home.MainActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.AttemptKey
import com.eve.app.util.Constants
import com.eve.app.util.LanguageManager
import com.eve.app.util.NumberCountUpHelper
import com.eve.app.util.SecurityHelper
import com.eve.app.util.ShareCardHelper
import com.eve.app.util.TopicAccuracyHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResultActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModels()
    private lateinit var allItems: List<AnswerItem>
    private lateinit var binding: ActivityResultBinding

    private var currentExamName: String = ""
    private var currentExamId: String = ""

    private val adapter = AnswerAdapter(
        onReport = { item ->
            ReportQuestionDialog.show(
                activity = this,
                item = item,
                examId = AttemptKey.sourceExamId(currentExamId),
                examName = currentExamName
            )
        }
    )
    private lateinit var paletteAdapter: QuestionPaletteAdapter
    private var paletteItems: List<PaletteItem> = emptyList()
    private var selectedQuestionIndex: Int = 0

    private val examRepo = ExamRepository()
    private val questionStatsRepo = QuestionStatsRepository()
    private var examCutoffs: Map<String, Double> = emptyMap()
    private var loadedQuestionStats: Map<String, QuestionStat> = emptyMap()
    private var currentScore: Double = 0.0

    private var selectedCutoffCategory: String = "General"
    private var fromHistory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            close()
        }

        currentExamId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME)
        currentExamName = examName.orEmpty()
        val attemptDate = intent.getStringExtra(Constants.EXTRA_ATTEMPT_DATE)

        if (viewModel.allItems.isEmpty()) {
            val incoming = ResultDataHolder.consumeAnswers()
            if (incoming.isNotEmpty()) {
                viewModel.initAnswers(incoming)
            } else if (currentExamId.isNotBlank()) {
                val cached = HomeViewModel.getCachedAttempt(currentExamId)
                if (cached != null && cached.answers.isNotEmpty()) {
                    viewModel.initAnswers(cached.answers)
                }
            }
        }
        allItems = viewModel.allItems

        if (allItems.isEmpty()) {
            finish()
            return
        }

        fromHistory = intent.getBooleanExtra(Constants.EXTRA_FROM_HISTORY, false)

        if (allItems.any { it.optionA.isBlank() } && currentExamId.isNotBlank()) {
            lifecycleScope.launch {
                try {
                    val genTestId = AttemptKey.generatedTestId(currentExamId)
                    val sourceExamId = AttemptKey.sourceExamId(currentExamId)
                    val questions: List<com.eve.app.data.model.Question> = if (genTestId != null) {
                        val genTest = examRepo.getGeneratedTest(genTestId)
                        genTest?.questions?.mapIndexed { idx, gq ->
                            com.eve.app.data.model.Question(
                                id = "${genTest.id}_$idx",
                                examId = sourceExamId,
                                questionText = gq.questionText,
                                optionA = gq.optionA,
                                optionB = gq.optionB,
                                optionC = gq.optionC,
                                optionD = gq.optionD,
                                correctAnswer = gq.correctAnswer,
                                explanation = gq.explanation
                            )
                        } ?: emptyList()
                    } else {
                        val regular = examRepo.getQuestions(sourceExamId)
                        if (regular.isNotEmpty()) regular else {
                            val live = examRepo.getLiveGeneratedTests(sourceExamId).firstOrNull()
                            live?.questions?.mapIndexed { idx, gq ->
                                com.eve.app.data.model.Question(
                                    id = "${live.id}_$idx",
                                    examId = sourceExamId,
                                    questionText = gq.questionText,
                                    optionA = gq.optionA,
                                    optionB = gq.optionB,
                                    optionC = gq.optionC,
                                    optionD = gq.optionD,
                                    correctAnswer = gq.correctAnswer,
                                    explanation = gq.explanation
                                )
                            } ?: emptyList()
                        }
                    }

                    if (questions.isNotEmpty()) {
                        val qMap = questions.associateBy { it.id }
                        val enriched = allItems.map { item ->
                            val q = qMap[item.questionId] ?: questions.getOrNull(item.number - 1)
                            if (q != null && item.optionA.isBlank()) {
                                item.copy(
                                    optionA = q.optionA,
                                    optionB = q.optionB,
                                    optionC = q.optionC,
                                    optionD = q.optionD,
                                    optionAHi = q.optionAHi,
                                    optionBHi = q.optionBHi,
                                    optionCHi = q.optionCHi,
                                    optionDHi = q.optionDHi
                                )
                            } else {
                                item
                            }
                        }
                        allItems = enriched
                        viewModel.updateAnswers(enriched)
                        selectQuestion(selectedQuestionIndex)
                    }
                } catch (_: Exception) {}
            }
        }

        if (!currentExamName.isBlank()) {
            binding.tvResultTitle.text = currentExamName
        }

        if (fromHistory) {
            binding.tvResultSubtitle.visibility = View.VISIBLE
            binding.tvResultSubtitle.text = if (!attemptDate.isNullOrBlank()) {
                "Attempted on $attemptDate"
            } else {
                "Historical Attempt"
            }
            binding.btnHome.text = "Close"
        }

        val canReattempt = intent.getBooleanExtra(Constants.EXTRA_CAN_REATTEMPT, false)
        binding.btnReattempt.visibility = if (canReattempt) View.VISIBLE else View.GONE
        if (canReattempt) {
            binding.btnReattempt.setOnClickListener {
                showReattemptDialog(currentExamId, if (currentExamName.isNotBlank()) currentExamName else "this test")
            }
        }

        val isCounted = intent.getIntExtra("EXTRA_COUNTED", 1)
        val total = if (intent.hasExtra("EXTRA_TOTAL")) intent.getIntExtra("EXTRA_TOTAL", allItems.size) else allItems.size
        val correct = if (intent.hasExtra("EXTRA_CORRECT")) intent.getIntExtra("EXTRA_CORRECT", allItems.count { it.isCorrect }) else allItems.count { it.isCorrect }
        val unattempted = if (intent.hasExtra("EXTRA_UNATTEMPTED")) intent.getIntExtra("EXTRA_UNATTEMPTED", allItems.count { !it.isAttempted }) else allItems.count { !it.isAttempted }
        val wrong = if (intent.hasExtra("EXTRA_WRONG")) intent.getIntExtra("EXTRA_WRONG", (total - correct - unattempted).coerceAtLeast(0)) else (total - correct - unattempted)

        if (intent.hasExtra("EXTRA_SCORE")) {
            currentScore = intent.getDoubleExtra("EXTRA_SCORE", 0.0)
        } else {
            val negMark = if (intent.hasExtra(Constants.EXTRA_NEGATIVE_MARKING)) {
                intent.getDoubleExtra(Constants.EXTRA_NEGATIVE_MARKING, Constants.NEGATIVE_MARK)
            } else {
                Constants.NEGATIVE_MARK
            }
            currentScore = Math.round((correct - wrong * negMark) * 100.0) / 100.0

            if (currentExamId.isNotBlank() && !intent.hasExtra(Constants.EXTRA_NEGATIVE_MARKING)) {
                lifecycleScope.launch {
                    try {
                        val exam = examRepo.getExam(AttemptKey.sourceExamId(currentExamId))
                        val examNeg = exam?.negativeMarkingValue ?: 0.0
                        val updatedScore = Math.round((correct - wrong * examNeg) * 100.0) / 100.0
                        if (updatedScore != currentScore) {
                            currentScore = updatedScore
                            val finalStr = if (currentScore % 1.0 == 0.0) {
                                currentScore.toInt().toString()
                            } else {
                                String.format(java.util.Locale.US, "%.2f", currentScore)
                            }
                            binding.tvScore.text = "$finalStr / $total"
                            updateCutoffUI()
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Section 1: Overview Breakdown Metrics
        binding.tvCorrectCount.text = correct.toString()
        binding.tvWrongCount.text = wrong.toString()
        binding.tvUnattemptedCount.text = unattempted.toString()

        val attempted = correct + wrong
        val accuracy = if (attempted > 0) (correct * 100.0 / attempted) else 0.0
        val accFormatted = String.format(java.util.Locale.US, "%.1f", accuracy)
        binding.tvAccuracy.text = "$accFormatted%"

        val scorePct = if (total > 0) (currentScore * 100.0 / total).coerceAtLeast(0.0) else 0.0
        binding.tvScorePercentage.text = String.format(java.util.Locale.US, "%.1f%% Score", scorePct)

        // Trigger celebratory confetti for high-performing counted attempts (accuracy >= 80%)
        if (isCounted == 1 && accuracy >= 80.0) {
            binding.confettiView.startConfetti()
        }

        // Number count-up animation for hero score (decelerating over 800ms)
        NumberCountUpHelper.animateScoreCountUp(
            textView = binding.tvScore,
            targetScore = currentScore,
            total = total,
            durationMs = 800L,
            onFinished = {
                NumberCountUpHelper.animateStatsCountUp(
                    textView = binding.tvStats,
                    correct = correct,
                    wrong = wrong,
                    unattempted = unattempted,
                    durationMs = 650L
                )
            }
        )

        // Speed and Pace Summary
        val timedItems = allItems.filter { it.timeTakenSeconds > 0 }
        if (timedItems.isNotEmpty()) {
            val avgSeconds = kotlin.math.round(timedItems.map { it.timeTakenSeconds }.average()).toLong()
            val fastest = timedItems.minByOrNull { it.timeTakenSeconds }
            val slowest = timedItems.maxByOrNull { it.timeTakenSeconds }
            val timeParts = mutableListOf<String>()
            timeParts.add("Avg: ${avgSeconds}s / question")
            if (fastest != null && slowest != null && fastest.number != slowest.number) {
                timeParts.add("Fastest: Q${fastest.number} (${fastest.timeTakenSeconds}s)")
                timeParts.add("Slowest: Q${slowest.number} (${slowest.timeTakenSeconds}s)")
            } else if (fastest != null) {
                timeParts.add("Fastest: Q${fastest.number} (${fastest.timeTakenSeconds}s)")
            }
            binding.tvTimeSummary.text = timeParts.joinToString("  •  ")
        } else {
            binding.tvTimeSummary.text = "Accuracy: $accFormatted%  •  Total Questions: $total"
        }

        // Share Card Button
        binding.btnShare.setOnClickListener {
            val scoreStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.2f", currentScore)
            val scorePctStr = String.format(java.util.Locale.US, "%.1f%% Score", scorePct)
            val rankText = binding.tvRank.text.toString()
            ShareCardHelper.generateAndShare(
                context = this,
                examName = if (currentExamName.isNotBlank()) currentExamName else "Mock Test",
                scoreText = "$scoreStr / $total",
                percentageText = scorePctStr,
                accuracyText = "$accFormatted%",
                rankText = rankText,
                correctCount = correct,
                wrongCount = wrong,
                skippedCount = unattempted
            )
        }

        // Setup Answers RecyclerView (Section 3: Answer Review)
        binding.rvAnswers.layoutManager = LinearLayoutManager(this)
        binding.rvAnswers.adapter = adapter
        adapter.setHindi(LanguageManager.isHindi(this))
        adapter.setShowTimeInsight(!fromHistory)

        LanguageManager.setupToggleButton(this, binding.btnLanguage) { hindi ->
            adapter.setHindi(hindi)
        }

        // Setup Segmented 4-Tab Navigation (Review | Overview | Leaderboard | Analysis)
        setupTabLayout()

        // Setup Question Palette Navigation (Single Question Isolation)
        setupQuestionPalette()

        // Setup Filter Chips (Right, Wrong, Unattempted)
        setupFilters(correct, wrong, unattempted)

        // Setup Section 4: Analysis Tab
        setupAnalysisSection(total, correct, wrong, unattempted, accuracy)

        // Setup Cutoff / Performance (Section 2)
        loadDepthStatsAndCutoffs(currentExamId)

        // Bottom Actions
        val showLeaderboard = currentExamId.isNotBlank()
        binding.btnLeaderboard.visibility = if (showLeaderboard) View.VISIBLE else View.GONE
        if (showLeaderboard) {
            binding.btnLeaderboard.setOnClickListener {
                startActivity(
                    Intent(this, LeaderboardActivity::class.java)
                        .putExtra(Constants.EXTRA_EXAM_ID, currentExamId)
                        .putExtra(Constants.EXTRA_EXAM_NAME, currentExamName)
                )
            }
        }
        binding.btnOpenLeaderboard.setOnClickListener {
            if (currentExamId.isNotBlank()) {
                startActivity(
                    Intent(this, LeaderboardActivity::class.java)
                        .putExtra(Constants.EXTRA_EXAM_ID, currentExamId)
                        .putExtra(Constants.EXTRA_EXAM_NAME, currentExamName)
                )
            } else {
                AppBulletin.show(this, "Leaderboard is only available for mock tests.")
            }
        }
        if (currentExamId.isBlank()) {
            binding.tvLeaderboardTabSubtitle.text = "Leaderboard is only available for scheduled and published mock tests."
        }
        binding.layoutActionCluster.visibility = if (canReattempt || showLeaderboard) View.VISIBLE else View.GONE

        binding.btnHome.setOnClickListener { close() }
    }

    private fun setupTabLayout() {
        binding.tabLayoutResult.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                binding.sectionReview.visibility = if (pos == 0) View.VISIBLE else View.GONE
                binding.sectionOverview.visibility = if (pos == 1) View.VISIBLE else View.GONE
                binding.sectionLeaderboard.visibility = if (pos == 2) View.VISIBLE else View.GONE
                binding.sectionAnalysis.visibility = if (pos == 3) View.VISIBLE else View.GONE

                if (pos == 0) {
                    selectQuestion(selectedQuestionIndex)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAnalysisSection(
        total: Int,
        correct: Int,
        wrong: Int,
        unattempted: Int,
        accuracy: Double
    ) {
        val attempted = correct + wrong
        val attemptedPct = if (total > 0) (attempted * 100.0 / total) else 0.0
        binding.tvAnalysisAttempted.text = "${String.format(java.util.Locale.US, "%.1f", attemptedPct)}%"
        binding.tvAnalysisAccuracy.text = "${String.format(java.util.Locale.US, "%.1f", accuracy)}%"

        val timedItems = allItems.filter { it.timeTakenSeconds > 0 }
        val avgSeconds = if (timedItems.isNotEmpty()) {
            kotlin.math.round(timedItems.map { it.timeTakenSeconds }.average()).toLong()
        } else {
            0L
        }
        binding.tvAnalysisAvgTime.text = "${avgSeconds}s"

        // Pace chart
        val barItems = allItems.mapIndexed { index, item ->
            QuestionTimeChartView.BarItem(
                questionNumber = index + 1,
                timeSeconds = item.timeTakenSeconds,
                isCorrect = item.isCorrect,
                isAttempted = item.isAttempted
            )
        }
        binding.chartTimeView.setItems(barItems)
        binding.chartTimeView.onBarSelected = { bar ->
            val status = when {
                bar.isCorrect -> "Correct"
                bar.isAttempted -> "Wrong"
                else -> "Skipped"
            }
            binding.tvChartDetail.text = "Question ${bar.questionNumber}: ${bar.timeSeconds}s • $status"
        }

        // 3 Slowest Questions
        val slowest = allItems.filter { it.timeTakenSeconds > 0 }.sortedByDescending { it.timeTakenSeconds }.take(3)
        binding.layoutSlowestList.removeAllViews()
        if (slowest.isNotEmpty()) {
            for (item in slowest) {
                val tv = android.widget.TextView(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (6 * resources.displayMetrics.density).toInt())
                    }
                    val status = when {
                        item.isCorrect -> "Correct"
                        item.isAttempted -> "Wrong"
                        else -> "Skipped"
                    }
                    val statusColor = when {
                        item.isCorrect -> ContextCompat.getColor(context, R.color.eve_status_success)
                        item.isAttempted -> ContextCompat.getColor(context, R.color.eve_status_error)
                        else -> ContextCompat.getColor(context, R.color.eve_text_secondary)
                    }
                    text = "Q${item.number}: ${item.timeTakenSeconds}s • $status"
                    setTextColor(statusColor)
                    textSize = 13f
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                }
                binding.layoutSlowestList.addView(tv)
            }
        } else {
            val emptyTv = android.widget.TextView(this).apply {
                text = "No timed questions recorded."
                setTextColor(ContextCompat.getColor(context, R.color.eve_text_secondary))
                textSize = 13f
            }
            binding.layoutSlowestList.addView(emptyTv)
        }

        // Topic-wise accuracy list (weakest first)
        val topicAccs = TopicAccuracyHelper.aggregate(allItems)
        binding.layoutTopicList.removeAllViews()
        if (topicAccs.isNotEmpty()) {
            for (ta in topicAccs) {
                val tv = android.widget.TextView(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
                    }
                    text = "${ta.topic}: ${ta.accuracy}% (${ta.correct}/${ta.total} correct)"
                    setTextColor(
                        if (ta.accuracy >= 70.0) ContextCompat.getColor(context, R.color.eve_status_success)
                        else if (ta.accuracy >= 40.0) ContextCompat.getColor(context, R.color.eve_primary)
                        else ContextCompat.getColor(context, R.color.eve_status_error)
                    )
                    textSize = 13f
                }
                binding.layoutTopicList.addView(tv)
            }

            val weakest = topicAccs.first()
            if (weakest.accuracy < 100.0) {
                binding.btnPracticeWeakest.visibility = View.VISIBLE
                binding.btnPracticeWeakest.text = "Practice Weakest: ${weakest.topic}"
                binding.btnPracticeWeakest.setOnClickListener {
                    val practiceIntent = Intent(this, TestActivity::class.java).apply {
                        putExtra(Constants.EXTRA_EXAM_ID, currentExamId)
                        putExtra(Constants.EXTRA_EXAM_NAME, currentExamName)
                        putExtra(Constants.EXTRA_TOPIC, weakest.topic)
                    }
                    startActivity(practiceIntent)
                }
            } else {
                binding.btnPracticeWeakest.visibility = View.GONE
            }
        } else {
            val tv = android.widget.TextView(this).apply {
                text = "No topics tagged for these questions."
                setTextColor(ContextCompat.getColor(context, R.color.eve_text_secondary))
                textSize = 13f
            }
            binding.layoutTopicList.addView(tv)
            binding.btnPracticeWeakest.visibility = View.GONE
        }
    }

    private fun setupQuestionPalette() {
        paletteItems = allItems.mapIndexed { index, item ->
            val state = when {
                item.isCorrect -> PaletteState.CORRECT
                item.isAttempted -> PaletteState.WRONG
                else -> PaletteState.UNATTEMPTED
            }
            PaletteItem(number = index + 1, state = state, isActive = (index == selectedQuestionIndex))
        }
        paletteAdapter = QuestionPaletteAdapter { pos ->
            selectQuestion(pos)
        }
        binding.rvResultPalette.adapter = paletteAdapter
        paletteAdapter.submit(paletteItems)

        if (allItems.isNotEmpty()) {
            selectQuestion(selectedQuestionIndex)
        }
    }

    private fun selectQuestion(pos: Int) {
        if (pos !in allItems.indices) return
        selectedQuestionIndex = pos

        paletteItems = paletteItems.mapIndexed { idx, item ->
            item.copy(isActive = (idx == pos))
        }
        if (::paletteAdapter.isInitialized) {
            paletteAdapter.submit(paletteItems)
        }

        adapter.resetExpandedSolutions()

        binding.rvAnswers.visibility = View.VISIBLE
        binding.layoutEmptyFilter.visibility = View.GONE
        adapter.submit(listOf(allItems[pos]))
        binding.rvAnswers.scrollToPosition(0)
    }

    private fun setupFilters(correct: Int, wrong: Int, unattempted: Int) {
        binding.chipCorrect.text = "Right ($correct)"
        binding.chipWrong.text = "Wrong ($wrong)"
        binding.chipNotAttempted.text = "Unattempted ($unattempted)"

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyReviewFilters()
        }
    }

    private fun applyReviewFilters() {
        val showCorrect = binding.chipCorrect.isChecked
        val showWrong = binding.chipWrong.isChecked
        val showUnattempted = binding.chipNotAttempted.isChecked

        val showAll = !showCorrect && !showWrong && !showUnattempted

        val matchingIndices = allItems.indices.filter { idx ->
            val item = allItems[idx]
            when {
                showAll -> true
                showCorrect -> item.isCorrect
                showWrong -> item.isAttempted && !item.isCorrect
                showUnattempted -> !item.isAttempted
                else -> true
            }
        }

        if (matchingIndices.isEmpty()) {
            binding.rvAnswers.visibility = View.GONE
            binding.layoutEmptyFilter.visibility = View.VISIBLE
            val filterName = when {
                showCorrect -> "Right"
                showWrong -> "Wrong"
                showUnattempted -> "Unattempted"
                else -> "matching"
            }
            binding.tvEmptyFilterTitle.text = "No $filterName questions"
            binding.tvEmptyFilterSub.text = when {
                showWrong -> "Great job! You didn't get any questions wrong."
                showUnattempted -> "Great job! You attempted every question."
                else -> "Try selecting another filter above or tap any question on the palette."
            }
        } else {
            binding.rvAnswers.visibility = View.VISIBLE
            binding.layoutEmptyFilter.visibility = View.GONE
            val targetIndex = if (showAll) selectedQuestionIndex.coerceIn(allItems.indices) else matchingIndices.first()
            selectQuestion(targetIndex)
        }
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
            val sourceId = AttemptKey.sourceExamId(examId)
            if (sourceId.isNotBlank()) {
                try {
                    val exam = examRepo.getExam(sourceId)
                    if (exam != null && exam.cutoffs.isNotEmpty()) {
                        examCutoffs = exam.cutoffs
                    } else {
                        val snap = FirebaseFirestore.getInstance().collection("exams").document(sourceId).get().await()
                        val cMap = snap.get("cutoffs") as? Map<*, *>
                        if (cMap != null) {
                            examCutoffs = cMap.mapNotNull { (k, v) ->
                                val d = (v as? Number)?.toDouble() ?: v?.toString()?.toDoubleOrNull()
                                if (d != null && k != null) k.toString() to d else null
                            }.toMap()
                        }
                    }
                } catch (_: Exception) {}
            }
            setupCutoffSelector()

            // 3. Load Rank, Percentile, Best/Avg Score
            val isCounted = intent.getIntExtra("EXTRA_COUNTED", 1)
            if (isCounted == 0) {
                binding.tvRank.text = "Not ranked"
                binding.tvLeaderboardTabRank.text = "Not ranked"
                (binding.tvPercentile.parent as? View)?.visibility = View.GONE
                binding.tvTopperAvg.text = "Topper: -- • Average: --"
                val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)
                binding.tvHistoryBestAvg.text = "Score: $sStr"
                return@launch
            }

            if (examId.isNotBlank()) {
                try {
                    val statsResp = ApiClient.api.getLeaderboardStats(examId)
                    if (statsResp.success && statsResp.data != null) {
                        val stats = statsResp.data
                        val totalParticipants = stats.participants
                        val myRank = stats.myRank
                        val percentile = stats.myPercentile

                        binding.tvRank.text = if (myRank != null && totalParticipants > 0) "#$myRank / $totalParticipants" else "-- / --"
                        binding.tvLeaderboardTabRank.text = binding.tvRank.text

                        if (percentile != null) {
                            (binding.tvPercentile.parent as? View)?.visibility = View.VISIBLE
                            binding.tvPercentile.text = "${String.format(java.util.Locale.US, "%.1f", percentile)}%"
                            if (percentile >= 50.0 || myRank == 1) {
                                binding.tvPercentile.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.eve_status_success))
                                binding.tvRank.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.eve_status_success))
                            }
                        }

                        val topperStr = if (stats.topperScore % 1.0 == 0.0) stats.topperScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", stats.topperScore)
                        val avgStr = if (stats.averageScore % 1.0 == 0.0) stats.averageScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", stats.averageScore)
                        binding.tvTopperAvg.text = "Topper: $topperStr • Average: $avgStr"
                        binding.tvHistoryBestAvg.text = "Topper: $topperStr  •  Avg: $avgStr"
                        return@launch
                    }
                } catch (_: Exception) {}

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
                    binding.tvLeaderboardTabRank.text = binding.tvRank.text
                    binding.tvPercentile.text = "${String.format(java.util.Locale.US, "%.1f", percentile)}%"
                    if (percentile >= 50.0 || rank == 1) {
                        binding.tvPercentile.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.eve_status_success))
                        binding.tvRank.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.eve_status_success))
                    }

                    // Student's historical best and average
                    val myPastScores = snap.documents
                        .filter { it.getString("userId") == currentUid }
                        .mapNotNull { it.getDouble("score") }

                    val best = if (myPastScores.isNotEmpty()) maxOf(myPastScores.maxOrNull() ?: currentScore, currentScore) else currentScore
                    val avg = if (myPastScores.isNotEmpty()) myPastScores.average() else currentScore

                    val bestStr = if (best % 1.0 == 0.0) best.toInt().toString() else String.format(java.util.Locale.US, "%.1f", best)
                    val avgStr = if (avg % 1.0 == 0.0) avg.toInt().toString() else String.format(java.util.Locale.US, "%.1f", avg)
                    binding.tvHistoryBestAvg.text = "Best: $bestStr  •  Avg: $avgStr"
                    val topper = allScores.maxOrNull() ?: currentScore
                    val topperStr = if (topper % 1.0 == 0.0) topper.toInt().toString() else String.format(java.util.Locale.US, "%.1f", topper)
                    val avgAll = if (allScores.isNotEmpty()) allScores.average() else currentScore
                    val avgAllStr = if (avgAll % 1.0 == 0.0) avgAll.toInt().toString() else String.format(java.util.Locale.US, "%.1f", avgAll)
                    binding.tvTopperAvg.text = "Topper: $topperStr • Average: $avgAllStr"

                } catch (e: Exception) {
                    binding.tvRank.text = "#1 / 1"
                    binding.tvLeaderboardTabRank.text = binding.tvRank.text
                    binding.tvPercentile.text = "100.0%"
                    val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)
                    binding.tvHistoryBestAvg.text = "Best: $sStr  •  Avg: $sStr"
                    binding.tvTopperAvg.text = "Topper: $sStr • Average: $sStr"
                }
            } else {
                binding.tvRank.text = "#1 / 1"
                binding.tvLeaderboardTabRank.text = binding.tvRank.text
                binding.tvPercentile.text = "100.0%"
                val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)
                binding.tvHistoryBestAvg.text = "Best: $sStr  •  Avg: $sStr"
                binding.tvTopperAvg.text = "Topper: $sStr • Average: $sStr"
            }
        }
    }

    private fun setupCutoffSelector() {
        val defaultCategory = intent.getStringExtra(Constants.EXTRA_EXAM_CATEGORY)?.trim()?.uppercase() ?: "GENERAL"
        selectedCutoffCategory = when (defaultCategory) {
            "OBC" -> "OBC"
            "SC" -> "SC"
            "ST" -> "ST"
            "EWS" -> "EWS"
            else -> "General"
        }

        binding.rowCatGeneral.setOnClickListener { selectCutoffCategory("General") }
        binding.rowCatObc.setOnClickListener { selectCutoffCategory("OBC") }
        binding.rowCatSc.setOnClickListener { selectCutoffCategory("SC") }
        binding.rowCatSt.setOnClickListener { selectCutoffCategory("ST") }
        binding.rowCatEws.setOnClickListener { selectCutoffCategory("EWS") }

        updateCutoffUI()
    }

    private fun selectCutoffCategory(category: String) {
        selectedCutoffCategory = category
        updateCutoffUI()
    }

    private fun updateCutoffUI() {
        // 1. Update Hero Card
        binding.tvCutoffSelectedCategory.text = selectedCutoffCategory
        val cutoff = examCutoffs[selectedCutoffCategory]
        val scoreStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.2f", currentScore)
        binding.tvCutoffScore.text = "Score: $scoreStr"

        if (cutoff != null && cutoff > 0) {
            val cutoffStr = if (cutoff % 1.0 == 0.0) cutoff.toInt().toString() else cutoff.toString()
            binding.tvCutoffValue.text = "Cutoff: $cutoffStr"
            if (currentScore >= cutoff) {
                binding.tvCutoffVerdict.text = "Qualified ✓"
                binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.eve_status_success))
                val diff = currentScore - cutoff
                binding.tvCutoffRelationship.text = if (diff >= 0.05) {
                    "You cleared the $selectedCutoffCategory cutoff mark by +${String.format(java.util.Locale.US, "%.1f", diff)} marks."
                } else {
                    "You achieved the exact qualifying score for $selectedCutoffCategory."
                }
            } else {
                binding.tvCutoffVerdict.text = "Not Qualified ✗"
                binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.eve_status_error))
                val diff = cutoff - currentScore
                binding.tvCutoffRelationship.text = "You are ${String.format(java.util.Locale.US, "%.1f", diff)} marks below the $selectedCutoffCategory cutoff threshold."
            }
        } else {
            binding.tvCutoffValue.text = "Cutoff: Not Configured"
            binding.tvCutoffVerdict.text = "No Cutoff Set"
            binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.eve_text_secondary))
            binding.tvCutoffRelationship.text = "No qualifying cutoff mark is configured for the $selectedCutoffCategory category."
        }

        // 2. Update Categories List Rows
        updateCategoryRow(
            category = "General",
            tvSub = binding.tvSubGeneral,
            ivCheck = binding.ivCheckGeneral
        )
        updateCategoryRow(
            category = "OBC",
            tvSub = binding.tvSubObc,
            ivCheck = binding.ivCheckObc
        )
        updateCategoryRow(
            category = "SC",
            tvSub = binding.tvSubSc,
            ivCheck = binding.ivCheckSc
        )
        updateCategoryRow(
            category = "ST",
            tvSub = binding.tvSubSt,
            ivCheck = binding.ivCheckSt
        )
        updateCategoryRow(
            category = "EWS",
            tvSub = binding.tvSubEws,
            ivCheck = binding.ivCheckEws
        )
    }

    private fun updateCategoryRow(
        category: String,
        tvSub: android.widget.TextView,
        ivCheck: android.widget.ImageView
    ) {
        val isSelected = selectedCutoffCategory.equals(category, ignoreCase = true)
        ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

        val c = examCutoffs[category]
        if (c != null && c > 0) {
            val cStr = if (c % 1.0 == 0.0) c.toInt().toString() else c.toString()
            val qualified = currentScore >= c
            tvSub.text = if (qualified) "Cutoff: $cStr  •  Qualified ✓" else "Cutoff: $cStr  •  Not Qualified ✗"
            tvSub.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (qualified) R.color.eve_status_success else R.color.eve_status_error
                )
            )
        } else {
            tvSub.text = "No cutoff configured"
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.eve_text_secondary))
        }
    }

    private fun showReattemptDialog(examId: String, examName: String) {
        lifecycleScope.launch {
            val canReattempt = com.eve.app.util.AttemptLimitManager.canAttempt(this@ResultActivity, examId)
            if (!canReattempt) {
                MaterialAlertDialogBuilder(this@ResultActivity)
                    .setTitle("Attempt Limit Reached")
                    .setMessage("You have reached the maximum limit of 3 attempts for this test. Normal users can attempt each test at most 3 times.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val dialog = MaterialAlertDialogBuilder(this@ResultActivity)
                .setTitle("Reattempt this test?")
                .setMessage("Your previous result for \"$examName\" (score, answers and rank) will be permanently deleted and replaced by your new attempt. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear & Reattempt", null)
                .create()

            dialog.setOnShowListener {
                val confirmBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                confirmBtn.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.eve_status_error))

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
                                val timeLimit = if (intent.hasExtra(Constants.EXTRA_TIME_LIMIT)) {
                                    intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 60)
                                } else {
                                    try {
                                        examRepo.getExam(AttemptKey.sourceExamId(examId))?.timeLimitMinutes ?: 60
                                    } catch (_: Exception) { 60 }
                                }
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
                            if (!com.eve.app.util.NetworkUtil.isOnline(this@ResultActivity)) {
                                ResultDataHolder.clear()
                                HomeViewModel.markAttemptCleared(examId)
                                val timeLimit = if (intent.hasExtra(Constants.EXTRA_TIME_LIMIT)) {
                                    intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 60)
                                } else {
                                    try {
                                        examRepo.getExam(AttemptKey.sourceExamId(examId))?.timeLimitMinutes ?: 60
                                    } catch (_: Exception) { 60 }
                                }
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
                                AppBulletin.showError(this@ResultActivity, e.localizedMessage ?: "Failed to reset attempt")
                            }
                        }
                    }
                }
            }

            dialog.show()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ResultDataHolder.clear()
        }
    }
}
