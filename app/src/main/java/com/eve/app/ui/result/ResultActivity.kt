package com.eve.app.ui.result

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
import com.eve.app.data.remote.toUserFriendlyMessage
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
import com.eve.app.ui.common.ExpandableCardHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ResultActivity : EveBaseActivity() {

    private val viewModel: ResultViewModel by viewModels()
    private val expandedCardStates = mutableMapOf<String, Boolean>()
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
        savedInstanceState?.let { bundle ->
            listOf("card_perf_standing", "card_time_per_question", "card_slowest_questions", "card_topic_accuracy").forEach { key ->
                if (bundle.containsKey(key)) {
                    expandedCardStates[key] = bundle.getBoolean(key)
                }
            }
        }
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            close()
        }
        binding.btnBack?.setOnClickListener {
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
                                id = AttemptKey.canonicalQuestionId(genTest.id, idx),
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
                                    id = AttemptKey.canonicalQuestionId(live.id, idx),
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

        // Setup Collapsible Cards on Overview Screen
        setupExpandableCards()

        // Bottom Actions
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
        binding.layoutActionCluster.visibility = if (canReattempt) View.VISIBLE else View.GONE

        binding.btnHome.setOnClickListener { close() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        for ((key, value) in expandedCardStates) {
            outState.putBoolean(key, value)
        }
    }

    private fun setupExpandableCards() {
        ExpandableCardHelper(
            cardView = binding.cardPerformanceStanding,
            headerView = binding.headerPerformanceStanding,
            chevronView = binding.ivPerformanceStandingChevron,
            hintView = binding.tvPerformanceStandingHint,
            collapsedSummaryView = null,
            expandedContentView = binding.layoutPerformanceStandingExpanded,
            scrollView = binding.scrollResultContent,
            cardTitle = "Performance Standing",
            stateKey = "card_perf_standing",
            stateStore = expandedCardStates
        )

        ExpandableCardHelper(
            cardView = binding.cardTimePerQuestion,
            headerView = binding.headerTimePerQuestion,
            chevronView = binding.ivTimePerQuestionChevron,
            hintView = binding.tvTimePerQuestionHint,
            collapsedSummaryView = binding.tvTimePerQuestionCollapsed,
            expandedContentView = binding.layoutTimePerQuestionExpanded,
            scrollView = binding.scrollResultContent,
            cardTitle = "Time Per Question",
            stateKey = "card_time_per_question",
            stateStore = expandedCardStates
        )

        ExpandableCardHelper(
            cardView = binding.cardSlowestQuestions,
            headerView = binding.headerSlowestQuestions,
            chevronView = binding.ivSlowestChevron,
            hintView = binding.tvSlowestHint,
            collapsedSummaryView = binding.tvSlowestCollapsed,
            expandedContentView = binding.layoutSlowestExpanded,
            scrollView = binding.scrollResultContent,
            cardTitle = "Slowest Questions",
            stateKey = "card_slowest_questions",
            stateStore = expandedCardStates
        )

        ExpandableCardHelper(
            cardView = binding.cardTopicAccuracy,
            headerView = binding.headerTopicAccuracy,
            chevronView = binding.ivTopicChevron,
            hintView = binding.tvTopicHint,
            collapsedSummaryView = binding.tvTopicCollapsed,
            expandedContentView = binding.layoutTopicExpanded,
            scrollView = binding.scrollResultContent,
            cardTitle = "Topic-wise Accuracy",
            stateKey = "card_topic_accuracy",
            stateStore = expandedCardStates
        )
    }

    private fun setupTabLayout() {
        binding.tabLayoutResult.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                binding.sectionReview.visibility = if (pos == 0) View.VISIBLE else View.GONE
                binding.sectionOverview.visibility = if (pos == 1) View.VISIBLE else View.GONE
                binding.sectionLeaderboard.visibility = if (pos == 2) View.VISIBLE else View.GONE

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
        binding.tvTimePerQuestionCollapsed.text = "Average: ${avgSeconds}s / question"

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
        if (slowest.isNotEmpty()) {
            val firstSlow = slowest.first()
            val firstStatus = when {
                firstSlow.isCorrect -> "Correct"
                firstSlow.isAttempted -> "Wrong"
                else -> "Skipped"
            }
            val moreSuffix = if (slowest.size > 1) " (+${slowest.size - 1} more)" else ""
            binding.tvSlowestCollapsed.text = "Q${firstSlow.number}: ${firstSlow.timeTakenSeconds}s • $firstStatus$moreSuffix"
        } else {
            binding.tvSlowestCollapsed.text = "No timed questions recorded."
        }
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
        if (topicAccs.isNotEmpty()) {
            val weakest = topicAccs.first()
            binding.tvTopicCollapsed.text = "Weakest: ${weakest.topic} (${weakest.accuracy}%)"
        } else {
            binding.tvTopicCollapsed.text = "No topics tagged for these questions."
        }
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
                    }
                } catch (_: Exception) {}
            }
            setupCutoffSelector()

            // 3. Load Rank, Percentile, Best/Avg Score
            val isCounted = intent.getIntExtra("EXTRA_COUNTED", 1)
            if (isCounted == 0) {
                (binding.tvRank.parent as? View)?.visibility = View.GONE
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

                        if (myRank != null && totalParticipants > 0) {
                            (binding.tvRank.parent as? View)?.visibility = View.VISIBLE
                            binding.tvRank.text = "#$myRank / $totalParticipants"
                            binding.tvLeaderboardTabRank.text = binding.tvRank.text
                        } else {
                            (binding.tvRank.parent as? View)?.visibility = View.GONE
                            binding.tvLeaderboardTabRank.text = "Not ranked"
                        }

                        if (percentile != null) {
                            (binding.tvPercentile.parent as? View)?.visibility = View.VISIBLE
                            binding.tvPercentile.text = "${String.format(java.util.Locale.US, "%.1f", percentile)}%"
                        } else {
                            (binding.tvPercentile.parent as? View)?.visibility = View.GONE
                        }

                        if (stats.topperScore > 0 || stats.averageScore > 0) {
                            val topperStr = if (stats.topperScore % 1.0 == 0.0) stats.topperScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", stats.topperScore)
                            val avgStr = if (stats.averageScore % 1.0 == 0.0) stats.averageScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", stats.averageScore)
                            binding.tvTopperAvg.text = "Topper: $topperStr • Average: $avgStr"
                            binding.tvHistoryBestAvg.text = "Topper: $topperStr  •  Avg: $avgStr"
                        } else {
                            val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)
                            binding.tvTopperAvg.text = "Topper: $sStr • Average: $sStr"
                            binding.tvHistoryBestAvg.text = "Score: $sStr"
                        }
                        return@launch
                    }
                } catch (_: Exception) {}
            }

            // Fallback when stats are not available or call fails: hide rank row
            (binding.tvRank.parent as? View)?.visibility = View.GONE
            binding.tvLeaderboardTabRank.text = "Not ranked"
            (binding.tvPercentile.parent as? View)?.visibility = View.GONE
            val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)
            binding.tvHistoryBestAvg.text = "Score: $sStr"
            binding.tvTopperAvg.text = "Topper: $sStr • Average: $sStr"
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

        binding.tvCutoffSelectedCategory.setOnClickListener {
            showCategoryBottomSheet()
        }

        updateCutoffUI()
    }

    private fun selectCutoffCategory(category: String) {
        selectedCutoffCategory = category
        updateCutoffUI()
    }

    private fun showCategoryBottomSheet() {
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        dialog.setContentView(sheetView)

        data class CategoryRowItem(
            val row: View,
            val tvSub: TextView,
            val ivCheck: ImageView,
            val category: String
        )

        val rows = listOf(
            CategoryRowItem(
                sheetView.findViewById(R.id.rowSheetGeneral),
                sheetView.findViewById(R.id.tvSheetSubGeneral),
                sheetView.findViewById(R.id.ivSheetCheckGeneral),
                "General"
            ),
            CategoryRowItem(
                sheetView.findViewById(R.id.rowSheetObc),
                sheetView.findViewById(R.id.tvSheetSubObc),
                sheetView.findViewById(R.id.ivSheetCheckObc),
                "OBC"
            ),
            CategoryRowItem(
                sheetView.findViewById(R.id.rowSheetSc),
                sheetView.findViewById(R.id.tvSheetSubSc),
                sheetView.findViewById(R.id.ivSheetCheckSc),
                "SC"
            ),
            CategoryRowItem(
                sheetView.findViewById(R.id.rowSheetSt),
                sheetView.findViewById(R.id.tvSheetSubSt),
                sheetView.findViewById(R.id.ivSheetCheckSt),
                "ST"
            ),
            CategoryRowItem(
                sheetView.findViewById(R.id.rowSheetEws),
                sheetView.findViewById(R.id.tvSheetSubEws),
                sheetView.findViewById(R.id.ivSheetCheckEws),
                "EWS"
            )
        )

        for (item in rows) {
            val isSelected = selectedCutoffCategory.equals(item.category, ignoreCase = true)
            item.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            val c = examCutoffs[item.category]
            if (c != null && c > 0) {
                val cStr = if (c % 1.0 == 0.0) c.toInt().toString() else c.toString()
                val qualified = currentScore >= c
                item.tvSub.text = if (qualified) "Cutoff: $cStr  •  Qualified ✓" else "Cutoff: $cStr  •  Not Qualified ✗"
                item.tvSub.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (qualified) R.color.eve_status_success else R.color.eve_status_error
                    )
                )
            } else {
                item.tvSub.text = "No cutoff configured"
                item.tvSub.setTextColor(ContextCompat.getColor(this, R.color.eve_text_secondary))
            }

            item.row.setOnClickListener {
                selectCutoffCategory(item.category)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateCutoffUI() {
        // Update Hero Card
        binding.tvCutoffSelectedCategory.text = selectedCutoffCategory
        val cutoff = examCutoffs[selectedCutoffCategory]
        val scoreStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.2f", currentScore)
        binding.tvCutoffScore.text = "Score: $scoreStr"

        if (cutoff != null && cutoff > 0) {
            val cutoffStr = if (cutoff % 1.0 == 0.0) cutoff.toInt().toString() else cutoff.toString()
            binding.tvCutoffValue.text = "Cutoff: $cutoffStr"
            if (currentScore >= cutoff) {
                binding.tvCutoffVerdict.text = "Qualified ✓"
                binding.tvCutoffVerdict.setBackgroundResource(R.drawable.bg_tile_right)
                binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.eve_tile_right_text))
                val diff = currentScore - cutoff
                binding.tvCutoffRelationship.text = if (diff >= 0.05) {
                    "You cleared the $selectedCutoffCategory cutoff mark by +${String.format(java.util.Locale.US, "%.1f", diff)} marks."
                } else {
                    "You achieved the exact qualifying score for $selectedCutoffCategory."
                }
            } else {
                binding.tvCutoffVerdict.text = "Not Qualified ✗"
                binding.tvCutoffVerdict.setBackgroundResource(R.drawable.bg_tile_wrong)
                binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.eve_tile_wrong_text))
                val diff = cutoff - currentScore
                binding.tvCutoffRelationship.text = "You are ${String.format(java.util.Locale.US, "%.1f", diff)} marks below the $selectedCutoffCategory cutoff threshold."
            }
        } else {
            binding.tvCutoffValue.text = "Cutoff: Not Configured"
            binding.tvCutoffVerdict.text = "No Cutoff Set"
            binding.tvCutoffVerdict.setBackgroundResource(R.drawable.bg_tile_surface2)
            binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(this, R.color.eve_text_secondary))
            binding.tvCutoffRelationship.text = "No qualifying cutoff mark is configured for the $selectedCutoffCategory category."
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
                                AppBulletin.showError(this@ResultActivity, e.toUserFriendlyMessage())
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
