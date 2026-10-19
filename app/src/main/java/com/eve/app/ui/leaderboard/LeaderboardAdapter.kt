package com.eve.app.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.databinding.ItemLeaderboardBinding

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

            b.tvRank.text = "#$rank"
            when (rank) {
                1 -> {
                    com.eve.app.designsystem.DesignSurfaces.badge(b.tvRank, R.drawable.design_badge_orange)
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.system_orange))
                }
                2, 3 -> {
                    com.eve.app.designsystem.DesignSurfaces.badge(b.tvRank, R.drawable.design_badge_neutral)
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                }
                else -> {
                    b.tvRank.background = null
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                    b.tvRank.setPadding(0, 0, 0, 0)
                }
            }

            val avatar = com.eve.app.util.AvatarDrawable.create(ctx, entry.displayName, entry.userId)
            (b.tvAvatarInitial.parent as? android.view.View)?.background = avatar
            b.tvAvatarInitial.text = ""
            b.tvScore.text = "${entry.scoreText}/${entry.total}"

            val isMe = currentUserId != null && entry.userId == currentUserId
            b.tvName.text = if (isMe) "${entry.displayName} (You)" else entry.displayName
            b.root.setCardBackgroundColor(
                ContextCompat.getColor(ctx, if (isMe) R.color.system_blue_container else R.color.surface_inset)
            )
            b.root.strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
            b.root.strokeWidth = 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    override fun getItemCount() = items.size
}
