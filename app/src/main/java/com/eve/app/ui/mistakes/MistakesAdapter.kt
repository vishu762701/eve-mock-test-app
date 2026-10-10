package com.eve.app.ui.mistakes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.remote.MistakeItem
import com.eve.app.databinding.ItemMistakeBinding

class MistakesAdapter(
    private val onToggleLearned: (MistakeItem) -> Unit,
    private val isLearned: (String) -> Boolean,
    private val isHindi: () -> Boolean
) : ListAdapter<MistakeItem, MistakesAdapter.MistakeViewHolder>(DiffCallback) {

    inner class MistakeViewHolder(private val binding: ItemMistakeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MistakeItem) {
            val hindi = isHindi()
            val learned = isLearned(item.questionId)

            val qText = if (hindi && item.questionTextHi.isNotBlank()) item.questionTextHi else item.questionText
            binding.tvQuestionText.text = qText

            val isSkipped = item.selected.isBlank()
            if (isSkipped) {
                binding.tvMistakeType.text = "Skipped"
                binding.tvMistakeType.setTextColor(ContextCompat.getColor(itemView.context, R.color.eve_text_secondary))
                binding.layoutYourAnswer.visibility = View.GONE
            } else {
                binding.tvMistakeType.text = "Wrong Answer"
                binding.tvMistakeType.setTextColor(ContextCompat.getColor(itemView.context, R.color.eve_status_error))
                binding.layoutYourAnswer.visibility = View.VISIBLE
                val ansText = if (hindi && item.selectedTextHi.isNotBlank()) item.selectedTextHi else item.selectedText
                binding.tvYourAnswer.text = "${item.selected}) $ansText"
            }

            val corText = if (hindi && item.correctTextHi.isNotBlank()) item.correctTextHi else item.correctText
            binding.tvCorrectAnswer.text = "${item.correct}) $corText"

            val expText = if (hindi && item.explanationHi.isNotBlank()) item.explanationHi else item.explanation
            if (expText.isNotBlank()) {
                binding.layoutExplanation.visibility = View.VISIBLE
                binding.tvExplanation.text = expText
            } else {
                binding.layoutExplanation.visibility = View.GONE
            }

            // Learned toggle state
            if (learned) {
                binding.btnMarkLearned.text = "Learned ✓"
                binding.btnMarkLearned.setTextColor(ContextCompat.getColor(itemView.context, R.color.eve_status_success))
                binding.btnMarkLearned.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.eve_shape_border)
                )
                binding.layoutExplanation.alpha = 0.65f
            } else {
                binding.btnMarkLearned.text = "Mark as learned"
                binding.btnMarkLearned.setTextColor(ContextCompat.getColor(itemView.context, R.color.eve_text))
                binding.btnMarkLearned.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.eve_shape_border)
                )
                binding.layoutExplanation.alpha = 1.0f
            }

            binding.btnMarkLearned.setOnClickListener {
                onToggleLearned(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MistakeViewHolder {
        val binding = ItemMistakeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MistakeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MistakeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MistakeItem>() {
        override fun areItemsTheSame(oldItem: MistakeItem, newItem: MistakeItem): Boolean =
            oldItem.questionId == newItem.questionId

        override fun areContentsTheSame(oldItem: MistakeItem, newItem: MistakeItem): Boolean =
            oldItem == newItem
    }
}
