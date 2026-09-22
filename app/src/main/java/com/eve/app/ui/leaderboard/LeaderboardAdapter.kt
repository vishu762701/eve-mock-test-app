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
                        1 -> R.color.eve_gold
                        2 -> R.color.eve_silver
                        3 -> R.color.eve_bronze
                        else -> R.color.eve_grey
                    }
                )
            )

            b.tvAvatarInitial.text = entry.initial
            b.tvScore.text = "${entry.scoreText}/${entry.total}"

            val isMe = currentUserId != null && entry.userId == currentUserId
            b.tvName.text = if (isMe) "${entry.displayName} (You)" else entry.displayName
            // View recycle hote hain isliye highlight aur normal dono states explicitly set
            // karni padti hain (warna scroll karne par purani highlight reuse ho jaati hai).
            b.root.setCardBackgroundColor(
                ContextCompat.getColor(ctx, if (isMe) R.color.eve_highlight_bg else R.color.eve_card_bg)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    override fun getItemCount() = items.size
}
