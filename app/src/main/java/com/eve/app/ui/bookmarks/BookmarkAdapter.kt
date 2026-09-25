package com.eve.app.ui.bookmarks

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.databinding.ItemBookmarkCardBinding

class BookmarkAdapter(
    private val onOpenQuestion: (BookmarkedQuestion) -> Unit,
    private val onUnbookmark: (BookmarkedQuestion) -> Unit,
    private val isHindi: () -> Boolean
) : ListAdapter<BookmarkedQuestion, BookmarkAdapter.BookmarkViewHolder>(DiffCallback) {

    private val expandedExplanations = mutableSetOf<String>()

    object DiffCallback : DiffUtil.ItemCallback<BookmarkedQuestion>() {
        override fun areItemsTheSame(oldItem: BookmarkedQuestion, newItem: BookmarkedQuestion): Boolean =
            oldItem.questionId == newItem.questionId

        override fun areContentsTheSame(oldItem: BookmarkedQuestion, newItem: BookmarkedQuestion): Boolean =
            oldItem == newItem
    }

    inner class BookmarkViewHolder(private val binding: ItemBookmarkCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookmarkedQuestion) {
            val context = binding.root.context
            val hindi = isHindi()

            // Header tags
            binding.tvExamTag.text = item.examName.ifBlank { "Practice Test" }
            if (item.questionNumber > 0) {
                binding.tvQuestionNumber.visibility = View.VISIBLE
                binding.tvQuestionNumber.text = "Q${item.questionNumber}"
            } else {
                binding.tvQuestionNumber.visibility = View.GONE
            }

            val topicTag = when {
                item.isPyq && item.pyqYear > 0 -> "PYQ ${item.pyqYear}"
                item.topic.isNotBlank() -> item.topic
                else -> ""
            }
            if (topicTag.isNotBlank()) {
                binding.tvTopicTag.visibility = View.VISIBLE
                binding.tvTopicTag.text = topicTag
            } else {
                binding.tvTopicTag.visibility = View.GONE
            }

            // Question Text
            val qText = item.displayQuestionText(hindi)
            binding.tvQuestionText.text = if (item.questionNumber > 0) {
                "Q${item.questionNumber}. $qText"
            } else {
                qText
            }

            // Options styling
            val greenColor = ContextCompat.getColor(context, R.color.eve_green)
            val defaultColor = ContextCompat.getColor(context, R.color.eve_text)

            fun styleOption(tv: android.widget.TextView, letter: String) {
                val optText = item.displayOptionText(letter, hindi)
                if (letter.equals(item.correctAnswer, ignoreCase = true)) {
                    tv.text = "$letter. $optText  ✓"
                    tv.setTextColor(greenColor)
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    tv.text = "$letter. $optText"
                    tv.setTextColor(defaultColor)
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }

            styleOption(binding.tvOptionA, "A")
            styleOption(binding.tvOptionB, "B")
            styleOption(binding.tvOptionC, "C")
            styleOption(binding.tvOptionD, "D")

            // Explanation
            val explanation = item.displayExplanation(hindi)
            if (explanation.isNotBlank()) {
                binding.btnToggleExplanation.visibility = View.VISIBLE
                val isExpanded = expandedExplanations.contains(item.questionId)
                binding.tvExplanation.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.tvExplanation.text = explanation
                binding.btnToggleExplanation.text = if (isExpanded) "Hide Explanation" else "View Explanation"

                binding.btnToggleExplanation.setOnClickListener {
                    if (isExpanded) {
                        expandedExplanations.remove(item.questionId)
                    } else {
                        expandedExplanations.add(item.questionId)
                    }
                    notifyItemChanged(bindingAdapterPosition)
                }
            } else {
                binding.btnToggleExplanation.visibility = View.GONE
                binding.tvExplanation.visibility = View.GONE
            }

            // Star unbookmark button
            binding.btnBookmark.setImageResource(R.drawable.ic_star_filled)
            binding.btnBookmark.setOnClickListener {
                binding.btnBookmark.animate().cancel()
                binding.btnBookmark.animate()
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(90)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            binding.btnBookmark.setImageResource(R.drawable.ic_star_outline)
                            binding.btnBookmark.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .setInterpolator(OvershootInterpolator(2.5f))
                                .setListener(null)
                                .withEndAction {
                                    onUnbookmark(item)
                                }
                                .start()
                        }
                    })
                    .start()
            }

            // Tap card to open question in test flow
            binding.root.setOnClickListener {
                onOpenQuestion(item)
            }
            binding.layoutTapToOpen.setOnClickListener {
                onOpenQuestion(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
