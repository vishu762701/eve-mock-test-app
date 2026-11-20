package com.eve.app.ui.result

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.SecurityHelper
import com.eve.app.util.TopicAccuracyHelper
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import java.util.Locale

/**
 * Result Detail (Statistics, Performance Standing, Analytics & Insights) screen
 * powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextDetailSettingsCell, ShadowSectionCell,
 * TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class ResultDetailActivity : EveBaseActivity() {

    companion object {
        const val EXTRA_DETAIL_TYPE = "EXTRA_DETAIL_TYPE"
        const val TYPE_STATISTICS = "type_statistics"
        const val TYPE_PERFORMANCE = "type_performance"
        const val TYPE_ANALYTICS = "type_analytics"

        fun launch(
            context: Context,
            ownerUid: String,
            type: String,
            examId: String,
            examName: String,
            score: Double,
            total: Int,
            correct: Int,
            wrong: Int,
            unattempted: Int,
            accuracy: Double,
            rank: String = "",
            percentile: String = "",
            topperAvg: String = ""
        ) {
            if (ownerUid.isBlank() || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != ownerUid) return
            val intent = Intent(context, ResultDetailActivity::class.java).apply {
                putExtra(Constants.EXTRA_RESULT_OWNER_UID, ownerUid)
                putExtra(EXTRA_DETAIL_TYPE, type)
                putExtra(Constants.EXTRA_EXAM_ID, examId)
                putExtra(Constants.EXTRA_EXAM_NAME, examName)
                putExtra("EXTRA_SCORE", score)
                putExtra("EXTRA_TOTAL", total)
                putExtra("EXTRA_CORRECT", correct)
                putExtra("EXTRA_WRONG", wrong)
                putExtra("EXTRA_UNATTEMPTED", unattempted)
                putExtra("EXTRA_ACCURACY", accuracy)
                putExtra("EXTRA_RANK", rank)
                putExtra("EXTRA_PERCENTILE", percentile)
                putExtra("EXTRA_TOPPER_AVG", topperAvg)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var listContainer: LinearLayout

    private var resultOwnerUid = ""
    private val resultOwnerListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
        if (resultOwnerUid.isNotBlank() && auth.currentUser?.uid != resultOwnerUid) {
            if (::rootView.isInitialized) rootView.visibility = View.INVISIBLE
            finish()
        }
    }

    override fun onDestroy() {
        com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(resultOwnerListener)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultOwnerUid = intent.getStringExtra(Constants.EXTRA_RESULT_OWNER_UID).orEmpty()
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        if (resultOwnerUid.isBlank() || auth.currentUser?.uid != resultOwnerUid) { finish(); return }
        auth.addAuthStateListener(resultOwnerListener)
        if (isFinishing || auth.currentUser?.uid != resultOwnerUid) { finish(); return }
        SecurityHelper.applyScreenProtection(this)

        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        val type = intent.getStringExtra(EXTRA_DETAIL_TYPE) ?: TYPE_STATISTICS
        val examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME).orEmpty()
        val score = intent.getDoubleExtra("EXTRA_SCORE", 0.0)
        val total = intent.getIntExtra("EXTRA_TOTAL", 0)
        val correct = intent.getIntExtra("EXTRA_CORRECT", 0)
        val wrong = intent.getIntExtra("EXTRA_WRONG", 0)
        val unattempted = intent.getIntExtra("EXTRA_UNATTEMPTED", 0)
        val accuracy = intent.getDoubleExtra("EXTRA_ACCURACY", 0.0)
        val rank = intent.getStringExtra("EXTRA_RANK").orEmpty()
        val percentile = intent.getStringExtra("EXTRA_PERCENTILE").orEmpty()
        val topperAvg = intent.getStringExtra("EXTRA_TOPPER_AVG").orEmpty()

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            if (examName.isNotBlank()) {
                setSubtitle(examName)
            }
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(listContainer, matchWrap())
        rootView.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val allItems: List<AnswerItem> = ResultDataHolder.getDetailItems(resultOwnerUid, examId)

        when (type) {
            TYPE_STATISTICS -> setupStatistics(score, total, correct, wrong, unattempted, accuracy)
            TYPE_PERFORMANCE -> setupPerformance(score, rank, percentile, topperAvg)
            TYPE_ANALYTICS -> setupAnalytics(examId, examName, allItems)
            else -> setupStatistics(score, total, correct, wrong, unattempted, accuracy)
        }
    }

    private fun setupStatistics(
        score: Double,
        total: Int,
        correct: Int,
        wrong: Int,
        unattempted: Int,
        accuracy: Double
    ) {
        actionBar.setTitle("Statistics Details")

        val scoreStr = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.US, "%.2f", score)
        val scorePct = if (total > 0) (score * 100.0 / total).coerceAtLeast(0.0) else 0.0
        val correctMarks = correct * 1.0
        val wrongDeduction = (correctMarks - score).coerceAtLeast(0.0)

        addHeader("Score Summary")
        addDetailRow("$scoreStr / $total", String.format(Locale.US, "%.1f%% Overall Score", scorePct), true)
        addDetailRow(String.format(Locale.US, "%.1f%%", accuracy), "Accuracy", false)
        addShadow()

        addHeader("Marking Breakdown")
        addDetailRow(
            String.format(Locale.US, "+%.2f", correctMarks),
            "Correct Answers ($correct × +1.00)",
            true
        )
        addDetailRow(
            String.format(Locale.US, "-%.2f", wrongDeduction),
            "Negative Marks ($wrong incorrect)",
            true
        )
        addDetailRow(
            "0.00",
            "Unattempted Questions ($unattempted skipped)",
            true
        )
        addDetailRow(scoreStr, "Final Net Score", false)
        addShadow()

        addInfo("Net score reflects positive marks earned minus negative marking deductions.")
    }

    private fun setupPerformance(
        score: Double,
        rank: String,
        percentile: String,
        topperAvg: String
    ) {
        actionBar.setTitle("Performance Standing")

        val scoreStr = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.US, "%.2f", score)
        var topperStr = "--"
        var avgStr = "--"
        if (topperAvg.contains("Topper:", ignoreCase = true)) {
            val parts = topperAvg.split("•")
            for (part in parts) {
                val clean = part.trim()
                if (clean.startsWith("Topper:", ignoreCase = true)) {
                    topperStr = clean.substringAfter(":").trim()
                } else if (clean.startsWith("Average:", ignoreCase = true) || clean.startsWith("Avg:", ignoreCase = true)) {
                    avgStr = clean.substringAfter(":").trim()
                }
            }
        }

        addHeader("Standing")
        addDetailRow(if (rank.isNotBlank()) rank else "Rank: --", "Your Rank", true)
        addDetailRow(if (percentile.isNotBlank()) percentile else "--%", "Percentile", false)
        addShadow()

        addHeader("Score Comparison")
        addDetailRow(scoreStr, "Your Score", true)
        addDetailRow(topperStr, "Topper Score", true)
        addDetailRow(avgStr, "Average Score", false)
        addShadow()

        addInfo("Rank and percentile update as more candidates complete this test.")
    }

    private fun setupAnalytics(
        examId: String,
        examName: String,
        allItems: List<AnswerItem>
    ) {
        actionBar.setTitle("Analytics & Insights")

        // 1. Topic Accuracy (Weakest First)
        addHeader("Topic Accuracy (Weakest First)")
        val topicAccs = TopicAccuracyHelper.aggregate(allItems)
        if (topicAccs.isNotEmpty()) {
            topicAccs.forEachIndexed { idx, ta ->
                val pctStr = String.format(Locale.US, "%.1f%%", ta.accuracy)
                addDetailRow(
                    ta.topic,
                    "$pctStr accuracy • ${ta.correct}/${ta.total} correct",
                    idx < topicAccs.lastIndex
                )
            }
            val weakest = topicAccs.first()
            if (weakest.accuracy < 100.0) {
                val btnWrap = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    val padH = AndroidUtilities.dp(16f)
                    val padV = AndroidUtilities.dp(12f)
                    setPadding(padH, padV, padH, padV)
                }
                val btnPractice = ButtonWithCounterView(this, null).apply {
                    setRound()
                    setText("Practice Weakest: ${weakest.topic}", false)
                    setOnClickListener {
                        val practiceIntent = Intent(this@ResultDetailActivity, TestActivity::class.java).apply {
                            putExtra(Constants.EXTRA_EXAM_ID, examId)
                            putExtra(Constants.EXTRA_EXAM_NAME, examName)
                            putExtra(Constants.EXTRA_TOPIC, weakest.topic)
                        }
                        startActivity(practiceIntent)
                    }
                }
                btnWrap.addView(
                    btnPractice,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        AndroidUtilities.dp(48f)
                    )
                )
                listContainer.addView(btnWrap, matchWrap())
            }
        } else {
            addInfo("No topic metadata recorded for this test.")
        }
        addShadow()

        // 2. Question Pace Chart
        addHeader("Question Pace")
        val chartSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, padV)
        }
        val chartDetail = TextView(this).apply {
            text = "Tap any bar to inspect time spent"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            setPadding(0, AndroidUtilities.dp(8f), 0, 0)
        }
        val barItems = allItems.mapIndexed { index, item ->
            QuestionTimeChartView.BarItem(
                questionNumber = index + 1,
                timeSeconds = item.timeTakenSeconds,
                isCorrect = item.isCorrect,
                isAttempted = item.isAttempted
            )
        }
        val chartView = QuestionTimeChartView(this).apply {
            setItems(barItems)
            onBarSelected = { bar ->
                val status = when {
                    bar.isCorrect -> "Correct"
                    bar.isAttempted -> "Wrong"
                    else -> "Skipped"
                }
                chartDetail.text = "Question ${bar.questionNumber}: ${bar.timeSeconds}s • $status"
            }
        }
        chartSection.addView(
            chartView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(180f)
            )
        )
        chartSection.addView(chartDetail, matchWrap())
        listContainer.addView(chartSection, matchWrap())
        addShadow()

        // 3. Slowest Questions
        addHeader("Slowest Questions")
        val slowest = allItems.filter { it.timeTakenSeconds > 0 }
            .sortedByDescending { it.timeTakenSeconds }
            .take(5)
        if (slowest.isNotEmpty()) {
            slowest.forEachIndexed { idx, item ->
                val status = when {
                    item.isCorrect -> "Correct"
                    item.isAttempted -> "Wrong"
                    else -> "Skipped"
                }
                val snippet = item.questionText.trim().take(90)
                addDetailRow(
                    "${item.timeTakenSeconds}s • $status",
                    snippet,
                    idx < slowest.lastIndex
                )
            }
        } else {
            addInfo("No timed questions recorded.")
        }
    }

    private fun addHeader(title: String) {
        listContainer.addView(HeaderCell(this).apply {
            setText(title)
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
    }

    private fun addDetailRow(text: String, value: String, divider: Boolean) {
        listContainer.addView(TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setMultilineDetail(true)
            setTextAndValue(text, value, divider)
        }, matchWrap())
    }

    private fun addShadow() {
        listContainer.addView(ShadowSectionCell(this), matchWrap())
    }

    private fun addInfo(text: String) {
        listContainer.addView(TextInfoPrivacyCell(this).apply {
            setText(text)
        }, matchWrap())
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
