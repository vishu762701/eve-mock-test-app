package com.eve.app.ui.history

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.TestAttempt
import com.eve.app.databinding.ItemHistoryBinding
import com.eve.app.theme.TelegramThemeEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onClick: (TestAttempt) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private var items: List<TestAttempt> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun submit(list: List<TestAttempt>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(attempt: TestAttempt) {
            val palette = TelegramThemeEngine.resolveCurrentPalette(b.root.context)

            b.tvExamName.text = attempt.examName
            b.tvExamName.setTextColor(palette.textPrimary)

            b.tvMeta.text = "${attempt.category}  •  ${dateFormat.format(Date(attempt.timestamp))}"
            b.tvMeta.setTextColor(palette.textSecondary)

            b.tvScore.text = "Score: ${attempt.scoreText} / ${attempt.total}"
            b.tvBreakdown.text =
                "Correct: ${attempt.correct}   Wrong: ${attempt.wrong}   Unattempted: ${attempt.unattempted}"
            b.tvBreakdown.setTextColor(palette.textSecondary)

            val ctx = b.root.context
            val color = when {
                attempt.total == 0 -> R.color.text_secondary
                attempt.correct >= attempt.total / 2.0 -> R.color.system_green_text
                else -> R.color.system_red_text
            }
            b.tvScore.setTextColor(ContextCompat.getColor(ctx, color))

            b.root.rippleColor = ColorStateList.valueOf(palette.actionSelector)
            b.root.setOnClickListener { onClick(attempt) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
