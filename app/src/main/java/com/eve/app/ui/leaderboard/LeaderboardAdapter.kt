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
            b.tvRank.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    when (rank) {
                        1 -> R.color.eve_premium_text
                        2 -> R.color.eve_silver
                        3 -> R.color.eve_bronze
                        else -> R.color.eve_grey
                    }
                )
            )

            val avatar = com.eve.app.util.AvatarDrawable.create(entry.displayName, entry.userId)
            (b.tvAvatarInitial.parent as? android.view.View)?.background = avatar
            b.tvAvatarInitial.text = ""
            b.tvScore.text = "${entry.scoreText}/${entry.total}"

            val isMe = currentUserId != null && entry.userId == currentUserId
            b.tvName.text = if (isMe) "${entry.displayName} (You)" else entry.displayName
            b.root.setCardBackgroundColor(
                ContextCompat.getColor(ctx, if (isMe) R.color.eve_highlight_bg else R.color.eve_card_bg)
            )
            b.root.strokeColor = ContextCompat.getColor(ctx, if (isMe) R.color.eve_premium else R.color.eve_stroke)
            b.root.strokeWidth = if (isMe) (1.5f * ctx.resources.displayMetrics.density).toInt() else (1f * ctx.resources.displayMetrics.density).toInt()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    override fun getItemCount() = items.size
}
