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

    fun submit(newItems: List<AnswerItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setHindi(value: Boolean) {
        hindi = value
        notifyDataSetChanged()
    }

    fun setQuestionStats(stats: Map<String, QuestionStat>) {
        questionStats = stats
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAnswerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AnswerItem) {
            b.tvQ.text = "Q${item.number}. ${item.displayQuestionText(hindi)}"
            val ctx = b.root.context

            b.ivBookmark.visibility = if (item.isBookmarked) View.VISIBLE else View.GONE
            b.ivReportQuestion.setOnClickListener { onReport?.invoke(item) }

            when {
                !item.isAttempted -> {
                    b.tvYourAnswer.text = "Your answer: Not attempted"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_grey))
                }
                item.isCorrect -> {
                    b.tvYourAnswer.text = "Your answer: ${item.selected}. ${item.displaySelectedText(hindi)}  ✓"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_green))
                }
                else -> {
                    b.tvYourAnswer.text = "Your answer: ${item.selected}. ${item.displaySelectedText(hindi)}  ✗"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_red))
                }
            }
            b.tvCorrectAnswer.text = "Correct answer: ${item.correct}. ${item.displayCorrectText(hindi)}"

            // X% got this right stat
            val stat = questionStats[item.questionId]
            val pct = when {
                stat != null && stat.totalAttempts > 0 -> stat.correctPercentage
                item.isCorrect -> 100
                else -> 0
            }
            b.tvAccuracyStat.text = "$pct% got this right"

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
