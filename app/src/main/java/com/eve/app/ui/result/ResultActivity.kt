package com.eve.app.ui.result

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import android.content.DialogInterface
import com.eve.app.designsystem.TelegramNativeAlertDialog
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
import com.eve.app.util.SecurityHelper
import com.eve.app.util.ShareCardHelper
import com.eve.app.util.TopicAccuracyHelper
import com.eve.app.ui.common.ExpandableCardHelper
import kotlinx.coroutines.launch

class ResultActivity : EveBaseActivity() {

    private val viewModel: ResultViewModel by viewModels()
    private lateinit var allItems: List<AnswerItem>
    private lateinit var binding: ActivityResultBinding
    private var reattemptLimitDialog: TelegramNativeAlertDialog? = null
    private var reattemptConfirmDialog: TelegramNativeAlertDialog? = null
    private var categoryBottomSheet: com.eve.app.designsystem.TelegramBottomSheet? = null

    private var currentExamName: String = ""
    private var currentExamId: String = ""

    private val adapter = AnswerAdapter(
        onReport = { item ->
            if (resultIdentityCurrent) {
                ReportQuestionDialog.show(
                    activity = this,
                    item = item,
                    examId = AttemptKey.sourceExamId(currentExamId),
                    examName = currentExamName
                )
            }
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
    private var canReattempt = false

    private var resultOwnerUid = ""
    private val resultIdentityCurrent: Boolean
        get() = !isFinishing && !isDestroyed && resultOwnerUid.isNotBlank() &&
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == resultOwnerUid
    private val resultOwnerListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
        if (resultOwnerUid.isNotBlank() && auth.currentUser?.uid != resultOwnerUid) {
            if (::binding.isInitialized) binding.root.visibility = android.view.View.INVISIBLE
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultOwnerUid = intent.getStringExtra(Constants.EXTRA_RESULT_OWNER_UID).orEmpty()
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        if (resultOwnerUid.isBlank() || auth.currentUser?.uid != resultOwnerUid) { finish(); return }
        auth.addAuthStateListener(resultOwnerListener)
        if (isFinishing || auth.currentUser?.uid != resultOwnerUid) { finish(); return }
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            close()
        }
        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) {
            close()
        }

        currentExamId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME)
        currentExamName = examName.orEmpty()
        val attemptDate = intent.getStringExtra(Constants.EXTRA_ATTEMPT_DATE)

