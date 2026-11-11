package com.eve.app.ui.mistakes

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

    companion object {
        const val PAYLOAD_LEARNED = "PAYLOAD_LEARNED"

        val DiffCallback = object : DiffUtil.ItemCallback<MistakeItem>() {
            override fun areItemsTheSame(oldItem: MistakeItem, newItem: MistakeItem): Boolean =
                oldItem.questionId == newItem.questionId

            override fun areContentsTheSame(oldItem: MistakeItem, newItem: MistakeItem): Boolean =
                oldItem == newItem
        }
    }

    inner class MistakeViewHolder(private val binding: ItemMistakeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun updateLearnedState(learned: Boolean, animated: Boolean) {
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(itemView.context)
            val greenColor = ContextCompat.getColor(itemView.context, R.color.system_green_text)
            val secondaryColor = palette.textSecondary
            binding.cbLearned.setColors(
                unchecked = palette.hairline,
                checked = ContextCompat.getColor(itemView.context, R.color.system_green),
                check = palette.onAccent
            )
            binding.cbLearned.setChecked(learned, animated)
            if (learned) {
                binding.tvMarkLearned.text = "Learned"
                binding.tvMarkLearned.setTextColor(greenColor)
            } else {
                binding.tvMarkLearned.text = "Mark as learned"
                binding.tvMarkLearned.setTextColor(secondaryColor)
            }
        }

        fun bind(item: MistakeItem) {
            val hindi = isHindi()
            val learned = isLearned(item.questionId)
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(itemView.context)
            binding.root.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)

            val qText = if (hindi && item.questionTextHi.isNotBlank()) item.questionTextHi else item.questionText
            binding.tvQuestionText.text = qText
            binding.tvQuestionText.setTextColor(palette.textPrimary)

            val isSkipped = item.selected.isBlank()
            if (isSkipped) {
                binding.tvMistakeType.text = "Skipped"
                binding.tvMistakeType.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                binding.layoutYourAnswer.visibility = View.GONE
            } else {
                binding.tvMistakeType.text = "Wrong Answer"
                binding.tvMistakeType.setTextColor(ContextCompat.getColor(itemView.context, R.color.system_red_text))
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
                binding.tvExplanation.setTextColor(palette.textSecondary)
            } else {
                binding.layoutExplanation.visibility = View.GONE
            }

            // Learned toggle with Telegram CheckBox2 and authentic round-rect selector
            binding.btnMarkLearned.background = com.eve.app.designsystem.TelegramThemeSelectors.getRoundRectSelectorDrawable(
                8 * itemView.resources.displayMetrics.density,
                palette.actionSelector
            )
            updateLearnedState(learned, animated = false)

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
        binding.root.rippleColor = android.content.res.ColorStateList.valueOf(
            com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
        )
        return MistakeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MistakeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: MistakeViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LEARNED)) {
            val item = getItem(position)
            holder.updateLearnedState(isLearned(item.questionId), animated = true)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
}
