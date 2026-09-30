package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AdminPremiumUserDto
import com.eve.app.databinding.ItemAdminPremiumUserBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminPremiumUserAdapter(
    private var items: List<AdminPremiumUserDto> = emptyList(),
    private val onExtend: (AdminPremiumUserDto) -> Unit,
    private val onRevoke: (AdminPremiumUserDto) -> Unit
) : RecyclerView.Adapter<AdminPremiumUserAdapter.UserViewHolder>() {

    fun submitList(newItems: List<AdminPremiumUserDto>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminPremiumUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onExtend, onRevoke)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class UserViewHolder(
        private val binding: ItemAdminPremiumUserBinding,
        private val onExtend: (AdminPremiumUserDto) -> Unit,
        private val onRevoke: (AdminPremiumUserDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(item: AdminPremiumUserDto) {
            val context = binding.root.context

            binding.tvUserDisplayName.text = item.displayName.ifBlank { "Student" }
            binding.tvUserEmail.text = item.email.ifBlank { item.userId }

            val expiryText = if (item.isLifetime) {
                "Lifetime"
            } else if (item.expiresAt != null && item.expiresAt > 0) {
                dateFormat.format(Date(item.expiresAt))
            } else {
                "N/A"
            }

            binding.tvPlanAndExpiry.text = "${item.planName} • Expires: $expiryText (Source: ${item.source})"

            val (statusText, colorRes) = when (item.status.uppercase()) {
                "ACTIVE" -> "ACTIVE" to R.color.eve_status_success
                "EXPIRED" -> "EXPIRED" to R.color.eve_status_error
                "REVOKED" -> "REVOKED" to R.color.eve_status_error
                else -> item.status.uppercase() to R.color.eve_text_secondary
            }

            binding.tvUserStatusBadge.text = statusText
            binding.tvUserStatusBadge.setTextColor(ContextCompat.getColor(context, colorRes))

            binding.btnExtendUser.setOnClickListener {
                onExtend(item)
            }

            binding.btnRevokeUser.setOnClickListener {
                onRevoke(item)
            }
        }
    }
}
