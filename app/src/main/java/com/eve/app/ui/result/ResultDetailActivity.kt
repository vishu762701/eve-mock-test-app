package com.eve.app.ui.result

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.databinding.ActivityResultDetailBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.SecurityHelper
import com.eve.app.util.TopicAccuracyHelper
import java.util.Locale

class ResultDetailActivity : EveBaseActivity() {

    companion object {
        const val EXTRA_DETAIL_TYPE = "EXTRA_DETAIL_TYPE"
        const val TYPE_STATISTICS = "type_statistics"
        const val TYPE_PERFORMANCE = "type_performance"
        const val TYPE_ANALYTICS = "type_analytics"

        fun launch(
            context: Context,
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
            val intent = Intent(context, ResultDetailActivity::class.java).apply {
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

    private lateinit var binding: ActivityResultDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityResultDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
        binding.btnBack.setOnClickListener {
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

        if (examName.isNotBlank()) {
            binding.tvDetailSubtitle.visibility = View.VISIBLE
            binding.tvDetailSubtitle.text = examName
        } else {
            binding.tvDetailSubtitle.visibility = View.GONE
        }

        val allItems: List<AnswerItem> = ResultDataHolder.getDetailItems()

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
        binding.tvDetailTitle.text = "Statistics Details"
        binding.layoutContainerStatistics.visibility = View.VISIBLE

        val scoreStr = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.US, "%.2f", score)
        binding.tvStatHeroScore.text = "$scoreStr / $total"

        val scorePct = if (total > 0) (score * 100.0 / total).coerceAtLeast(0.0) else 0.0
        binding.tvStatHeroPercentage.text = String.format(Locale.US, "%.1f%% Score", scorePct)

        // Marking breakdown
        val correctMarks = correct * 1.0
        val wrongDeduction = (correctMarks - score).coerceAtLeast(0.0)

        binding.tvStatCorrectLabel.text = "Correct Answers ($correct × +1.00)"
        binding.tvStatCorrectMarks.text = String.format(Locale.US, "+%.2f", correctMarks)

        binding.tvStatWrongLabel.text = "Negative Marks ($wrong incorrect)"
        binding.tvStatWrongMarks.text = String.format(Locale.US, "-%.2f", wrongDeduction)

        binding.tvStatUnattemptedLabel.text = "Unattempted Questions ($unattempted skipped)"
        binding.tvStatUnattemptedCount.text = "0.00"

        binding.tvStatFinalNetScore.text = scoreStr

        // 4 Tiles
        binding.tvStatRightCount.text = correct.toString()
        binding.tvStatWrongCount.text = wrong.toString()
        binding.tvStatUnattemptedTileCount.text = unattempted.toString()
        binding.tvStatAccuracyPct.text = String.format(Locale.US, "%.1f%%", accuracy)
    }

    private fun setupPerformance(
        score: Double,
        rank: String,
        percentile: String,
        topperAvg: String
    ) {
        binding.tvDetailTitle.text = "Performance Standing"
        binding.layoutContainerPerformance.visibility = View.VISIBLE

        binding.tvPerfDetailRank.text = if (rank.isNotBlank()) rank else "Rank: --"
        binding.tvPerfDetailPercentile.text = if (percentile.isNotBlank()) "Percentile: $percentile" else "Percentile: --%"

        val scoreStr = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.US, "%.2f", score)
        binding.tvPerfYourScore.text = scoreStr

        // Parse topper and avg from topperAvg string if available (e.g. "Topper: 85.0 • Average: 62.0")
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
        binding.tvPerfTopperScore.text = topperStr
        binding.tvPerfAvgScore.text = avgStr
    }

    private fun setupAnalytics(
        examId: String,
        examName: String,
        allItems: List<AnswerItem>
    ) {
        binding.tvDetailTitle.text = "Analytics & Insights"
        binding.layoutContainerAnalytics.visibility = View.VISIBLE

        // 1. Topic Accuracy (Weakest First)
        val topicAccs = TopicAccuracyHelper.aggregate(allItems)
        binding.layoutTopicList.removeAllViews()

        if (topicAccs.isNotEmpty()) {
            val density = resources.displayMetrics.density
            for (ta in topicAccs) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (10 * density).toInt())
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.bg_tile_surface2)
                    setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                }

                val header = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tvTitle = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = ta.topic
                    setTextColor(ContextCompat.getColor(context, R.color.eve_text))
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }

                val tvAcc = TextView(this).apply {
                    text = "${ta.accuracy}% (${ta.correct}/${ta.total})"
                    setTextColor(
                        if (ta.accuracy >= 70.0) ContextCompat.getColor(context, R.color.eve_status_success)
                        else if (ta.accuracy >= 40.0) ContextCompat.getColor(context, R.color.eve_primary)
                        else ContextCompat.getColor(context, R.color.eve_status_error)
                    )
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }

                header.addView(tvTitle)
                header.addView(tvAcc)
                row.addView(header)

                binding.layoutTopicList.addView(row)
            }

            val weakest = topicAccs.first()
            if (weakest.accuracy < 100.0) {
                binding.btnPracticeWeakest.visibility = View.VISIBLE
                binding.btnPracticeWeakest.text = "Practice Weakest: ${weakest.topic}"
                binding.btnPracticeWeakest.setOnClickListener {
                    val practiceIntent = Intent(this, TestActivity::class.java).apply {
                        putExtra(Constants.EXTRA_EXAM_ID, examId)
                        putExtra(Constants.EXTRA_EXAM_NAME, examName)
                        putExtra(Constants.EXTRA_TOPIC, weakest.topic)
                    }
                    startActivity(practiceIntent)
                }
            } else {
                binding.btnPracticeWeakest.visibility = View.GONE
            }
        } else {
            val emptyTv = TextView(this).apply {
                text = "No topic metadata recorded for this test."
                setTextColor(ContextCompat.getColor(context, R.color.eve_text_secondary))
                textSize = 13f
            }
            binding.layoutTopicList.addView(emptyTv)
            binding.btnPracticeWeakest.visibility = View.GONE
        }

        // 2. Question Pace Chart
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

        // 3. Slowest Questions
        val slowest = allItems.filter { it.timeTakenSeconds > 0 }.sortedByDescending { it.timeTakenSeconds }.take(5)
        binding.layoutSlowestList.removeAllViews()

        if (slowest.isNotEmpty()) {
            val density = resources.displayMetrics.density
            for (item in slowest) {
                val tv = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (8 * density).toInt())
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
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    background = ContextCompat.getDrawable(context, R.drawable.bg_tile_surface2)
                    setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
                }
                binding.layoutSlowestList.addView(tv)
            }
        } else {
            val emptyTv = TextView(this).apply {
                text = "No timed questions recorded."
                setTextColor(ContextCompat.getColor(context, R.color.eve_text_secondary))
                textSize = 13f
            }
            binding.layoutSlowestList.addView(emptyTv)
        }
    }
}
