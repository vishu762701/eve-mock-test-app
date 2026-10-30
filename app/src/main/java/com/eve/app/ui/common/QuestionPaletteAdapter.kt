package com.eve.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.designsystem.DesignMotion
import org.telegram.ui.Components.CubicBezierInterpolator
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
    private val progressiveMotion: Boolean = false,
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
    private val pendingReveals = mutableSetOf<Int>()
    private var pendingActiveNumber: Int? = null

    /** Test-only ADAPTED reveal choreography.
     * Physics atom: uploaded Telegram FilterTabsView.java:837,855-881:
     * CubicBezierInterpolator.EASE_OUT_QUINT, 320ms. Translation/scale are
     * deliberately EVE-specific, NOT an exact upstream numbered-strip view. */
    fun submit(newItems: List<PaletteItem>) {
        if (!progressiveMotion) {
            items = newItems
            notifyDataSetChanged()
            return
        }
        val previous = items
        val retainsPrefix = previous.isNotEmpty() && newItems.size >= previous.size &&
            previous.indices.all { previous[it].number == newItems[it].number }
        if (!retainsPrefix) {
            pendingReveals.clear()
            pendingActiveNumber = null
            items = newItems
            notifyDataSetChanged()
            return
        }
        val oldActive = previous.firstOrNull { it.isActive }?.number
        val newActive = newItems.firstOrNull { it.isActive }?.number
        pendingActiveNumber = if (oldActive != newActive) newActive else null
        val appended = newItems.drop(previous.size).map { it.number }
        pendingReveals.addAll(appended)
        items = newItems
        notifyItemRangeChanged(0, previous.size)
        if (appended.isNotEmpty()) notifyItemRangeInserted(previous.size, appended.size)
    }

    private fun settle(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
    }

    private fun animateNewState(holder: VH, item: PaletteItem) {
        val view = holder.itemView
        settle(view)
        if (!progressiveMotion) return
        val revealing = pendingReveals.remove(item.number)
        val activating = pendingActiveNumber == item.number
        if (activating) pendingActiveNumber = null
        if ((!revealing && !activating) || !DesignMotion.enabled(view.context)) return
        // Vertical entry is layout-direction neutral; colors and touch geometry
        // remain unchanged. Only newly appended circles animate their opacity.
        if (revealing) {
            view.alpha = 0f
            view.translationY = 4f * view.resources.displayMetrics.density
            view.scaleX = 0.92f
            view.scaleY = 0.92f
        } else {
            view.scaleX = 0.96f
            view.scaleY = 0.96f
        }
        val number = item.number
        view.post {
            val position = holder.bindingAdapterPosition
            if (!view.isAttachedToWindow || position == RecyclerView.NO_POSITION ||
                items.getOrNull(position)?.number != number) {
                settle(view)
                return@post
            }
            view.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(320L)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .start()
        }
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
                        textColor = ContextCompat.getColor(ctx, R.color.system_green_text)
                        strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                    }
                    PaletteState.WRONG -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.system_red_container)
                        textColor = ContextCompat.getColor(ctx, R.color.system_red_text)
                        strokeColor = ContextCompat.getColor(ctx, R.color.hairline)
                    }
                    PaletteState.MARKED,
                    PaletteState.ANSWERED_MARKED -> {
                        bgColor = ContextCompat.getColor(ctx, R.color.system_orange_container)
                        textColor = ContextCompat.getColor(ctx, R.color.system_orange_text)
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
        val item = items[position]
        holder.bind(item, position)
        animateNewState(holder, item)
    }

    override fun onViewRecycled(holder: VH) {
        settle(holder.itemView)
        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        settle(holder.itemView)
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = items.size
}
