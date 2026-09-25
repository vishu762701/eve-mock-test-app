package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.BroadcastMessage
import com.eve.app.databinding.ItemSentBroadcastBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SentBroadcastAdapter(
    private val onDelete: (BroadcastMessage) -> Unit,
    private val onItemClick: (BroadcastMessage) -> Unit,
    private val onItemLongClick: (BroadcastMessage) -> Unit
) : ListAdapter<BroadcastMessage, SentBroadcastAdapter.VH>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    var isSelectionMode: Boolean = false
    val selectedIds = mutableSetOf<String>()

    fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        notifyDataSetChanged()
    }

    fun selectAll(ids: Collection<String>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
    }

    object DiffCallback : DiffUtil.ItemCallback<BroadcastMessage>() {
        override fun areItemsTheSame(oldItem: BroadcastMessage, newItem: BroadcastMessage): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BroadcastMessage, newItem: BroadcastMessage): Boolean =
            oldItem == newItem
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

            if (isSelectionMode) {
                binding.cbSelect.visibility = View.VISIBLE
                binding.cbSelect.isChecked = selectedIds.contains(item.id)
                binding.btnDeleteBroadcast.visibility = View.GONE

                binding.root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(getItem(pos))
                    }
                }
                binding.root.setOnLongClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemLongClick(getItem(pos))
                    }
                    true
                }
            } else {
                binding.cbSelect.visibility = View.GONE
                binding.btnDeleteBroadcast.visibility = View.VISIBLE
                binding.btnDeleteBroadcast.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onDelete(getItem(pos))
                    }
                }
                binding.root.setOnClickListener(null)
                binding.root.setOnLongClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemLongClick(getItem(pos))
                    }
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSentBroadcastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
