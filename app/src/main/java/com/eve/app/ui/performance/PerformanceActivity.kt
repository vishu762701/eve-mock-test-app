package com.eve.app.ui.performance

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eve.app.R
import com.eve.app.data.model.PerformanceData
import com.eve.app.data.model.TopicStat
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.LineProgressView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Performance Analytics screen powered by real ported Telegram UI components:
 * ActionBar, EmptyTextProgressView, HeaderCell, TextCell, TextDetailSettingsCell,
 * LineProgressView, ShadowSectionCell, TextInfoPrivacyCell, and ButtonWithCounterView,
 * paired with Eve's interactive ScoreTrendChartView.
 */
class PerformanceActivity : EveBaseActivity() {

    companion object {
        var cachedPerformanceData: PerformanceData? = null
    }

    private val viewModel: PerformanceViewModel by viewModels()

    private var hasEmptyPlayed = false
    private var lastRenderedData: PerformanceData? = null
    private val performanceAuth = FirebaseAuth.getInstance()
    private var performanceOwnerUid: String? = null
    private fun hasPerformanceOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(
            performanceOwnerUid, performanceAuth.currentUser?.uid)
    private val performanceAuthListener = FirebaseAuth.AuthStateListener {
        if (performanceOwnerUid != null && !hasPerformanceOwner()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var scrollView: ScrollView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    private lateinit var attemptsCell: TextCell
    private lateinit var accuracyCell: TextCell
    private lateinit var questionsCell: TextCell
    private lateinit var chartTrend: ScoreTrendChartView
    private lateinit var weakSectionContainer: LinearLayout
    private lateinit var weakRowsContainer: LinearLayout
    private lateinit var topicsRowsContainer: LinearLayout
    private lateinit var noTopicsText: TextInfoPrivacyCell

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUid = performanceAuth.currentUser?.uid
        performanceOwnerUid = if (savedInstanceState == null) currentUid
            else savedInstanceState.getString("performance_owner_uid")
        if (!hasPerformanceOwner()) { finish(); return }
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false

        Theme.syncFromEvePalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Performance")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        root.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // 2. Scrollable Telegram sections
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        val sections = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(
            sections,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Overview Section
        sections.addView(HeaderCell(this).apply {
            setText("Overview")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        attemptsCell = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        sections.addView(attemptsCell, matchWrap())

        accuracyCell = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        sections.addView(accuracyCell, matchWrap())

        questionsCell = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        sections.addView(questionsCell, matchWrap())

        sections.addView(ShadowSectionCell(this), matchWrap())

        // Score Trend Section
        sections.addView(HeaderCell(this).apply {
            setText("Score Trend")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        val chartCard = FrameLayout(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, AndroidUtilities.dp(16f))
        }
        chartTrend = ScoreTrendChartView(this)
        chartCard.addView(
            chartTrend,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(200f)
            )
        )
        sections.addView(chartCard, matchWrap())
        sections.addView(ShadowSectionCell(this), matchWrap())

        // Focus Areas (Weak Topics) Section
        weakSectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        weakSectionContainer.addView(HeaderCell(this).apply {
            setText("Focus Areas")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
        weakRowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        weakSectionContainer.addView(weakRowsContainer, matchWrap())
        weakSectionContainer.addView(ShadowSectionCell(this), matchWrap())
        sections.addView(weakSectionContainer, matchWrap())

        // Topic Breakdown Section
        sections.addView(HeaderCell(this).apply {
            setText("Topic Breakdown")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
        topicsRowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        sections.addView(topicsRowsContainer, matchWrap())

        noTopicsText = TextInfoPrivacyCell(this).apply {
            setText("Performance metrics are calculated from your submitted mock test attempts.")
        }
        sections.addView(noTopicsText, matchWrap())

        contentFrame.addView(
            scrollView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        // 3. Real Telegram EmptyTextProgressView + Retry ButtonWithCounterView
        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("No insights available yet\nYour performance insights will appear here after you submit a test")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        retryButton = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Retry", false)
            visibility = View.GONE
            setOnClickListener { viewModel.load() }
        }
        contentFrame.addView(
            retryButton,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                48f,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                24f,
                0f,
                24f,
                32f
            )
        )

        cachedPerformanceData?.let {
            if (it.totalAttempts > 0) {
                lastRenderedData = it
                emptyView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                renderData(it)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPerformanceOwner()) { finish(); return }
        performanceAuth.addAuthStateListener(performanceAuthListener)
    }

    override fun onStop() {
        performanceAuth.removeAuthStateListener(performanceAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasPerformanceOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putString("performance_owner_uid", performanceOwnerUid)
    }

    private fun render(state: UiState<PerformanceData>) {
        if (!hasPerformanceOwner()) { finish(); return }
        when (state) {
            is UiState.Loading -> {
                retryButton.visibility = View.GONE
                if (lastRenderedData == null) {
                    scrollView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.showProgress()
                }
            }
            is UiState.Success -> {
                retryButton.visibility = View.GONE
                val data = state.data
                cachedPerformanceData = data
                lastRenderedData = data
                if (data.totalAttempts == 0) {
                    hasEmptyPlayed = true
                    actionBar.setSubtitle(null)
                    scrollView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No insights available yet\nYour performance insights will appear here after you submit a test")
                    emptyView.showTextView()
                } else {
                    hasEmptyPlayed = false
                    emptyView.visibility = View.GONE
                    scrollView.visibility = View.VISIBLE
                    renderData(data)
                }
            }
            is UiState.Error -> {
                if (lastRenderedData != null) {
                    scrollView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    AppBulletin.showError(this@PerformanceActivity, "Couldn't refresh performance: ${state.message}")
                } else {
                    scrollView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    emptyView.setText("Something went wrong\n${state.message}")
                    emptyView.showTextView()
                }
            }
        }
    }

    private fun renderData(data: PerformanceData) {
        actionBar.setSubtitle("${data.overallAccuracy}% overall accuracy")
        attemptsCell.setTextAndValue("Total Attempts", data.totalAttempts.toString(), true)
        accuracyCell.setTextAndValue("Overall Accuracy", "${data.overallAccuracy}%", true)
        questionsCell.setTextAndValue("Questions Solved", data.totalQuestions.toString(), false)

        chartTrend.submit(data.scoreTrend)

        // Focus Areas (Weak Topics)
        val weak = data.weakTopics
        weakRowsContainer.removeAllViews()
        if (weak.isEmpty()) {
            weakSectionContainer.visibility = View.GONE
        } else {
            weakSectionContainer.visibility = View.VISIBLE
            weak.forEachIndexed { idx, stat ->
                val cell = TextDetailSettingsCell(this).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    setTextAndValue(
                        stat.topic,
                        "${stat.accuracy}% accuracy (${stat.correct}/${stat.total} correct)",
                        idx < weak.lastIndex
                    )
                }
                weakRowsContainer.addView(cell, matchWrap())
            }
        }

        // Topic Breakdown with real Telegram LineProgressView
        topicsRowsContainer.removeAllViews()
        if (data.topicStats.isEmpty()) {
            noTopicsText.setText("No topic tags were recorded in your attempts yet.")
        } else {
            noTopicsText.setText("Topics are ordered from lowest to highest accuracy so you know what to revise next.")
            data.topicStats.forEachIndexed { idx, stat ->
                topicsRowsContainer.addView(buildTopicProgressRow(stat, idx < data.topicStats.lastIndex), matchWrap())
            }
        }
    }

    private fun buildTopicProgressRow(stat: TopicStat, showDivider: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = AndroidUtilities.dp(21f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, if (showDivider) 0 else padV)
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }

        val topLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvTopic = TextView(this).apply {
            text = stat.topic
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            typeface = AndroidUtilities.bold()
        }
        topLine.addView(tvTopic, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val accentColor = when {
            stat.accuracy >= 70 -> Theme.getColor(Theme.key_windowBackgroundWhiteGreenText2)
            stat.accuracy >= 40 -> Theme.getColor(Theme.key_statisticChartLine_orange)
            else -> Theme.getColor(Theme.key_text_RedRegular)
        }

        val tvPercent = TextView(this).apply {
            text = "${stat.accuracy}%"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(accentColor)
            typeface = AndroidUtilities.bold()
        }
        topLine.addView(tvPercent, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        row.addView(topLine, matchWrap())

        val tvSub = TextView(this).apply {
            text = "${stat.correct}/${stat.total} correct"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2))
            setPadding(0, AndroidUtilities.dp(2f), 0, AndroidUtilities.dp(8f))
        }
        row.addView(tvSub, matchWrap())

        // Real ported Telegram LineProgressView
        val progress = LineProgressView(this).apply {
            setBackColor(Theme.getColor(Theme.key_windowBackgroundGray))
            setProgressColor(accentColor)
            setProgress((stat.accuracy / 100f).coerceIn(0f, 1f), true)
        }
        row.addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(4f)
            ).apply {
                bottomMargin = if (showDivider) AndroidUtilities.dp(12f) else 0
            }
        )

        if (showDivider) {
            val divider = View(this).apply {
                setBackgroundColor(Theme.getColor(Theme.key_divider))
            }
            row.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
        }
        return row
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
