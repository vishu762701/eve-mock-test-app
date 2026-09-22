package com.eve.app.ui.performance

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.TopicStat
import com.eve.app.databinding.ItemTopicStatBinding

class TopicStatAdapter : RecyclerView.Adapter<TopicStatAdapter.VH>() {

    private var items: List<TopicStat> = emptyList()

    fun submit(list: List<TopicStat>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemTopicStatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(stat: TopicStat) {
            b.tvTopicName.text = stat.topic
            b.tvTopicAccuracy.text = "${stat.accuracy}%"
            b.pbTopicAccuracy.progress = stat.accuracy
            b.tvTopicCount.text = "${stat.correct}/${stat.total} correct"

            val colorRes = when {
                stat.accuracy >= 70 -> R.color.eve_green
                stat.accuracy >= 40 -> R.color.eve_accent
                else -> R.color.eve_red
            }
            val color = ContextCompat.getColor(b.root.context, colorRes)
            b.tvTopicAccuracy.setTextColor(color)
            b.pbTopicAccuracy.progressDrawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTopicStatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
