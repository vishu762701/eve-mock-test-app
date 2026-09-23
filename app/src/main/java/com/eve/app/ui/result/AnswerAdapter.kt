package com.eve.app.ui.result

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.databinding.ItemAnswerBinding

class AnswerAdapter : RecyclerView.Adapter<AnswerAdapter.VH>() {

    private var items: List<AnswerItem> = emptyList()
    private var hindi = false

    fun submit(newItems: List<AnswerItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setHindi(value: Boolean) {
        hindi = value
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAnswerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AnswerItem) {
            b.tvQ.text = "Q${item.number}. ${item.displayQuestionText(hindi)}"
            val ctx = b.root.context

            b.ivBookmark.visibility = if (item.isBookmarked) View.VISIBLE else View.GONE

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

            // Display explanation under the answer key so students see why it's correct
            val explanationText = item.displayExplanation(hindi)
            if (explanationText.isNotBlank()) {
                b.tvExplanation.visibility = View.VISIBLE
                b.tvExplanation.text = "Explanation: $explanationText"
            } else {
                b.tvExplanation.visibility = View.GONE
            }
            b.btnToggleExplanation.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
