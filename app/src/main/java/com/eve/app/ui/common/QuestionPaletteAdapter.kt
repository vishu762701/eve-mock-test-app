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
    private val approvedTestStyle: Boolean = false,
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
            val borderWidthPx = Math.round(density).coerceAtLeast(1)

            b.tvCircleNumber.text = item.number.toString()

            val bgColor: Int
            val textColor: Int
            val strokeColor: Int
            val strokeW: Int

            if (item.isActive && !approvedTestStyle) {
                bgColor = ContextCompat.getColor(ctx, R.color.surface)
                textColor = ContextCompat.getColor(ctx, R.color.text_primary)
                strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                strokeW = borderWidthPx
            } else {
                strokeW = Math.round(density).coerceAtLeast(1)
                when (item.state) {
                    PaletteState.CORRECT,
                    PaletteState.ANSWERED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.system_green_container)
                        textColor = ContextCompat.getColor(ctx, R.color.system_green)
                        strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                    }
                    PaletteState.WRONG -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.system_red_container)
                        textColor = ContextCompat.getColor(ctx, R.color.system_red)
                        strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                    }
                    PaletteState.MARKED,
                    PaletteState.ANSWERED_MARKED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.system_orange_container)
                        textColor = ContextCompat.getColor(ctx, R.color.system_orange)
                        strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                    }
                    PaletteState.UNATTEMPTED,
                    PaletteState.VISITED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.surface)
                        textColor = ContextCompat.getColor(ctx, R.color.text_secondary)
                        strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                    }
                }
            }

            if (approvedTestStyle) {
                // Explicit padding also keeps the approved 8dp gap on API 24/25.
                val gap = ctx.resources.getDimensionPixelSize(R.dimen.design_chip_gap)
                val halfGap = gap / 2
                b.root.setPadding(halfGap, halfGap, gap - halfGap, gap - halfGap)
                b.cardCircle.layoutParams = b.cardCircle.layoutParams.apply {
                    width = ctx.resources.getDimensionPixelSize(R.dimen.design_pill_height)
                    height = ctx.resources.getDimensionPixelSize(R.dimen.design_pill_height)
                }
                b.cardCircle.radius = ctx.resources.getDimensionPixelSize(R.dimen.design_pill_height) / 2f
                com.eve.app.designsystem.DesignType.apply(b.tvCircleNumber, com.eve.app.designsystem.DesignType.Role.CHIP)
                val fill = when (item.state) {
                    PaletteState.ANSWERED -> R.color.system_green
                    PaletteState.MARKED, PaletteState.ANSWERED_MARKED -> R.color.system_yellow
                    PaletteState.UNATTEMPTED, PaletteState.VISITED -> R.color.surface_inset
                    else -> null
                }
                b.cardCircle.setCardBackgroundColor(fill?.let { ContextCompat.getColor(ctx, it) }
                    ?: bgColor)
                b.tvCircleNumber.setTextColor(if (fill == R.color.system_green || fill == R.color.system_yellow) ContextCompat.getColor(ctx, R.color.solid_black)
                    else if (fill == R.color.surface_inset) ContextCompat.getColor(ctx, R.color.text_primary) else textColor)
                b.cardCircle.strokeWidth = 0
                b.cardCircle.strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                // Active position must not erase answer/review state or add an extra outline.
                b.tvCircleNumber.typeface = if (item.isActive) com.eve.app.util.FontManager.typeface(ctx, bold = true)
                    else com.eve.app.util.FontManager.mediumTypeface(ctx)
                b.root.isSelected = item.isActive
                b.root.contentDescription = "Question ${item.number}, ${item.state.name.lowercase().replace('_', ' ')}" +
                    if (item.isActive) ", current" else ""
            } else {
                b.cardCircle.setCardBackgroundColor(bgColor)
                b.tvCircleNumber.setTextColor(textColor)
                b.cardCircle.strokeWidth = 0
                b.cardCircle.strokeColor = strokeColor
            }
            b.cardCircle.cardElevation = ctx.resources.getDimension(R.dimen.design_zero)

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
