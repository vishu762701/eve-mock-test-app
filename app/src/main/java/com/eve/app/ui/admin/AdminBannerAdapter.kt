package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.HomeBanner
import com.eve.app.databinding.ItemAdminBannerBinding
import com.eve.app.util.ExamImageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task A: Adapter for the Admin Dashboard banner management list.
 * Supports Up/Down reordering and deletion with confirmation.
 */
class AdminBannerAdapter(
    private val onMoveUp: (HomeBanner) -> Unit,
    private val onMoveDown: (HomeBanner) -> Unit,
    private val onDelete: (HomeBanner) -> Unit
) : RecyclerView.Adapter<AdminBannerAdapter.VH>() {

    private var items: List<HomeBanner> = emptyList()

    fun submitList(list: List<HomeBanner>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAdminBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position, items.size)
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemAdminBannerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(banner: HomeBanner, position: Int, total: Int) {
            b.tvBannerOrder.text = "Banner #${position + 1}"
            b.tvBannerDate.text = if (banner.uploadedAt > 0L) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(banner.uploadedAt))
            } else {
                "Active"
            }
            ExamImageHelper.loadBannerImage(b.ivBannerThumb, banner.imageUrl)

            b.btnMoveUp.isEnabled = position > 0
            b.btnMoveUp.alpha = if (position > 0) 1.0f else 0.35f
            b.btnMoveUp.setOnClickListener { onMoveUp(banner) }

            b.btnMoveDown.isEnabled = position < total - 1
            b.btnMoveDown.alpha = if (position < total - 1) 1.0f else 0.35f
            b.btnMoveDown.setOnClickListener { onMoveDown(banner) }

            b.btnDeleteBanner.setOnClickListener { onDelete(banner) }
        }
    }
}
