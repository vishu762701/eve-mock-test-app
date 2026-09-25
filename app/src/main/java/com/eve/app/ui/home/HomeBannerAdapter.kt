package com.eve.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.HomeBanner
import com.eve.app.databinding.ItemHomeBannerBinding
import com.eve.app.util.ExamImageHelper

/**
 * Task A: Adapter for the student-side horizontal swipeable banner carousel (ViewPager2).
 */
class HomeBannerAdapter(
    private val onClick: ((HomeBanner) -> Unit)? = null
) : RecyclerView.Adapter<HomeBannerAdapter.BannerVH>() {

    private var items: List<HomeBanner> = emptyList()

    fun submitList(list: List<HomeBanner>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
        val binding = ItemHomeBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerVH(binding)
    }

    override fun onBindViewHolder(holder: BannerVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BannerVH(private val b: ItemHomeBannerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(banner: HomeBanner) {
            ExamImageHelper.loadBannerImage(b.ivBanner, banner.imageUrl)
            b.root.setOnClickListener { onClick?.invoke(banner) }
        }
    }
}
