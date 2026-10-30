package com.eve.app.ui.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ItemQuestionBinding
import com.eve.app.util.HapticHelper

class QuestionAdapter(
    private val questions: List<Question>,
    private val getSelected: (Int) -> String,
    private val onSelect: (Int, String) -> Unit,
    private val getBookmarked: (Int) -> Boolean,
    private val onToggleBookmark: (Int) -> Unit,
    private val isHindi: () -> Boolean,
    private val onReport: (Question) -> Unit,
    private val getQuestionTime: (Int) -> Long,
    private val isMarked: (Int) -> Boolean = { false },
    private val onToggleMark: (Int) -> Unit = {},
    private val canSelect: () -> Boolean = { true }
) : RecyclerView.Adapter<QuestionAdapter.VH>() {

    companion object {
        const val PAYLOAD_TIMER = "PAYLOAD_TIMER"

        fun formatQuestionTime(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) {
                String.format("%d:%02d:%02d", h, m, s)
            } else {
                String.format("%d:%02d", m, s)
            }
        }
    }

    inner class VH(
        private val b: ItemQuestionBinding
    ) : RecyclerView.ViewHolder(b.root) {
        private var suppressSelectionCallbacks = false
        init {
            // View IDs repeat on every page. The ViewModel is the only answer
            // state owner; Android hierarchy restoration must not check a page.
            // Telegram ActionBar.java:534 establishes the prominent-title atom.
            // EVE question hierarchy uses owner-requested stronger 700 weight;
            // the arrangement/700 weight are ADAPTED, not Telegram-exact.
            com.eve.app.designsystem.DesignType.apply(
                b.tvQuestion, com.eve.app.designsystem.DesignType.Role.HEADING
            )
            b.tvQuestion.typeface = com.eve.app.util.FontManager.boldTypeface(b.root.context)
            b.rgOptions.isSaveEnabled = false
            listOf(b.rbA, b.rbB, b.rbC, b.rbD).forEach {
                it.isSaveEnabled = false
                it.applyChoiceStyle()
            }
        }

        fun updateTimer(seconds: Long) {
            b.tvQuestionTimer.text = formatQuestionTime(seconds)
        }

        fun bind(position: Int, q: Question) {
            updateTimer(getQuestionTime(position))
            b.btnReport.setOnClickListener {
                onReport(q)
            }

            val hindi = isHindi()
            val questionText = "Q${position + 1}. ${q.displayQuestionText(hindi)}"
            b.tvQuestion.text = questionText
            b.rbA.text = "A. ${q.displayOptionText("A", hindi)}"
            b.rbB.text = "B. ${q.displayOptionText("B", hindi)}"
            b.rbC.text = "C. ${q.displayOptionText("C", hindi)}"
            b.rbD.text = "D. ${q.displayOptionText("D", hindi)}"


            // Keep the approved font sizes; scroll the full question and options on small screens.
            b.questionScroll.scrollTo(0, 0)

            com.eve.app.designsystem.DesignIconStates.bookmark(b.btnBookmark, getBookmarked(position))
            b.btnBookmark.setOnClickListener {
                onToggleBookmark(position)
                val nowBookmarked = getBookmarked(position)
                if (nowBookmarked) HapticHelper.performLight(b.btnBookmark)
                com.eve.app.designsystem.DesignIconStates.bookmark(b.btnBookmark, nowBookmarked)
            }

            // Clear previous listener on recycled view, then restore saved answer selection
            b.rgOptions.setOnCheckedChangeListener(null)
            b.rgOptions.clearCheck()
            when (getSelected(position)) {
                "A" -> b.rbA.isChecked = true
                "B" -> b.rbB.isChecked = true
                "C" -> b.rbC.isChecked = true
                "D" -> b.rbD.isChecked = true
            }
            resetOptionAnimations()
            b.rgOptions.setOnCheckedChangeListener { _, checkedId ->
                if (suppressSelectionCallbacks) return@setOnCheckedChangeListener
                if (!canSelect()) {
                    suppressSelectionCallbacks = true
                    b.rgOptions.clearCheck()
                    when (getSelected(position)) {
                        "A" -> b.rbA.isChecked = true
                        "B" -> b.rbB.isChecked = true
                        "C" -> b.rbC.isChecked = true
                        "D" -> b.rbD.isChecked = true
                    }
                    suppressSelectionCallbacks = false
                    resetOptionAnimations()
                    return@setOnCheckedChangeListener
                }
                val letter = when (checkedId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> ""
                }
                if (letter.isNotEmpty()) {
                    HapticHelper.performOptionSelect(b.root)
                    onSelect(position, letter)
                }
            }
        }

        fun clearSelection() {
            // RadioGroup may notify the old ID while unchecking its child. Do not save
            // either that intermediate answer or NO_ID; keep the next tap connected.
            suppressSelectionCallbacks = true
            try {
                b.rgOptions.clearCheck()
                resetOptionAnimations()
            } finally {
                suppressSelectionCallbacks = false
            }
        }

        private fun resetOptionAnimations() {
            listOf(b.rbA, b.rbB, b.rbC, b.rbD).forEach { it.jumpDrawablesToCurrentState() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(
            ItemQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(position, questions[position])

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TIMER)) {
            holder.updateTimer(getQuestionTime(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount() = questions.size
}
