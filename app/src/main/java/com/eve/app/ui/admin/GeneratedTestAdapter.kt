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
    private val onDelete: (GeneratedTest) -> Unit,
    private val onSchedule: (GeneratedTest) -> Unit = {}
) : RecyclerView.Adapter<GeneratedTestAdapter.VH>() {

    private var items: List<GeneratedTest> = emptyList()

    fun getItems(): List<GeneratedTest> = items

    fun submit(list: List<GeneratedTest>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemGeneratedTestBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(test: GeneratedTest) {
            b.tvExamName.text = test.displayTitle
            val timeAgo = if (test.generatedAt > 0) {
                DateUtils.getRelativeTimeSpanString(
                    test.generatedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            } else "Recent"
            b.tvTestDetails.text = "${test.questionCount} questions • Generated $timeAgo" + if (test.validationError.isNotBlank()) "\n${test.validationError}" else ""

            if (test.availableFrom > 0) {
                b.tvScheduledTime.text = com.eve.app.util.TestScheduleHelper.formatOpensAt(test.availableFrom)
                b.tvScheduledTime.visibility = android.view.View.VISIBLE
            } else {
                b.tvScheduledTime.visibility = android.view.View.GONE
            }

            val isLive = test.isLive
            b.switchLive.setOnCheckedChangeListener(null)
            b.switchLive.isChecked = isLive
            b.switchLive.isEnabled = isLive || test.validationError.isBlank()
            b.switchLive.setOnCheckedChangeListener { _, isChecked ->
                onStatusToggle(test, isChecked)
            }

            b.tvStatusBadge.text = test.status.uppercase()
            b.tvStatusBadge.setBackgroundResource(
                if (isLive) R.drawable.bg_tile_right else R.drawable.bg_tile_medium
            )
            b.tvStatusBadge.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isLive) R.color.eve_tile_right_text else R.color.eve_tile_medium_text
                )
            )

            b.btnSchedule.setOnClickListener { onSchedule(test) }
            b.btnPreview.setOnClickListener { onPreview(test) }
            b.btnDelete.setOnClickListener { onDelete(test) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemGeneratedTestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
