package com.eve.app.ui.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.databinding.ItemAnswerBinding

class AnswerAdapter(private val items: List<AnswerItem>) :
    RecyclerView.Adapter<AnswerAdapter.VH>() {

    inner class VH(private val b: ItemAnswerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AnswerItem) {
            b.tvQ.text = "Q${item.number}. ${item.questionText}"
            val ctx = b.root.context

            when {
                !item.isAttempted -> {
                    b.tvYourAnswer.text = "Your answer: Not attempted"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_grey))
                }
                item.isCorrect -> {
                    b.tvYourAnswer.text = "Your answer: ${item.selected}. ${item.selectedText}  ✓"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_green))
                }
                else -> {
                    b.tvYourAnswer.text = "Your answer: ${item.selected}. ${item.selectedText}  ✗"
                    b.tvYourAnswer.setTextColor(ContextCompat.getColor(ctx, R.color.eve_red))
                }
            }
            b.tvCorrectAnswer.text = "Correct answer: ${item.correct}. ${item.correctText}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
