package com.eve.app.ui.admin.emoji

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eve.app.R
import com.eve.app.data.model.EmojiItem
import com.eve.app.databinding.ItemEmojiGridBinding

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
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EmojiViewHolder(private val b: ItemEmojiGridBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: EmojiItem) {
            b.tvAltEmoji.text = item.altEmoji
            b.ivPremiumBadge.visibility = if (item.premiumOrigin) View.VISIBLE else View.GONE

            b.root.setOnClickListener { onItemClick(item) }
            b.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }

            // Reset views
            b.lottieView.visibility = View.GONE
            b.ivStaticEmoji.visibility = View.GONE
            b.tvAltEmoji.visibility = View.VISIBLE

            if (item.kind == "static" && item.url.isNotBlank()) {
                b.ivStaticEmoji.visibility = View.VISIBLE
                b.ivStaticEmoji.load(item.url) {
                    listener(onSuccess = { _, _ ->
                        if (bindingAdapterPosition != RecyclerView.NO_POSITION && items[bindingAdapterPosition].id == item.id) {
                            b.tvAltEmoji.visibility = View.GONE
                        }
                    })
                }
            } else if (item.url.startsWith("res:raw/")) {
                val rawName = item.url.removePrefix("res:raw/")
                val resId = b.root.context.resources.getIdentifier(rawName, "raw", b.root.context.packageName)
                if (resId != 0) {
                    b.lottieView.visibility = View.VISIBLE
                    b.tvAltEmoji.visibility = View.GONE
                    b.lottieView.setAnimation(resId)
                    b.lottieView.playAnimation()
                }
            } else if (item.kind == "lottie" && item.url.isNotBlank()) {
                b.lottieView.visibility = View.VISIBLE
                b.tvAltEmoji.visibility = View.GONE
                b.lottieView.setAnimationFromUrl(item.url)
                b.lottieView.playAnimation()
            }
        }
    }
}
