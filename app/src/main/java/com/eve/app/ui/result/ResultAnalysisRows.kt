package com.eve.app.ui.result

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.databinding.ItemResultTopicBinding
import com.eve.app.util.TopicAccuracy

/** Production analytics binders also used by native screenshot verification. */
object ResultAnalysisRows {
    private fun params(context: Context) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = (12 * context.resources.displayMetrics.density).toInt() }

    fun topic(context: Context, accuracy: TopicAccuracy): View =
        ItemResultTopicBinding.inflate(LayoutInflater.from(context)).apply {
            root.layoutParams = params(context)
            tvTopicName.text = accuracy.topic
            tvTopicResult.text = "${accuracy.accuracy}% (${accuracy.correct}/${accuracy.total})"
            tvTopicResult.setTextColor(ContextCompat.getColor(context, when {
                accuracy.accuracy >= 70.0 -> R.color.system_green
                accuracy.accuracy >= 40.0 -> R.color.system_orange
                else -> R.color.system_red
            }))
        }.root

    fun slowest(context: Context, answer: AnswerItem): View = TextView(context).apply {
        layoutParams = params(context)
        val status = when { answer.isCorrect -> "Correct"; answer.isAttempted -> "Wrong"; else -> "Skipped" }
        text = "Q${answer.number}: ${answer.timeTakenSeconds}s • $status"
        TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Eve_Token_Secondary)
        setTextColor(ContextCompat.getColor(context, when {
            answer.isCorrect -> R.color.system_green
            answer.isAttempted -> R.color.system_red
            else -> R.color.text_secondary
        }))
        setBackgroundResource(R.drawable.design_tile_surface)
        val padding = (12 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
    }
}