        if (viewModel.allItems.isEmpty()) {
            val incoming = ResultDataHolder.consumeAnswers(resultOwnerUid, currentExamId)
            if (incoming.isNotEmpty()) {
                viewModel.initAnswers(incoming)
            } else if (currentExamId.isNotBlank()) {
                val cached = HomeViewModel.getCachedAttempt(currentExamId, resultOwnerUid)
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

                    if (!resultIdentityCurrent) return@launch
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
                        if (!resultIdentityCurrent) return@launch
                        allItems = enriched
                        viewModel.updateAnswers(enriched)
                        selectQuestion(selectedQuestionIndex)
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) {}
            }
        }

        if (!currentExamName.isBlank()) {
            binding.tvResultTitle.text = currentExamName
        }

        if (fromHistory) {
            // Telegram ActionBar.java:1445-1452: the title is 18sp on phones
            // (20sp tablets) only when a navigation subtitle is displayed.
            androidx.core.widget.TextViewCompat.setTextAppearance(
                binding.tvResultTitle, R.style.TextAppearance_Eve_Telegram_NavigationTitle_WithSubtitle
            )
            binding.tvResultSubtitle.visibility = View.VISIBLE
            binding.tvResultSubtitle.text = if (!attemptDate.isNullOrBlank()) {
                "Attempted on $attemptDate"
            } else {
                "Historical Attempt"
            }
        } else {
            androidx.core.widget.TextViewCompat.setTextAppearance(
                binding.tvResultTitle, R.style.TextAppearance_Eve_Telegram_NavigationTitle
            )
            binding.tvResultSubtitle.visibility = View.GONE
        }

        canReattempt = intent.getBooleanExtra(Constants.EXTRA_CAN_REATTEMPT, false)
        binding.btnReattempt.visibility = if (canReattempt) View.VISIBLE else View.GONE
        if (canReattempt) {
            com.eve.app.designsystem.DesignPress.bind(binding.btnReattempt) {
                showReattemptDialog(currentExamId, if (currentExamName.isNotBlank()) currentExamName else "this test")
            }
        }

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
                        if (!resultIdentityCurrent) return@launch
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
                    } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                    catch (_: Exception) {}
                }
            }
        }

        // Section 1: Overview Breakdown Metrics
        binding.tvCorrectCount.setSlotValue(correct)
        binding.tvWrongCount.setSlotValue(wrong)
        binding.tvUnattemptedCount.setSlotValue(unattempted)

        val attempted = correct + wrong
        val accuracy = if (attempted > 0) (correct * 100.0 / attempted) else 0.0
        val accFormatted = String.format(java.util.Locale.US, "%.1f", accuracy)
        binding.tvAccuracy.text = "$accFormatted%"

        val scorePct = if (total > 0) (currentScore * 100.0 / total).coerceAtLeast(0.0) else 0.0
        binding.tvScorePercentage.text = String.format(java.util.Locale.US, "%.1f%% Score", scorePct)

        val displayedScore = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString()
            else String.format(java.util.Locale.US, "%.2f", currentScore)
        binding.tvScore.text = "$displayedScore / $total"
        binding.tvStats.text = "Correct: $correct   Wrong: $wrong   Unattempted: $unattempted"

        // Share Card Button
        com.eve.app.designsystem.DesignPress.bind(binding.btnShare)
        binding.btnShare.setOnClickListener {
            if (!resultIdentityCurrent) return@setOnClickListener
            val scoreStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.2f", currentScore)
            val scorePctStr = String.format(java.util.Locale.US, "%.1f%% Score", scorePct)
            val rankText = binding.tvRank.text.toString()
            val examTitle = if (currentExamName.isNotBlank()) currentExamName else "Mock Test"
            val shareSummary = "I scored $scoreStr / $total ($scorePctStr) on $examTitle with $accFormatted% accuracy! $rankText via EVE."

            com.eve.app.designsystem.TelegramShareAlert.show(
                context = this,
                title = examTitle,
                subtitle = "Score: $scoreStr/$total ($scorePctStr) • $accFormatted% Accuracy",
                shareText = shareSummary
            )
        }

        // Setup Answers RecyclerView (Section 1: Answer Review)
        binding.rvAnswers.layoutManager = LinearLayoutManager(this)
        binding.rvAnswers.adapter = adapter
        adapter.setHindi(LanguageManager.isHindi(this))
        adapter.setShowTimeInsight(!fromHistory)

        // Setup Segmented Navigation Tabs (Overview | Review | Leaderboard)
        setupTabLayout()

        // Setup Question Palette Navigation (Single Question Isolation)
        setupQuestionPalette()

        // Setup Filter Chips (Right, Wrong, Unattempted)
        setupFilters(correct, wrong, unattempted)

        // Setup Overview Analytics
        setupAnalysisSection(total, correct, wrong, accuracy)

        // Setup Cutoff / Performance
        loadDepthStatsAndCutoffs(currentExamId)

        // Setup Tappable Overview Detail Cards (Statistics, Performance, Analytics)
        setupDetailCardClicks(total, correct, wrong, unattempted, accuracy)

        // Leaderboard Tab Actions
        com.eve.app.designsystem.DesignPress.bind(binding.btnOpenLeaderboard)
        binding.btnOpenLeaderboard.setOnClickListener {
            if (!resultIdentityCurrent) return@setOnClickListener
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
    }

    private fun setupTabLayout() {
        ResultTabs.bind(binding) { selectQuestion(selectedQuestionIndex) }
    }

    private fun setupAnalysisSection(
        total: Int,
        correct: Int,
        wrong: Int,
        accuracy: Double
    ) {
        val attempted = correct + wrong
        val attemptedPct = if (total > 0) (attempted * 100.0 / total) else 0.0
        binding.tvAnalysisAttempted.text = "${String.format(java.util.Locale.US, "%.1f", attemptedPct)}%"
        binding.tvAnalysisAccuracy.text = "${String.format(java.util.Locale.US, "%.1f", accuracy)}%"
        binding.gaugeTopicAccuracy.setPercentage(accuracy.toFloat())

        val timedItems = allItems.filter { it.timeTakenSeconds > 0 }
        val avgSeconds = if (timedItems.isNotEmpty()) {
            kotlin.math.round(timedItems.map { it.timeTakenSeconds }.average()).toLong()
        } else {
            0L
        }
        binding.tvAnalysisAvgTime.text = "${avgSeconds}s"
    }

    private fun setupDetailCardClicks(
        total: Int,
        correct: Int,
        wrong: Int,
        unattempted: Int,
        accuracy: Double
    ) {
        val openStats = View.OnClickListener {
            if (!ResultDataHolder.setDetailItems(allItems, resultOwnerUid, currentExamId)) return@OnClickListener
            ResultDetailActivity.launch(
                context = this,
                ownerUid = resultOwnerUid,
                type = ResultDetailActivity.TYPE_STATISTICS,
                examId = currentExamId,
                examName = currentExamName,
                score = currentScore,
                total = total,
                correct = correct,
                wrong = wrong,
                unattempted = unattempted,
                accuracy = accuracy,
                rank = binding.tvRank.text.toString(),
                percentile = binding.tvPercentile.text.toString(),
                topperAvg = binding.tvTopperAvg.text.toString()
            )
        }
        com.eve.app.designsystem.DesignPress.bind(binding.headerOverviewStatistics)
        binding.headerOverviewStatistics.setOnClickListener(openStats)
        com.eve.app.designsystem.DesignPress.bind(binding.rowOverviewStatisticsTiles)
        binding.rowOverviewStatisticsTiles.setOnClickListener(openStats)

        val openPerf = View.OnClickListener {
            if (!ResultDataHolder.setDetailItems(allItems, resultOwnerUid, currentExamId)) return@OnClickListener
            ResultDetailActivity.launch(
                context = this,
                ownerUid = resultOwnerUid,
                type = ResultDetailActivity.TYPE_PERFORMANCE,
                examId = currentExamId,
                examName = currentExamName,
                score = currentScore,
                total = total,
                correct = correct,
                wrong = wrong,
                unattempted = unattempted,
                accuracy = accuracy,
                rank = binding.tvRank.text.toString(),
                percentile = binding.tvPercentile.text.toString(),
                topperAvg = binding.tvTopperAvg.text.toString()
            )
        }
        com.eve.app.designsystem.DesignPress.bind(binding.headerOverviewPerformance)
        binding.headerOverviewPerformance.setOnClickListener(openPerf)
        com.eve.app.designsystem.DesignPress.bind(binding.cardPerformanceStanding)
        binding.cardPerformanceStanding.setOnClickListener(openPerf)

        val openAnalytics = View.OnClickListener {
            if (!ResultDataHolder.setDetailItems(allItems, resultOwnerUid, currentExamId)) return@OnClickListener
            ResultDetailActivity.launch(
                context = this,
                ownerUid = resultOwnerUid,
                type = ResultDetailActivity.TYPE_ANALYTICS,
                examId = currentExamId,
                examName = currentExamName,
                score = currentScore,
                total = total,
                correct = correct,
                wrong = wrong,
                unattempted = unattempted,
                accuracy = accuracy,
                rank = binding.tvRank.text.toString(),
                percentile = binding.tvPercentile.text.toString(),
                topperAvg = binding.tvTopperAvg.text.toString()
            )
        }
        com.eve.app.designsystem.DesignPress.bind(binding.headerOverviewAnalytics)
        binding.headerOverviewAnalytics.setOnClickListener(openAnalytics)
        com.eve.app.designsystem.DesignPress.bind(binding.cardOverviewAnalytics)
        binding.cardOverviewAnalytics.setOnClickListener(openAnalytics)
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
        com.eve.app.ui.common.TelegramHorizontalScrollLayoutManager.install(binding.rvResultPalette)
        binding.rvResultPalette.adapter = paletteAdapter
        paletteAdapter.submit(paletteItems)

        if (allItems.isNotEmpty()) {
            selectQuestion(selectedQuestionIndex)
        }
    }

    private fun selectQuestion(pos: Int) {
        if (!resultIdentityCurrent || pos !in allItems.indices) return
        selectedQuestionIndex = pos

        paletteItems = paletteItems.mapIndexed { idx, item ->
            item.copy(isActive = (idx == pos))
        }
        if (::paletteAdapter.isInitialized) {
            paletteAdapter.submit(paletteItems)
            binding.rvResultPalette.smoothScrollToPosition(pos)
        }

        adapter.resetExpandedSolutions()

        binding.rvAnswers.visibility = View.VISIBLE
        binding.layoutEmptyFilter.visibility = View.GONE
        adapter.submit(listOf(allItems[pos]))
        binding.rvAnswers.scrollToPosition(0)
    }

    private fun setupFilters(correct: Int, wrong: Int, unattempted: Int) {
        (binding.chipGroupFilter.parent as? View)?.let {
            com.eve.app.ui.common.TelegramHorizontalScrollLayoutManager.install(it)
        }
        binding.chipCorrect.text = "Right ($correct)"
        binding.chipWrong.text = "Wrong ($wrong)"
        binding.chipNotAttempted.text = "Unattempted ($unattempted)"

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyReviewFilters()
        }
    }

    private fun applyReviewFilters() {
        if (!resultIdentityCurrent) return
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
            if (!resultIdentityCurrent) return@launch
            // 1. Load Question Stats for Accuracy & Overtime
            try {
                val qIds = allItems.map { it.questionId }.filter { it.isNotBlank() }
                val stats = viewModel.getOrFetchQuestionStats(qIds)
                if (!resultIdentityCurrent) return@launch
                loadedQuestionStats = stats
                adapter.setQuestionStats(stats)
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) {}
            if (!resultIdentityCurrent) return@launch

            // 2. Load Cutoffs & Exam Info
            val sourceId = AttemptKey.sourceExamId(examId)
            if (sourceId.isNotBlank()) {
                try {
                    val exam = examRepo.getExam(sourceId)
                    if (!resultIdentityCurrent) return@launch
                    if (exam != null && exam.cutoffs.isNotEmpty()) {
                        examCutoffs = exam.cutoffs
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) {}
            }
            if (!resultIdentityCurrent) return@launch
            setupCutoffSelector()

            // 3. Load Rank, Percentile, Best/Avg Score
            val isCounted = intent.getIntExtra("EXTRA_COUNTED", 1)
            if (isCounted == 0) {
                (binding.tvRank.parent as? View)?.visibility = View.GONE
                binding.tvLeaderboardTabRank.text = "Not ranked"
                binding.tvLeaderboardTabSubtitle.text = "This attempt is not counted on the official leaderboard."
                binding.layoutLeaderboardChips.visibility = View.GONE
                (binding.tvPercentile.parent as? View)?.visibility = View.GONE
                binding.tvTopperAvg.text = "Topper: -- • Average: --"
                val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)
                binding.tvHistoryBestAvg.text = "Score: $sStr"
                return@launch
            }

            if (examId.isNotBlank()) {
                try {
                    val statsResp = ApiClient.api.getLeaderboardStats(examId)
                    if (!resultIdentityCurrent) return@launch
                    if (statsResp.success && statsResp.data != null) {
                        val stats = statsResp.data
                        val totalParticipants = stats.participants
                        val myRank = stats.myRank
                        val percentile = stats.myPercentile
                        val sStr = if (currentScore % 1.0 == 0.0) currentScore.toInt().toString() else String.format(java.util.Locale.US, "%.1f", currentScore)

                        if (myRank != null && totalParticipants > 0) {
                            (binding.tvRank.parent as? View)?.visibility = View.VISIBLE
                            binding.tvRank.text = "#$myRank / $totalParticipants"
                            binding.tvLeaderboardTabRank.text = "#$myRank"
                            binding.tvLeaderboardTabSubtitle.text = if (totalParticipants == 1) {
                                "You're the first on the board!"
                            } else {
                                "of $totalParticipants students"
                            }
                            binding.layoutLeaderboardChips.visibility = View.VISIBLE
                            binding.tvLeaderboardScoreChip.text = "Score: $sStr"
                            if (percentile != null) {
                                binding.tvLeaderboardPercentileChip.visibility = View.VISIBLE
                                binding.tvLeaderboardPercentileChip.text = "Percentile: ${String.format(java.util.Locale.US, "%.1f", percentile)}%"
                            } else {
                                binding.tvLeaderboardPercentileChip.visibility = View.GONE
                            }
                        } else {
                            (binding.tvRank.parent as? View)?.visibility = View.GONE
                            binding.tvLeaderboardTabRank.text = "Not ranked"
                            binding.tvLeaderboardTabSubtitle.text = "No rank data available yet"
                            binding.layoutLeaderboardChips.visibility = View.GONE
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
                            binding.tvTopperAvg.text = "Topper: $sStr • Average: $sStr"
                            binding.tvHistoryBestAvg.text = "Score: $sStr"
                        }
                        return@launch
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) {}
            }
            if (!resultIdentityCurrent) return@launch

            // Fallback when stats are not available or call fails: hide rank row
            (binding.tvRank.parent as? View)?.visibility = View.GONE
            binding.tvLeaderboardTabRank.text = "Not ranked"
            binding.tvLeaderboardTabSubtitle.text = "Leaderboard stats currently unavailable"
            binding.layoutLeaderboardChips.visibility = View.GONE
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

        com.eve.app.designsystem.DesignPress.bind(binding.tvCutoffSelectedCategory)
        binding.tvCutoffSelectedCategory.setOnClickListener {
            if (resultIdentityCurrent) showCategoryBottomSheet()
        }

        updateCutoffUI()
    }

    private fun selectCutoffCategory(category: String) {
        selectedCutoffCategory = category
        updateCutoffUI()
    }

    private fun showCategoryBottomSheet() {
        if (!resultIdentityCurrent) return
        categoryBottomSheet?.dismiss()
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
        val dialog = com.eve.app.designsystem.TelegramBottomSheet.Builder(this)
            .setCustomView(sheetView)
            .create()
        categoryBottomSheet = dialog
        dialog.setOnDismissListener {
            if (categoryBottomSheet === dialog) categoryBottomSheet = null
        }
        dialog.show()

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

        val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(this)
        val selectorColor = palette.actionSelector
        val rowSelector = {
            com.eve.app.designsystem.TelegramThemeSelectors.getRoundRectSelectorDrawable(
                8 * resources.displayMetrics.density,
                selectorColor
            )
        }

        for (item in rows) {
            item.row.background = rowSelector()
            val isSelected = selectedCutoffCategory.equals(item.category, ignoreCase = true)
            if (item.ivCheck is com.eve.app.ui.common.TelegramCheckBox2) {
                item.ivCheck.visibility = View.VISIBLE
                item.ivCheck.setChecked(isSelected, animated = false)
            } else {
                item.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
                item.ivCheck.imageTintList = android.content.res.ColorStateList.valueOf(palette.accent)
            }

            val c = examCutoffs[item.category]
            if (c != null && c > 0) {
                val cStr = if (c % 1.0 == 0.0) c.toInt().toString() else c.toString()
                val qualified = currentScore >= c
                item.tvSub.text = if (qualified) "Cutoff: $cStr  •  Qualified ✓" else "Cutoff: $cStr  •  Not Qualified ✗"
                item.tvSub.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (qualified) R.color.system_green_text else R.color.system_red_text
                    )
                )
            } else {
                item.tvSub.text = "No cutoff configured"
                item.tvSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }

            item.row.setOnClickListener {
                if (resultIdentityCurrent) selectCutoffCategory(item.category)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateCutoffUI() {
        ResultCutoffPresentation.bind(binding, selectedCutoffCategory, currentScore, examCutoffs)
    }

    private fun showReattemptDialog(examId: String, examName: String) {
        if (isFinishing || isDestroyed || reattemptConfirmDialog?.isShowing == true || reattemptLimitDialog?.isShowing == true) return
        lifecycleScope.launch {
            val canReattempt = com.eve.app.util.AttemptLimitManager.canAttempt(this@ResultActivity, examId)
            if (isFinishing || isDestroyed) return@launch
            if (!resultIdentityCurrent) return@launch
            if (!canReattempt) {
                val limitDialog = TelegramNativeAlertDialog.Builder(this@ResultActivity)
                    .setTitle("Attempt Limit Reached")
                    .setMessage("You have reached the maximum limit of 3 attempts for this test. Normal users can attempt each test at most 3 times.")
                    .setPositiveButton("OK", null)
                    .create()
                limitDialog.setOnDismissListener { dismissed ->
                    if (reattemptLimitDialog === dismissed) reattemptLimitDialog = null
                }
                limitDialog.show()
                reattemptLimitDialog = limitDialog
                return@launch
            }

            val dialog = TelegramNativeAlertDialog.Builder(this@ResultActivity)
                .setTitle("Reattempt this test?")
                .setMessage("A new attempt starts with no selected answers. Your previous result and history are preserved.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reattempt", null)
                .create()

            dialog.setOnShowListener {
                val confirmBtn = requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))
                val cancelBtn = requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE))
                confirmBtn.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.system_red_text))

                cancelBtn.setOnClickListener {
                    dialog.dismiss()
                }

                confirmBtn.setOnClickListener {
                    if (!dialog.isShowing || !confirmBtn.isEnabled || !resultIdentityCurrent) return@setOnClickListener
                    confirmBtn.isEnabled = false
                    cancelBtn.isEnabled = false
                    confirmBtn.text = "Resetting..."

                    lifecycleScope.launch {
                        try {
                            val response = ApiClient.apiService.resetAttemptPost(mapOf("examId" to examId))
                            if (!resultIdentityCurrent) return@launch
                            if (response.success) {
                                com.eve.app.data.local.TestSessionStore(this@ResultActivity).clearSession(examId)
                                ResultDataHolder.clear(resultOwnerUid, currentExamId)
                                HomeViewModel.markAttemptCleared(examId, resultOwnerUid)
                                val timeLimit = if (intent.hasExtra(Constants.EXTRA_TIME_LIMIT)) {
                                    intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 60)
                                } else {
                                    try {
                                        examRepo.getExam(AttemptKey.sourceExamId(examId))?.timeLimitMinutes ?: 60
                                    } catch (_: Exception) { 60 }
                                }
                                // Exam metadata may suspend after the reset receipt; never start
                                // a new test for a replacement Firebase account.
                                if (!resultIdentityCurrent) return@launch
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
                                confirmBtn.text = "Reattempt"
                                AppBulletin.showError(this@ResultActivity, response.error ?: "Failed to reset attempt")
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) {
                            confirmBtn.isEnabled = true
                            cancelBtn.isEnabled = true
                            confirmBtn.text = "Reattempt"
                            AppBulletin.showError(this@ResultActivity, e.toUserFriendlyMessage(false))
                        }
                    }
                }
            }

            dialog.setOnDismissListener { dismissed ->
                if (reattemptConfirmDialog === dismissed) reattemptConfirmDialog = null
            }
            dialog.show()
            reattemptConfirmDialog = dialog
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

    override fun onStop() {
        categoryBottomSheet?.dismiss()
        categoryBottomSheet = null
        reattemptLimitDialog?.dismiss()
        reattemptLimitDialog = null
        reattemptConfirmDialog?.dismiss()
        reattemptConfirmDialog = null
        super.onStop()
    }

    override fun onDestroy() {
        categoryBottomSheet?.dismiss()
        categoryBottomSheet = null
        reattemptLimitDialog?.dismiss()
        reattemptLimitDialog = null
        reattemptConfirmDialog?.dismiss()
        reattemptConfirmDialog = null
        com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(resultOwnerListener)
        super.onDestroy()
        if (isFinishing) {
            ResultDataHolder.clear(resultOwnerUid, currentExamId)
        }
    }

}
