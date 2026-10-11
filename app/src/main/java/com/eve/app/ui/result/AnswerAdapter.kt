package com.eve.app.ui.result

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.QuestionStat
import com.eve.app.databinding.ItemAnswerBinding

class AnswerAdapter(
    private val onReport: ((AnswerItem) -> Unit)? = null
) : RecyclerView.Adapter<AnswerAdapter.VH>() {

    private var items: List<AnswerItem> = emptyList()
    private var hindi = false
    private var questionStats: Map<String, QuestionStat> = emptyMap()
    private var showTimeInsight = true
    private val expandedQuestionKeys = mutableSetOf<String>()

    fun submit(newItems: List<AnswerItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun resetExpandedSolutions() {
        expandedQuestionKeys.clear()
        notifyDataSetChanged()
    }

    fun isSolutionExpanded(questionKey: String): Boolean = expandedQuestionKeys.contains(questionKey)

    fun expandSolution(questionKey: String) {
        expandedQuestionKeys.add(questionKey)
        notifyDataSetChanged()
    }

    fun setHindi(value: Boolean) {
        hindi = value
        notifyDataSetChanged()
    }

    fun setShowTimeInsight(value: Boolean) {
        showTimeInsight = value
        notifyDataSetChanged()
    }

    fun setQuestionStats(stats: Map<String, QuestionStat>) {
        questionStats = stats
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAnswerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AnswerItem) {
            val ctx = b.root.context
            val questionKey = if (item.questionId.isNotBlank()) item.questionId else "q_${item.number}"
            val isExpanded = expandedQuestionKeys.contains(questionKey)

            val statusDrawable = when {
                !item.isAttempted -> R.drawable.ic_status_circle_unattempted
                item.isCorrect -> R.drawable.ic_status_circle_right
                else -> R.drawable.ic_status_circle_wrong
            }
            b.ivStatusCircle.setImageResource(statusDrawable)
            b.tvQuestionNum.text = "Q${item.number}"
            b.tvQ.text = item.displayQuestionText(hindi)

            val (statusText, badgeColor) = when {
                !item.isAttempted -> Pair("Unattempted", R.color.eve_text_secondary)
                item.isCorrect -> Pair("Correct", R.color.eve_status_success)
                else -> Pair("Incorrect", R.color.eve_status_error)
            }
            b.tvQuestionStatusLabel.text = statusText
            b.tvQuestionStatusLabel.setTextColor(ContextCompat.getColor(ctx, badgeColor))

            // Bind complete question options (A, B, C, D)
            val optA = item.displayOptionText("A", hindi)
            val optB = item.displayOptionText("B", hindi)
            val optC = item.displayOptionText("C", hindi)
            val optD = item.displayOptionText("D", hindi)
            val hasOptions = optA.isNotBlank() || optB.isNotBlank()
            b.layoutOptionsContainer.visibility = if (hasOptions) View.VISIBLE else View.GONE

            fun bindOptionRow(
                letter: String,
                text: String,
                layout: View,
                badge: android.widget.TextView,
                textView: android.widget.TextView,
                statusIv: android.widget.ImageView
            ) {
                if (text.isBlank()) {
                    layout.visibility = View.GONE
                    return
                }
                layout.visibility = View.VISIBLE
                badge.text = letter
                textView.text = text

                val isSelected = letter.equals(item.selected, ignoreCase = true)
                val isCorrect = letter.equals(item.correct, ignoreCase = true)

                if (isExpanded) {
                    when {
                        isCorrect -> {
                            layout.setBackgroundResource(R.drawable.bg_option_review_correct)
                            badge.setBackgroundResource(R.drawable.bg_option_badge_correct)
                            badge.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                            textView.setTextColor(ContextCompat.getColor(ctx, R.color.eve_tile_right_text))
                            statusIv.visibility = View.VISIBLE
                            statusIv.setImageResource(R.drawable.ic_check_circle)
                            statusIv.imageTintList = android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(ctx, R.color.eve_status_success)
                            )
                        }
                        isSelected -> {
                            layout.setBackgroundResource(R.drawable.bg_option_review_wrong)
                            badge.setBackgroundResource(R.drawable.bg_option_badge_wrong)
                            badge.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                            textView.setTextColor(ContextCompat.getColor(ctx, R.color.eve_tile_wrong_text))
                            statusIv.visibility = View.VISIBLE
                            statusIv.setImageResource(R.drawable.ic_close)
                            statusIv.imageTintList = android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(ctx, R.color.eve_status_error)
                            )
                        }
                        else -> {
                            layout.setBackgroundResource(R.drawable.bg_option_review_default)
                            badge.setBackgroundResource(R.drawable.bg_option_badge_default)
                            badge.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text_secondary))
                            textView.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text_secondary))
                            statusIv.visibility = View.GONE
                        }
                    }
                } else {
                    if (isSelected) {
                        layout.setBackgroundResource(R.drawable.bg_option_review_selected)
                        badge.setBackgroundResource(R.drawable.bg_option_badge_selected)
                        badge.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                        textView.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text))
                    } else {
                        layout.setBackgroundResource(R.drawable.bg_option_review_default)
                        badge.setBackgroundResource(R.drawable.bg_option_badge_default)
                        badge.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text))
                        textView.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text))
                    }
                    statusIv.visibility = View.GONE
                }
            }

            bindOptionRow("A", optA, b.layoutOptionA, b.tvBadgeOptionA, b.tvOptionA, b.ivOptionAStatus)
            bindOptionRow("B", optB, b.layoutOptionB, b.tvBadgeOptionB, b.tvOptionB, b.ivOptionBStatus)
            bindOptionRow("C", optC, b.layoutOptionC, b.tvBadgeOptionC, b.tvOptionC, b.ivOptionCStatus)
            bindOptionRow("D", optD, b.layoutOptionD, b.tvBadgeOptionD, b.tvOptionD, b.ivOptionDStatus)

            // Initial Review State: Hide solution details, answers, and status until View Solution is tapped
            if (isExpanded) {
                b.btnViewSolution.visibility = View.GONE
                b.layoutSolutionDetails.visibility = View.VISIBLE
                b.ivStatusCircle.visibility = View.VISIBLE
                b.tvQuestionStatusLabel.visibility = View.VISIBLE
            } else {
                b.btnViewSolution.visibility = View.VISIBLE
                b.layoutSolutionDetails.visibility = View.GONE
                b.ivStatusCircle.visibility = View.GONE
                b.tvQuestionStatusLabel.visibility = View.GONE
            }

            b.btnViewSolution.setOnClickListener {
                expandedQuestionKeys.add(questionKey)
                bind(item)
            }

            b.ivBookmark.visibility = if (item.isBookmarked) View.VISIBLE else View.GONE
            b.ivReportQuestion.setOnClickListener { onReport?.invoke(item) }
            b.layoutReportAction.setOnClickListener { onReport?.invoke(item) }

            when {
                !item.isAttempted -> {
                    b.tvYourAnswer.text = "Your answer: Not attempted"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_grey))
                }
                item.isCorrect -> {
                    b.tvYourAnswer.text = "Your answer: ${item.selected}. ${item.displaySelectedText(hindi)}  ✓ Correct"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_status_success))
                }
                else -> {
                    b.tvYourAnswer.text = "Your answer: ${item.selected}. ${item.displaySelectedText(hindi)}  ✗ Incorrect"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_status_error))
                }
            }
            b.tvCorrectAnswer.text = "Correct answer: ${item.correct}. ${item.displayCorrectText(hindi)}  ✓"
            b.tvCorrectAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_status_success))

            // X% got this right stat
            val stat = questionStats[item.questionId]
            val pct = when {
                stat != null && stat.totalAttempts > 0 -> stat.correctPercentage
                item.isCorrect -> 100
                else -> 0
            }
            b.tvAccuracyStat.text = "$pct% got this right"

            // Per-question Time vs Average Insight (only when fresh submit, in-memory time, and stats exist)
            val timeSpent = item.timeTakenSeconds
            if (showTimeInsight && item.isAttempted && timeSpent > 0 && stat != null && stat.totalAttempts > 0) {
                b.layoutTimeInsight.visibility = View.VISIBLE
                val avgTime = stat.calculatedAvgSeconds.toLong().coerceAtLeast(1L)
                val comparison = com.eve.app.util.TimeComparisonFormatter.format(
                    timeSpent = timeSpent,
                    avgTime = avgTime,
                    isCorrect = item.isCorrect
                )

                val maxTime = maxOf(timeSpent, avgTime, 1L).toFloat()
                val youPct = ((timeSpent.toFloat() / maxTime) * 100f).toInt().coerceIn(5, 100)
                val avgPct = ((avgTime.toFloat() / maxTime) * 100f).toInt().coerceIn(5, 100)

                b.pbTimeYou.progress = youPct
                b.pbTimeAvg.progress = avgPct

                val statusColor = ContextCompat.getColor(ctx, comparison.statusColorRes)
                b.pbTimeYou.progressTintList = android.content.res.ColorStateList.valueOf(statusColor)
                b.pbTimeAvg.progressTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.eve_grey)
                )

                b.tvTimeYou.text = "${timeSpent}s"
                b.tvTimeAvg.text = "${avgTime}s"

                b.tvTimeInsightMessage.text = comparison.message
                b.tvTimeInsightMessage.setTextColor(statusColor)
            } else {
                b.layoutTimeInsight.visibility = View.GONE
            }

            // Solution in Key Points Format
            val rawExp = item.displayExplanation(hindi)
            if (rawExp.isNotBlank()) {
                b.layoutSolution.visibility = View.VISIBLE

                // Bold one-line summary stating the correct answer
                val summary = if (hindi) {
                    "सही उत्तर विकल्प ${item.correct} है।"
                } else {
                    "The correct answer is Option ${item.correct}."
                }
                b.tvCorrectSummary.text = summary

                // Format explanation with bold concept labels & bullet points
                val formattedHtml = formatExplanationToKeyPoints(rawExp)
                b.tvExplanation.text = Html.fromHtml(formattedHtml, Html.FROM_HTML_MODE_COMPACT)
            } else {
                b.layoutSolution.visibility = View.GONE
            }

            b.btnToggleExplanation.visibility = View.GONE
        }
    }

    private fun formatExplanationToKeyPoints(raw: String): String {
        var text = raw.trim()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        val bulletLines = mutableListOf<String>()
        for (line in lines) {
            if (line.startsWith("The correct answer is", ignoreCase = true) ||
                line.startsWith("सही उत्तर विकल्प", ignoreCase = true) ||
                line.startsWith("Key Points", ignoreCase = true)) {
                continue
            }
            // Replace markdown **bold** with <b>bold</b>
            var converted = line.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            if (!converted.startsWith("•") && !converted.startsWith("-")) {
                converted = "• $converted"
            }
            bulletLines.add(converted)
        }

        if (bulletLines.isEmpty() && text.isNotBlank()) {
            val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
            for ((idx, s) in sentences.withIndex()) {
                val label = if (idx == 0) "<b>Key Concept:</b> " else "• "
                bulletLines.add("$label$s")
            }
        }

        return bulletLines.joinToString("<br/><br/>")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
