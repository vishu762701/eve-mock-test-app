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

            val bgColor = when (item.state) {
                PaletteState.CORRECT -> ContextCompat.getColor(ctx, R.color.eve_status_success)
                PaletteState.WRONG -> ContextCompat.getColor(ctx, R.color.eve_status_error)
                PaletteState.ANSWERED -> ContextCompat.getColor(ctx, R.color.eve_primary)
                PaletteState.UNATTEMPTED -> ContextCompat.getColor(ctx, R.color.eve_surface_variant)
                PaletteState.VISITED -> ContextCompat.getColor(ctx, android.R.color.transparent)
                PaletteState.MARKED -> ContextCompat.getColor(ctx, R.color.eve_status_warning)
                PaletteState.ANSWERED_MARKED -> ContextCompat.getColor(ctx, R.color.eve_primary)
            }

            val textColor = when (item.state) {
                PaletteState.CORRECT,
                PaletteState.WRONG,
                PaletteState.ANSWERED,
                PaletteState.MARKED,
                PaletteState.ANSWERED_MARKED -> ContextCompat.getColor(ctx, R.color.eve_on_primary)
                PaletteState.UNATTEMPTED -> ContextCompat.getColor(ctx, R.color.eve_text_secondary)
                PaletteState.VISITED -> ContextCompat.getColor(ctx, R.color.eve_status_error)
            }

            b.cardCircle.setCardBackgroundColor(bgColor)
            b.tvCircleNumber.setTextColor(textColor)

            // Dot indicator for ANSWERED_MARKED
            if (item.state == PaletteState.ANSWERED_MARKED) {
                b.dotMarked.visibility = View.VISIBLE
            } else {
                b.dotMarked.visibility = View.GONE
            }

            // Stroke and elevation
            if (item.isActive) {
                b.cardCircle.strokeWidth = stroke3dp
                b.cardCircle.strokeColor = ContextCompat.getColor(ctx, R.color.eve_primary)
                b.cardCircle.cardElevation = 4f
            } else {
                if (item.state == PaletteState.VISITED) {
                    b.cardCircle.strokeWidth = stroke2dp
                    b.cardCircle.strokeColor = ContextCompat.getColor(ctx, R.color.eve_status_error)
                } else {
                    b.cardCircle.strokeWidth = (1 * density).toInt().coerceAtLeast(1)
                    b.cardCircle.strokeColor = ContextCompat.getColor(ctx, R.color.eve_stroke)
                }
                b.cardCircle.cardElevation = 0f
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
