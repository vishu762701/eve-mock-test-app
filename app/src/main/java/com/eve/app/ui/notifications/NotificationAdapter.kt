package com.eve.app.ui.notifications

import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.databinding.ItemNotificationBinding
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.StoredNotification

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.VH>() {

    private var items: List<StoredNotification> = emptyList()

    fun submit(list: List<StoredNotification>) {
        val diffCallback = object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = list.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val o = items[oldItemPosition]
                val n = list[newItemPosition]
                return o.timestampMillis == n.timestampMillis && o.title == n.title
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == list[newItemPosition]
            }
        }
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        items = list
        diffResult.dispatchUpdatesTo(this)
    }

    inner class VH(private val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: StoredNotification) {
            val palette = TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(palette.accent)
            b.tvTitle.setTextColor(palette.textPrimary)
            b.tvBody.setTextColor(palette.textSecondary)
            b.tvTime.setTextColor(palette.textSecondary)
            com.eve.app.ui.admin.emoji.EmojiRenderer.attachEmoji(b.tvTitle, item.title, item.emoji)
            b.tvBody.text = item.body
            b.tvTime.text = DateUtils.getRelativeTimeSpanString(
                item.timestampMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        b.root.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
            8 * parent.resources.displayMetrics.density,
            TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
