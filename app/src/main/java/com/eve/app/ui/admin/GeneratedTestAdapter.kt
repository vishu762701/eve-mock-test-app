package com.eve.app.ui.admin

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.GeneratedTest
import com.eve.app.databinding.ItemGeneratedTestBinding

class GeneratedTestAdapter(
    private val onStatusToggle: (GeneratedTest, Boolean) -> Unit,
    private val onPreview: (GeneratedTest) -> Unit,
    private val onDelete: (GeneratedTest) -> Unit
) : RecyclerView.Adapter<GeneratedTestAdapter.VH>() {

    private var items: List<GeneratedTest> = emptyList()

    fun submit(list: List<GeneratedTest>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemGeneratedTestBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(test: GeneratedTest) {
            b.tvExamName.text = test.examName.ifBlank { "Exam Test" }
            val timeAgo = if (test.generatedAt > 0) {
                DateUtils.getRelativeTimeSpanString(
                    test.generatedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            } else "Recent"
            b.tvTestDetails.text = "${test.questionCount} questions • Generated $timeAgo"

            val isLive = test.isLive
            b.switchLive.setOnCheckedChangeListener(null)
            b.switchLive.isChecked = isLive
            b.switchLive.setOnCheckedChangeListener { _, isChecked ->
                onStatusToggle(test, isChecked)
            }

            b.tvStatusBadge.text = if (isLive) "LIVE" else "PAUSED"
            b.tvStatusBadge.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isLive) R.color.eve_green else R.color.eve_text_secondary
                )
            )

            b.btnPreview.setOnClickListener { onPreview(test) }
            b.btnDelete.setOnClickListener { onDelete(test) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemGeneratedTestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
