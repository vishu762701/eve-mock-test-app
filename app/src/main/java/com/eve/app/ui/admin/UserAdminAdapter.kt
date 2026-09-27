package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AdminUser
import com.eve.app.databinding.ItemUserAdminBinding
import com.eve.app.util.AvatarDrawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdminAdapter(
    private val onViewDetails: (AdminUser) -> Unit,
    private val onToggleStatus: (AdminUser, Boolean) -> Unit
) : ListAdapter<AdminUser, UserAdminAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemUserAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: AdminUser) {
            val name = user.displayName.ifBlank { "Student" }
            binding.tvUserName.text = name
            binding.tvUserEmail.text = user.email.ifBlank { "No email" }

            binding.ivUserAvatar.setImageDrawable(AvatarDrawable.create(name, user.id))

            val joinedStr = if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "Unknown"
            val activeStr = formatRelativeActive(user.lastActive)
            binding.tvUserDates.text = "Joined: $joinedStr • Active: $activeStr"

            if (user.disabled) {
                binding.tvUserStatusBadge.text = "Banned"
                binding.tvUserStatusBadge.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.eve_red)
                )
            } else {
                binding.tvUserStatusBadge.text = "Active"
                binding.tvUserStatusBadge.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.eve_green)
                )
            }

            binding.root.setOnClickListener { onViewDetails(user) }

            binding.btnMoreUser.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "👁️ View Test History")
                if (user.disabled) {
                    popup.menu.add(0, 2, 1, "✅ Enable Account")
                } else {
                    popup.menu.add(0, 2, 1, "🚫 Disable / Ban Account")
                }

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onViewDetails(user)
                            true
                        }
                        2 -> {
                            onToggleStatus(user, !user.disabled)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }

        private fun formatRelativeActive(timestamp: Long): String {
            if (timestamp <= 0) return "Never"
            val diff = System.currentTimeMillis() - timestamp
            val minutes = diff / (60 * 1000)
            val hours = diff / (60 * 60 * 1000)
            val days = diff / (24 * 60 * 60 * 1000)

            return when {
                minutes < 5 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AdminUser>() {
        override fun areItemsTheSame(oldItem: AdminUser, newItem: AdminUser) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AdminUser, newItem: AdminUser) =
            oldItem == newItem
    }
}
