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
                    b.tvRank.setBackgroundResource(R.drawable.bg_tile_medium)
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.eve_tile_medium_text))
                    b.tvRank.setPadding((6 * ctx.resources.displayMetrics.density).toInt(), (2 * ctx.resources.displayMetrics.density).toInt(), (6 * ctx.resources.displayMetrics.density).toInt(), (2 * ctx.resources.displayMetrics.density).toInt())
                }
                2, 3 -> {
                    b.tvRank.setBackgroundResource(R.drawable.bg_tile_surface2)
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text_secondary))
                    b.tvRank.setPadding((6 * ctx.resources.displayMetrics.density).toInt(), (2 * ctx.resources.displayMetrics.density).toInt(), (6 * ctx.resources.displayMetrics.density).toInt(), (2 * ctx.resources.displayMetrics.density).toInt())
                }
                else -> {
                    b.tvRank.background = null
                    b.tvRank.setTextColor(ContextCompat.getColor(ctx, R.color.eve_text_secondary))
                    b.tvRank.setPadding(0, 0, 0, 0)
                }
            }

            val avatar = com.eve.app.util.AvatarDrawable.create(entry.displayName, entry.userId)
            (b.tvAvatarInitial.parent as? android.view.View)?.background = avatar
            b.tvAvatarInitial.text = ""
            b.tvScore.text = "${entry.scoreText}/${entry.total}"

            val isMe = currentUserId != null && entry.userId == currentUserId
            b.tvName.text = if (isMe) "${entry.displayName} (You)" else entry.displayName
            b.root.setCardBackgroundColor(
                ContextCompat.getColor(ctx, R.color.eve_surface)
            )
            b.root.strokeColor = ContextCompat.getColor(ctx, R.color.eve_shape_border)
            b.root.strokeWidth = Math.round(ctx.resources.displayMetrics.density).coerceAtLeast(1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    override fun getItemCount() = items.size
}
