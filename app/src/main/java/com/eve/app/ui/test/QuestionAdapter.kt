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
    private val isHindi: () -> Boolean
) : RecyclerView.Adapter<QuestionAdapter.VH>() {

    inner class VH(private val b: ItemQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(position: Int, q: Question) {
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
                // Phase 25: shrink-then-pop-back-with-overshoot ("bounce") — icon swap
                // happens exactly at the smallest point so the new icon is the one that
                // bounces in, jaisa Telegram/Twitter ke like-button me hota hai.
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
                if (letter.isNotEmpty()) onSelect(position, letter)
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
                    if (letter.isNotEmpty()) onSelect(position, letter)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(position, questions[position])

    override fun getItemCount() = questions.size
}
