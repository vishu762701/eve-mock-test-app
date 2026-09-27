package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.databinding.ItemAuditLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditLogAdapter : RecyclerView.Adapter<AuditLogAdapter.VH>() {

    private var items: List<AdminAuditLog> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun submit(newItems: List<AdminAuditLog>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAuditLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(log: AdminAuditLog) {
            b.tvActionBadge.text = log.actionBadge
            b.tvDescription.text = log.description
            b.tvAdminEmail.text = "By: ${log.adminEmail.ifBlank { "Admin" }}"
            b.tvTimestamp.text = if (log.timestamp > 0L) {
                dateFormat.format(Date(log.timestamp))
            } else {
                "Recent"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAuditLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
