package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.BroadcastMessage
import com.eve.app.databinding.ItemSentBroadcastBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SentBroadcastAdapter(
    private val onDelete: (BroadcastMessage) -> Unit
) : RecyclerView.Adapter<SentBroadcastAdapter.VH>() {

    private var items: List<BroadcastMessage> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun submit(list: List<BroadcastMessage>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val binding: ItemSentBroadcastBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BroadcastMessage) {
            binding.tvBroadcastTitle.text = item.title
            binding.tvBroadcastMessage.text = item.message
            val formattedTime = if (item.sentAt > 0) {
                dateFormat.format(Date(item.sentAt))
            } else {
                "—"
            }
            binding.tvBroadcastTime.text = formattedTime
            binding.btnDeleteBroadcast.setOnClickListener {
                onDelete(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSentBroadcastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size
}
