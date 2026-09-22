package com.eve.app.ui.notifications

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.databinding.ItemNotificationBinding
import com.eve.app.util.StoredNotification

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.VH>() {

    private var items: List<StoredNotification> = emptyList()

    fun submit(list: List<StoredNotification>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: StoredNotification) {
            b.tvTitle.text = item.title
            b.tvBody.text = item.body
            b.tvTime.text = DateUtils.getRelativeTimeSpanString(
                item.timestampMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
