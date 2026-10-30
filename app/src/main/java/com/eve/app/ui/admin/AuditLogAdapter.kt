package com.eve.app.ui.admin

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.databinding.ItemAuditLogBinding
import com.eve.app.databinding.ItemAuditLogHeaderBinding
import com.eve.app.util.AvatarDrawable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class AuditLogListItem {
    data class Header(val title: String) : AuditLogListItem()
    data class Entry(val log: AdminAuditLog) : AuditLogListItem()
}

class AuditLogAdapter(
    private val onItemClick: ((AdminAuditLog) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    private val items = mutableListOf<AuditLogListItem>()
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    fun submitLogs(logs: List<AdminAuditLog>) {
        items.clear()
        if (logs.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        // Group logs by day
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        var currentHeader = ""
        for (log in logs) {
            val date = Date(log.timestamp)
            calendar.time = date
            val day = calendar.get(Calendar.DAY_OF_YEAR)
            val year = calendar.get(Calendar.YEAR)

            val headerText = when {
                year == todayYear && day == today -> "Today"
                year == todayYear && day == today - 1 -> "Yesterday"
                else -> fullDateFormat.format(date)
            }

            if (headerText != currentHeader) {
                currentHeader = headerText
                items.add(AuditLogListItem.Header(headerText))
            }
            items.add(AuditLogListItem.Entry(log))
        }
        notifyDataSetChanged()
    }

    fun submit(logs: List<AdminAuditLog>) {
        submitLogs(logs)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AuditLogListItem.Header -> TYPE_HEADER
            is AuditLogListItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemAuditLogHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemAuditLogBinding.inflate(inflater, parent, false)
                EntryViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AuditLogListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is AuditLogListItem.Entry -> (holder as EntryViewHolder).bind(item.log)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(private val b: ItemAuditLogHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(title: String) {
            b.tvDateHeader.text = title
        }
    }

    inner class EntryViewHolder(private val b: ItemAuditLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(log: AdminAuditLog) {
            val email = log.adminEmail.ifBlank { "Admin" }
            b.tvAdminEmail.text = email
            b.ivAvatar.setImageDrawable(AvatarDrawable.create(b.root.context, email, email))

            b.tvDescription.text = log.description
            b.tvActionBadge.text = log.actionBadge

            if (log.timestamp > 0L) {
                b.tvTimestamp.text = timeFormat.format(Date(log.timestamp))
            } else {
                b.tvTimestamp.text = "Recent"
            }

            b.tvClientSource.visibility = if (log.source == "client") View.VISIBLE else View.GONE

            b.root.setOnClickListener {
                onItemClick?.invoke(log)
            }
        }
    }
}
