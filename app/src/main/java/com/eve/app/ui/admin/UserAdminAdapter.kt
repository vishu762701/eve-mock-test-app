package com.eve.app.ui.admin

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import com.eve.app.designsystem.TelegramPopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AdminUser
import com.eve.app.databinding.ItemUserAdminBinding
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine
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
            val palette = TelegramThemeEngine.resolveCurrentPalette(binding.root.context)
            val density = binding.root.resources.displayMetrics.density

            val name = user.displayName.ifBlank { "Student" }
            binding.tvUserName.text = name
            binding.tvUserName.setTextColor(palette.textPrimary)
            binding.tvUserEmail.text = user.email.ifBlank { "No email" }
            binding.tvUserEmail.setTextColor(palette.textSecondary)

            binding.ivUserAvatar.setImageDrawable(AvatarDrawable.create(binding.root.context, name, user.id))

            val joinedStr = if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "Unknown"
            val activeStr = formatRelativeActive(user.lastActive)
            binding.tvUserDates.text = "Joined: $joinedStr • Active: $activeStr"
            binding.tvUserDates.setTextColor(palette.textSecondary)

            if (user.disabled) {
                binding.tvUserStatusBadge.text = "Banned"
                binding.tvUserStatusBadge.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.system_red_text)
                )
            } else {
                binding.tvUserStatusBadge.text = "Active"
                binding.tvUserStatusBadge.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.system_green_text)
                )
            }

            binding.root.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                12f * density,
                palette.actionSelector
            )
            binding.root.setOnClickListener { onViewDetails(user) }

            binding.btnMoreUser.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )
            binding.btnMoreUser.imageTintList = ColorStateList.valueOf(palette.textSecondary)
            binding.btnMoreUser.setOnClickListener { view ->
                dismissActivePopup()
                val popup = TelegramPopupMenu(view.context).apply {
                    addItem(1, "👁️ View Test History") { onViewDetails(user) }
                    if (user.disabled) {
                        addItem(2, "✅ Enable Account") { onToggleStatus(user, false) }
                    } else {
                        addItem(2, "🚫 Disable / Ban Account", isDestructive = true) { onToggleStatus(user, true) }
                    }
                }
                activePopup = popup
                popup.setOnDismissListener { if (activePopup === popup) activePopup = null }
                com.eve.app.designsystem.DesignPopupGlass.around(view) { popup.show(view) }
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

    private var activePopup: TelegramPopupMenu? = null

    fun dismissActivePopup() {
        activePopup?.dismiss()
        activePopup = null
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
