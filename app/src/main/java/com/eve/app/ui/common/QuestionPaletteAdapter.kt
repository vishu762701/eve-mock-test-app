package com.eve.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.databinding.ItemPaletteCircleBinding

enum class PaletteState {
    UNATTEMPTED,
    ANSWERED,
    CORRECT,
    WRONG,
    VISITED,
    MARKED,
    ANSWERED_MARKED
}

data class SubmitDialogCounts(
    val answered: Int,
    val notAnswered: Int,
    val markedForReview: Int,
    val notVisited: Int
)

data class PaletteItem(
    val number: Int,
    val state: PaletteState = PaletteState.UNATTEMPTED,
    val isActive: Boolean = false
)

class QuestionPaletteAdapter(
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<QuestionPaletteAdapter.VH>() {

    companion object {
        fun mapPaletteState(answered: Boolean, visited: Boolean, marked: Boolean): PaletteState = when {
            marked && answered -> PaletteState.ANSWERED_MARKED
            marked -> PaletteState.MARKED
            answered -> PaletteState.ANSWERED
            visited -> PaletteState.VISITED
            else -> PaletteState.UNATTEMPTED
        }

        fun calculateSubmitDialogCounts(
            totalQuestions: Int,
            answeredIndices: Set<Int>,
            visitedIndices: Set<Int>,
            markedIndices: Set<Int>
        ): SubmitDialogCounts {
            val answered = (0 until totalQuestions).count { answeredIndices.contains(it) }
            val notAnswered = totalQuestions - answered
            val marked = (0 until totalQuestions).count { markedIndices.contains(it) }
            val notVisited = (0 until totalQuestions).count { !visitedIndices.contains(it) }
            return SubmitDialogCounts(answered, notAnswered, marked, notVisited)
        }
    }

    private var items: List<PaletteItem> = emptyList()

    fun submit(newItems: List<PaletteItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemPaletteCircleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: PaletteItem, position: Int) {
            val ctx = b.root.context
            val density = ctx.resources.displayMetrics.density
            val stroke2dp = (2 * density).toInt().coerceAtLeast(1)
            val stroke3dp = (3 * density).toInt().coerceAtLeast(2)

            b.tvCircleNumber.text = item.number.toString()

            val bgColor: Int
            val textColor: Int
            val strokeColor: Int
            val strokeW: Int

            if (item.isActive) {
                bgColor = ContextCompat.getColor(ctx, R.color.eve_surface)
                textColor = ContextCompat.getColor(ctx, R.color.eve_text)
                strokeColor = ContextCompat.getColor(ctx, R.color.eve_text)
                strokeW = stroke2dp
            } else {
                strokeW = (1 * density).toInt().coerceAtLeast(1)
                when (item.state) {
                    PaletteState.CORRECT,
                    PaletteState.ANSWERED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.eve_tile_right_fill)
                        textColor = ContextCompat.getColor(ctx, R.color.eve_tile_right_text)
                        strokeColor = ContextCompat.getColor(ctx, R.color.eve_tile_right_border)
                    }
                    PaletteState.WRONG -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.eve_tile_wrong_fill)
                        textColor = ContextCompat.getColor(ctx, R.color.eve_tile_wrong_text)
                        strokeColor = ContextCompat.getColor(ctx, R.color.eve_tile_wrong_border)
                    }
                    PaletteState.MARKED,
                    PaletteState.ANSWERED_MARKED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.eve_tile_medium_fill)
                        textColor = ContextCompat.getColor(ctx, R.color.eve_tile_medium_text)
                        strokeColor = ContextCompat.getColor(ctx, R.color.eve_tile_medium_border)
                    }
                    PaletteState.UNATTEMPTED,
                    PaletteState.VISITED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.eve_surface_2)
                        textColor = ContextCompat.getColor(ctx, R.color.eve_text_secondary)
                        strokeColor = ContextCompat.getColor(ctx, R.color.eve_border)
                    }
                }
            }

            b.cardCircle.setCardBackgroundColor(bgColor)
            b.tvCircleNumber.setTextColor(textColor)
            b.cardCircle.strokeWidth = strokeW
            b.cardCircle.strokeColor = strokeColor
            b.cardCircle.cardElevation = 0f

            // Dot indicator for marked for review (orange dot)
            if (item.state == PaletteState.MARKED || item.state == PaletteState.ANSWERED_MARKED) {
                b.dotMarked.visibility = View.VISIBLE
            } else {
                b.dotMarked.visibility = View.GONE
            }

            b.root.setOnClickListener {
                onSelect(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPaletteCircleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}
