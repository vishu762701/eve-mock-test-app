package com.eve.app.ui.bookmarks

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.databinding.ItemBookmarkCardBinding
import com.eve.app.designsystem.DesignIconStates
import com.eve.app.designsystem.UiArcMotion
import com.eve.app.util.FontManager

/**
 * BookmarkAdapter with UIArc Expandable Card motion.
 * Genuine native translation of official UIArc Expandable Card specification:
 * - Registry: https://uiarc.dev/r/expandable-card.json
 * - Inline height morphing with UiArcMotion.Spring.SMOOTH
 * - Opening: Delayed text fade-in (120ms entrance delay, Ease.STANDARD)
 * - Closing: Fast text fade-out (100ms) with closeHold (60ms) before height collapse
 * - Rapid reversal support without glitching
 * - Synchronous state restoration on view recycling
 * - Instant fallback when reduced motion is requested
 */
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

    inner class BookmarkViewHolder(val binding: ItemBookmarkCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var heightAnimator: ValueAnimator? = null
        private var alphaAnimator: ValueAnimator? = null

        fun cancelAnimations() {
            heightAnimator?.cancel()
            heightAnimator = null
            alphaAnimator?.cancel()
            alphaAnimator = null
        }

        fun bind(item: BookmarkedQuestion) {
            cancelAnimations()
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
            val greenColor = ContextCompat.getColor(context, R.color.system_green)
            val defaultColor = ContextCompat.getColor(context, R.color.text_primary)

            fun styleOption(tv: android.widget.TextView, letter: String) {
                val optText = item.displayOptionText(letter, hindi)
                if (item.correctAnswer.isNotBlank() && letter.equals(item.correctAnswer, ignoreCase = true)) {
                    tv.text = "$letter. $optText  ✓"
                    tv.setTextColor(greenColor)
                    tv.typeface = FontManager.typeface(tv.context, bold = true)
                } else {
                    tv.text = "$letter. $optText"
                    tv.setTextColor(defaultColor)
                    tv.typeface = FontManager.typeface(tv.context, bold = false)
                }
            }

            styleOption(binding.tvOptionA, "A")
            styleOption(binding.tvOptionB, "B")
            styleOption(binding.tvOptionC, "C")
            styleOption(binding.tvOptionD, "D")

            // Explanation with Expandable Card motion
            val explanation = item.displayExplanation(hindi)
            if (explanation.isNotBlank()) {
                binding.btnToggleExplanation.visibility = View.VISIBLE
                val isExpanded = expandedExplanations.contains(item.questionId)
                binding.tvExplanation.text = explanation
                binding.tvExplanation.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.tvExplanation.alpha = 1f
                binding.tvExplanation.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.btnToggleExplanation.text = if (isExpanded) "Hide Explanation" else "View Explanation"

                binding.btnToggleExplanation.setOnClickListener {
                    toggleExplanation(item)
                }
            } else {
                binding.btnToggleExplanation.visibility = View.GONE
                binding.tvExplanation.visibility = View.GONE
            }

            DesignIconStates.bookmark(binding.btnBookmark, true)
            binding.btnBookmark.setOnClickListener {
                DesignIconStates.bookmark(binding.btnBookmark, false)
                onUnbookmark(item)
            }

            // Tap card to open question in test flow
            binding.root.setOnClickListener {
                onOpenQuestion(item)
            }
            binding.layoutTapToOpen.setOnClickListener {
                onOpenQuestion(item)
            }
        }

        private fun toggleExplanation(item: BookmarkedQuestion) {
            val context = binding.root.context
            val willExpand = !expandedExplanations.contains(item.questionId)

            if (willExpand) {
                expandedExplanations.add(item.questionId)
                expandExplanation(context)
            } else {
                expandedExplanations.remove(item.questionId)
                collapseExplanation(context)
            }
        }

        private fun expandExplanation(context: android.content.Context) {
            binding.btnToggleExplanation.text = "Hide Explanation"

            if (!UiArcMotion.isMotionEnabled(context)) {
                binding.tvExplanation.visibility = View.VISIBLE
                binding.tvExplanation.alpha = 1f
                binding.tvExplanation.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                return
            }

            val wasCollapsing = heightAnimator?.isRunning == true
            val startingHeight = if (wasCollapsing) {
                binding.tvExplanation.height
            } else {
                0
            }
            cancelAnimations()

            binding.tvExplanation.visibility = View.VISIBLE
            binding.tvExplanation.alpha = if (wasCollapsing) binding.tvExplanation.alpha else 0f

            val availableWidth = when {
                binding.root.width > 0 -> binding.root.width - binding.root.paddingLeft - binding.root.paddingRight
                binding.root.measuredWidth > 0 -> binding.root.measuredWidth
                else -> binding.root.resources.displayMetrics.widthPixels
            }
            val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            binding.tvExplanation.measure(widthSpec, heightSpec)
            val targetHeight = binding.tvExplanation.measuredHeight

            if (targetHeight <= 0) {
                binding.tvExplanation.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.tvExplanation.alpha = 1f
                return
            }

            binding.tvExplanation.layoutParams.height = startingHeight
            binding.tvExplanation.requestLayout()

            val hAnim = ValueAnimator.ofInt(startingHeight, targetHeight).apply {
                duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
                interpolator = UiArcMotion.Spring.SMOOTH
                addUpdateListener {
                    val h = it.animatedValue as Int
                    binding.tvExplanation.layoutParams.height = h
                    binding.tvExplanation.requestLayout()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.tvExplanation.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        binding.tvExplanation.requestLayout()
                        heightAnimator = null
                    }
                })
            }
            heightAnimator = hAnim
            hAnim.start()

            val aAnim = ValueAnimator.ofFloat(binding.tvExplanation.alpha, 1f).apply {
                startDelay = if (wasCollapsing) 0L else 120L
                duration = UiArcMotion.Duration.STANDARD
                interpolator = UiArcMotion.Ease.STANDARD
                addUpdateListener {
                    binding.tvExplanation.alpha = it.animatedValue as Float
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        alphaAnimator = null
                    }
                })
            }
            alphaAnimator = aAnim
            aAnim.start()
        }

        private fun collapseExplanation(context: android.content.Context) {
            binding.btnToggleExplanation.text = "View Explanation"

            if (!UiArcMotion.isMotionEnabled(context)) {
                binding.tvExplanation.visibility = View.GONE
                binding.tvExplanation.alpha = 1f
                binding.tvExplanation.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                return
            }

            val wasExpanding = heightAnimator?.isRunning == true
            val startingHeight = if (wasExpanding) {
                binding.tvExplanation.height
            } else {
                binding.tvExplanation.measuredHeight.let { if (it > 0) it else binding.tvExplanation.height }
            }
            cancelAnimations()

            val aAnim = ValueAnimator.ofFloat(binding.tvExplanation.alpha, 0f).apply {
                duration = 100L
                interpolator = UiArcMotion.Ease.STANDARD
                addUpdateListener {
                    binding.tvExplanation.alpha = it.animatedValue as Float
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        alphaAnimator = null
                    }
                })
            }
            alphaAnimator = aAnim
            aAnim.start()

            val hAnim = ValueAnimator.ofInt(startingHeight, 0).apply {
                startDelay = if (wasExpanding) 0L else 60L
                duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
                interpolator = UiArcMotion.Spring.SMOOTH
                addUpdateListener {
                    val h = it.animatedValue as Int
                    binding.tvExplanation.layoutParams.height = h
                    binding.tvExplanation.requestLayout()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.tvExplanation.visibility = View.GONE
                        binding.tvExplanation.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        binding.tvExplanation.alpha = 1f
                        binding.tvExplanation.requestLayout()
                        heightAnimator = null
                    }
                })
            }
            heightAnimator = hAnim
            hAnim.start()
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

    override fun onViewRecycled(holder: BookmarkViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelAnimations()
    }
}
