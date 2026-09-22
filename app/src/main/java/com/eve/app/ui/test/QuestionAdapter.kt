package com.eve.app.ui.test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ItemQuestionBinding

class QuestionAdapter(
    private val questions: List<Question>,
    private val getSelected: (Int) -> String,
    private val onSelect: (Int, String) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.VH>() {

    inner class VH(private val b: ItemQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(position: Int, q: Question) {
            b.tvQuestion.text = "Q${position + 1}. ${q.questionText}"
            b.rbA.text = "A. ${q.optionA}"
            b.rbB.text = "B. ${q.optionB}"
            b.rbC.text = "C. ${q.optionC}"
            b.rbD.text = "D. ${q.optionD}"

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
