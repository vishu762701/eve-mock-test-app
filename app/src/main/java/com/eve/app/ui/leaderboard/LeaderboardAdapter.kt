package com.eve.app.ui.leaderboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.databinding.ItemLeaderboardBinding
import com.eve.app.theme.TelegramThemeEngine

class LeaderboardAdapter(
    private val currentUserId: String?
) : RecyclerView.Adapter<LeaderboardAdapter.VH>() {

    private var items: List<LeaderboardEntry> = emptyList()

    fun submit(list: List<LeaderboardEntry>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemLeaderboardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: LeaderboardEntry, rank: Int) {
            val ctx = b.root.context
            val palette = TelegramThemeEngine.resolveCurrentPalette(ctx)

            b.tvRank.text = "#$rank"
            when (rank) {
                1 -> {
                    com.eve.app.designsystem.DesignSurfaces.badge(b.tvRank, R.drawable.design_badge_orange)
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.system_orange_text))
                }
                2, 3 -> {
                    com.eve.app.designsystem.DesignSurfaces.badge(b.tvRank, R.drawable.design_badge_neutral)
                    b.tvRank.setTextColor(palette.textSecondary)
                }
                else -> {
                    b.tvRank.background = null
                    b.tvRank.setTextColor(palette.textSecondary)
                    b.tvRank.setPadding(0, 0, 0, 0)
                }
            }

            val avatar = com.eve.app.util.AvatarDrawable.create(ctx, entry.displayName, entry.userId)
            (b.tvAvatarInitial.parent as? android.view.View)?.background = avatar
            b.tvAvatarInitial.text = ""
            b.tvScore.text = "${entry.scoreText}/${entry.total}"

            val isMe = currentUserId != null && entry.userId == currentUserId
            b.tvName.text = if (isMe) "${entry.displayName} (You)" else entry.displayName
            b.tvName.setTextColor(palette.textPrimary)
            b.tvScore.setTextColor(if (isMe) palette.accent else palette.textPrimary)
            b.root.setCardBackgroundColor(
                if (isMe) (if (palette.isDark) 0x33007AFF else 0x1F007AFF) else palette.surfaceInset
            )
            b.root.strokeColor = palette.hairline
            b.root.strokeWidth = 0
            b.root.rippleColor = ColorStateList.valueOf(palette.actionSelector)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    override fun getItemCount() = items.size
}
