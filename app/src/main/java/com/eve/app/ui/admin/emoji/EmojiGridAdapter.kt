package com.eve.app.ui.admin.emoji

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.EmojiItem
import com.eve.app.databinding.ItemEmojiGridBinding
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.ButtonBounce

class EmojiGridAdapter(
    private val onItemClick: (EmojiItem) -> Unit,
    private val onItemLongClick: (EmojiItem) -> Unit
) : RecyclerView.Adapter<EmojiGridAdapter.EmojiViewHolder>() {

    private val items = mutableListOf<EmojiItem>()

    fun submitList(newItems: List<EmojiItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemEmojiGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.background = Theme.createRadSelectorDrawable(
            Theme.getColor(Theme.key_listSelector),
            AndroidUtilities.dp(8f),
            AndroidUtilities.dp(8f)
        )
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: EmojiViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = items.size

    inner class EmojiViewHolder(private val b: ItemEmojiGridBinding) : RecyclerView.ViewHolder(b.root) {

        private val bounce = ButtonBounce(b.root)

        fun bind(item: EmojiItem) {
            b.ivPremiumBadge.visibility = if (item.premiumOrigin) View.VISIBLE else View.GONE

            b.root.setOnClickListener {
                bounce.setPressed(false)
                onItemClick(item)
            }
            b.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }

            EmojiRenderer.bindPreview(
                lottieView = b.lottieView,
                staticView = b.ivStaticEmoji,
                altView = b.tvAltEmoji,
                item = item,
                loop = true
            )
        }

        fun recycle() {
            b.lottieView.cancelAnimation()
        }
    }
}
