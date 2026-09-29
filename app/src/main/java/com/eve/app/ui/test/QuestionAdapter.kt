package com.eve.app.ui.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ItemQuestionBinding

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
    private val onToggleMark: (Int) -> Unit = {}
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

    inner class VH(private val b: ItemQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun updateTimer(seconds: Long) {
            b.tvQuestionTimer.text = formatQuestionTime(seconds)
        }

        fun bind(position: Int, q: Question) {
            updateTimer(getQuestionTime(position))
            b.btnReport.setOnClickListener {
                onReport(q)
            }

            val hindi = isHindi()
            b.tvQuestion.text = "Q${position + 1}. ${q.displayQuestionText(hindi)}"
            b.rbA.text = "A. ${q.displayOptionText("A", hindi)}"
            b.rbB.text = "B. ${q.displayOptionText("B", hindi)}"
            b.rbC.text = "C. ${q.displayOptionText("C", hindi)}"
            b.rbD.text = "D. ${q.displayOptionText("D", hindi)}"

            fun refreshBookmarkIcon() {
                b.btnBookmark.setImageResource(
                    if (getBookmarked(position)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                )
            }
            refreshBookmarkIcon()
            b.btnBookmark.setOnClickListener {
                onToggleBookmark(position)
                b.btnBookmark.animate().cancel()
                b.btnBookmark.animate()
                    .scaleX(0.6f).scaleY(0.6f)
                    .setDuration(90)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            refreshBookmarkIcon()
                            b.btnBookmark.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(220)
                                .setInterpolator(OvershootInterpolator(3f))
                                .setListener(null)
                                .start()
                        }
                    })
                    .start()
            }



            // Recycled view me purana state / listener saaf karo, phir saved answer restore karo
            b.rgOptions.setOnCheckedChangeListener(null)
            b.rgOptions.clearCheck()
            when (getSelected(position)) {
                "A" -> b.rbA.isChecked = true
                "B" -> b.rbB.isChecked = true
                "C" -> b.rbC.isChecked = true
                "D" -> b.rbD.isChecked = true
            }
            b.rgOptions.setOnCheckedChangeListener { _, checkedId ->
                val letter = when (checkedId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> ""
                }
                if (letter.isNotEmpty()) {
                    com.eve.app.util.HapticHelper.performOptionSelect(b.root)
                    onSelect(position, letter)
                }
            }

            fun refreshMarkReview() {
                val marked = isMarked(position)
                b.btnMarkReview.text = if (marked) b.root.context.getString(R.string.unmark_review) else b.root.context.getString(R.string.mark_for_review)
            }
            refreshMarkReview()
            b.btnMarkReview.setOnClickListener {
                onToggleMark(position)
                refreshMarkReview()
            }

            b.btnClear.setOnClickListener {
                b.rgOptions.setOnCheckedChangeListener(null)
                b.rgOptions.clearCheck()
                onSelect(position, "")
                b.rgOptions.setOnCheckedChangeListener { _, checkedId ->
                    val letter = when (checkedId) {
                        R.id.rbA -> "A"
                        R.id.rbB -> "B"
                        R.id.rbC -> "C"
                        R.id.rbD -> "D"
                        else -> ""
                    }
                    if (letter.isNotEmpty()) {
                        com.eve.app.util.HapticHelper.performOptionSelect(b.root)
                        onSelect(position, letter)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

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
