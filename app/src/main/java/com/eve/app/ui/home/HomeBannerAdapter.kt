package com.eve.app.ui.home

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.HomeBanner
import com.eve.app.util.ExamImageHelper
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme

/** Programmatic Telegram-styled banner pager adapter (Pass 3a, no XML inflation). */
class HomeBannerAdapter(
    private val onBannerClick: ((HomeBanner) -> Unit)? = null
) : RecyclerView.Adapter<HomeBannerAdapter.BannerVH>() {

    private var items: List<HomeBanner> = emptyList()

    fun submitList(list: List<HomeBanner>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
        val ctx = parent.context
        val root = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        val iv = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(iv)
        return BannerVH(root, iv)
    }

    override fun onBindViewHolder(holder: BannerVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BannerVH(
        private val container: FrameLayout,
        private val ivBanner: ImageView
    ) : RecyclerView.ViewHolder(container) {
        fun bind(banner: HomeBanner) {
            ExamImageHelper.loadBannerImage(ivBanner, banner.imageUrl)
            if (onBannerClick != null) {
                container.foreground = Theme.createRadSelectorDrawable(
                    Theme.getColor(Theme.key_listSelector),
                    12,
                    12
                )
                container.setOnClickListener { onBannerClick.invoke(banner) }
            } else {
                container.foreground = null
                container.setOnClickListener(null)
            }
        }
    }
}
