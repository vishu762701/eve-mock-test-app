package com.eve.app.ui.admin

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.HomeBanner
import com.eve.app.databinding.ItemAdminBannerBinding
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.ExamImageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin list for Home banners, including link editing, reorder, and delete actions.
 */
class AdminBannerAdapter(
    private val onMoveUp: (HomeBanner) -> Unit,
    private val onMoveDown: (HomeBanner) -> Unit,
    private val onEditLink: (HomeBanner) -> Unit,
    private val onDelete: (HomeBanner) -> Unit
) : RecyclerView.Adapter<AdminBannerAdapter.VH>() {

    private var items: List<HomeBanner> = emptyList()

    fun submitList(list: List<HomeBanner>) {
        val diffCallback = object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = list.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == list[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == list[newItemPosition]
            }
        }
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        items = list
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAdminBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.root.rippleColor = ColorStateList.valueOf(
            TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position, items.size)
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemAdminBannerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(banner: HomeBanner, position: Int, total: Int) {
            val palette = TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            val density = b.root.resources.displayMetrics.density

            b.tvBannerOrder.text = "Banner #${position + 1}"
            b.tvBannerOrder.setTextColor(palette.textPrimary)

            b.tvBannerDate.text = if (banner.uploadedAt > 0L) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(banner.uploadedAt))
            } else {
                "Active"
            }
            b.tvBannerDate.setTextColor(palette.textSecondary)

            ExamImageHelper.loadBannerImage(b.ivBannerThumb, banner.imageUrl)
            b.tvBannerLink.text = banner.linkLabel.takeIf { banner.linkUrl.isNotBlank() && it.isNotBlank() }
                ?.let { "CTA: $it" }.orEmpty()
            b.tvBannerLink.setTextColor(palette.accent)
            b.tvBannerLink.visibility = if (b.tvBannerLink.text.isNullOrBlank()) View.GONE else View.VISIBLE

            b.btnEditBannerLink.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )
            b.btnEditBannerLink.imageTintList = ColorStateList.valueOf(palette.textSecondary)
            com.eve.app.designsystem.DesignPress.bind(b.btnEditBannerLink) { onEditLink(banner) }

            b.btnMoveUp.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )
            b.btnMoveUp.imageTintList = ColorStateList.valueOf(palette.textSecondary)
            b.btnMoveUp.isEnabled = position > 0
            b.btnMoveUp.alpha = if (position > 0) 1.0f else 0.35f
            com.eve.app.designsystem.DesignPress.bind(b.btnMoveUp) { onMoveUp(banner) }

            b.btnMoveDown.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )
            b.btnMoveDown.imageTintList = ColorStateList.valueOf(palette.textSecondary)
            b.btnMoveDown.isEnabled = position < total - 1
            b.btnMoveDown.alpha = if (position < total - 1) 1.0f else 0.35f
            com.eve.app.designsystem.DesignPress.bind(b.btnMoveDown) { onMoveDown(banner) }

            b.btnDeleteBanner.rippleColor = ColorStateList.valueOf(palette.actionSelector)
            com.eve.app.designsystem.DesignPress.bind(b.btnDeleteBanner) { onDelete(banner) }
        }
    }
}
