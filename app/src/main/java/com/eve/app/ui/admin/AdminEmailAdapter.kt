package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.databinding.ItemAdminEmailBinding
import com.eve.app.util.AvatarDrawable

class AdminEmailAdapter(
    private val onItemClick: (DynamicAdminItem) -> Unit = {},
    private val onRemove: (DynamicAdminItem) -> Unit = {}
) : RecyclerView.Adapter<AdminEmailAdapter.VH>() {

    private var items: List<DynamicAdminItem> = emptyList()

    fun submit(list: List<DynamicAdminItem>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAdminEmailBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DynamicAdminItem) {
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            val email = item.email
            b.tvEmail.text = email
            b.tvEmail.setTextColor(palette.textPrimary)
            val roleText = when {
                item.isOwner -> "Owner"
                !item.customTitle.isNullOrBlank() -> item.customTitle
                item.rights.isEmpty() -> "No rights assigned"
                item.rights.size == 13 -> "All rights"
                else -> "${item.rights.size} rights"
            }
            b.tvRole.text = roleText
            b.tvRole.setTextColor(palette.textSecondary)

            val initials = AvatarDrawable.extractInitials(email)
            val colorIndex = AvatarDrawable.getColorIndex(email)
            b.ivAvatar.setImageDrawable(AvatarDrawable(b.root.context, initials, colorIndex))

            if (item.isOwner) {
                b.btnRemove.visibility = View.GONE
            } else {
                b.btnRemove.visibility = View.VISIBLE
                b.btnRemove.background = com.eve.app.designsystem.TelegramThemeSelectors.getCircleSelectorDrawable(palette.actionSelector)
                b.btnRemove.setOnClickListener { onRemove(item) }
            }

            b.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAdminEmailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        b.root.background = com.eve.app.designsystem.TelegramThemeSelectors.getRoundRectSelectorDrawable(
            8 * parent.resources.displayMetrics.density,
            com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}

