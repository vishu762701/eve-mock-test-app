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

    fun submit(newItems: List<AnswerItem>) {
        items = newItems
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
