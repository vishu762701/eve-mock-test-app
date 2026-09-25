package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.FeedbackPostReply
import com.eve.app.databinding.ItemPostReplyBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostRepliesAdapter(
    private val onMarkRead: (FeedbackPostReply) -> Unit,
    private val onDelete: (FeedbackPostReply) -> Unit
) : ListAdapter<FeedbackPostReply, PostRepliesAdapter.VH>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    object DiffCallback : DiffUtil.ItemCallback<FeedbackPostReply>() {
        override fun areItemsTheSame(oldItem: FeedbackPostReply, newItem: FeedbackPostReply): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FeedbackPostReply, newItem: FeedbackPostReply): Boolean =
            oldItem == newItem
    }

    inner class VH(private val binding: ItemPostReplyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FeedbackPostReply) {
            val authorText = if (item.email.isNotBlank()) {
                "${item.name} • ${item.email}"
            } else {
                item.name
            }
            binding.tvReplyAuthor.text = authorText
            binding.tvReplyText.text = item.text

            val timeText = if (item.timestamp > 0L) {
                dateFormat.format(Date(item.timestamp))
            } else {
                "—"
            }
            binding.tvReplyTime.text = timeText

            if (item.read) {
                binding.btnMarkRead.text = "Read"
                binding.btnMarkRead.isEnabled = false
                binding.btnMarkRead.alpha = 0.5f
            } else {
                binding.btnMarkRead.text = "Mark Read"
                binding.btnMarkRead.isEnabled = true
                binding.btnMarkRead.alpha = 1.0f
                binding.btnMarkRead.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onMarkRead(getItem(pos))
                    }
                }
            }

            binding.btnDeleteReply.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDelete(getItem(pos))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPostReplyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
