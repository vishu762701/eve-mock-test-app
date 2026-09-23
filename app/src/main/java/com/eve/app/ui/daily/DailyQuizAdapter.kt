package com.eve.app.ui.daily

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.DailyQuizDay
import com.eve.app.databinding.ItemDailyQuizBinding
import com.eve.app.util.DateUtil

class DailyQuizAdapter(
    private val onClick: (DailyQuizDay) -> Unit
) : RecyclerView.Adapter<DailyQuizAdapter.VH>() {

    private var items: List<DailyQuizDay> = emptyList()

    fun submit(list: List<DailyQuizDay>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemDailyQuizBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(day: DailyQuizDay) {
            val weekday = DateUtil.weekday(day.date)
            b.tvDate.text = if (weekday.isBlank()) DateUtil.display(day.date)
            else "${DateUtil.display(day.date)}  •  $weekday"
            val q = if (day.questionCount == 1) "1 question" else "${day.questionCount} questions"
            b.tvMeta.text = "$q  •  ${day.timeLimitMinutes} min"
            b.root.setOnClickListener { onClick(day) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDailyQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
