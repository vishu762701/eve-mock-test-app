package com.eve.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.HomeBanner
import com.eve.app.databinding.ItemHomeBannerBinding
import com.eve.app.util.ExamImageHelper

/** Pages published banners inside the single Home banner surface. */
class HomeBannerAdapter(
    private val onBannerClick: ((HomeBanner) -> Unit)? = null
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
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
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
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            if (onBannerClick != null) {
                b.root.foreground = com.eve.app.designsystem.TelegramThemeSelectors.getRoundRectSelectorDrawable(
                    12f * b.root.resources.displayMetrics.density,
                    palette.actionSelector
                )
                b.root.setOnClickListener { onBannerClick.invoke(banner) }
            } else {
                b.root.foreground = null
                b.root.setOnClickListener(null)
            }
        }
    }
}
