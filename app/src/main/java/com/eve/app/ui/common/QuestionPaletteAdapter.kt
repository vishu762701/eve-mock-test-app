package com.eve.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.databinding.ItemPaletteCircleBinding

enum class PaletteState {
    UNATTEMPTED,
    ANSWERED,
    CORRECT,
    WRONG
}

data class PaletteItem(
    val number: Int,
    val state: PaletteState = PaletteState.UNATTEMPTED,
    val isActive: Boolean = false
)

class QuestionPaletteAdapter(
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<QuestionPaletteAdapter.VH>() {

    private var items: List<PaletteItem> = emptyList()

    fun submit(newItems: List<PaletteItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemPaletteCircleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: PaletteItem, position: Int) {
            val ctx = b.root.context
            b.tvCircleNumber.text = item.number.toString()

            val bgColor = when (item.state) {
                PaletteState.CORRECT -> ContextCompat.getColor(ctx, R.color.eve_green)
                PaletteState.WRONG -> ContextCompat.getColor(ctx, R.color.eve_red)
                PaletteState.ANSWERED -> ContextCompat.getColor(ctx, R.color.eve_primary)
                PaletteState.UNATTEMPTED -> ContextCompat.getColor(ctx, R.color.eve_surface_variant)
            }

            val textColor = when (item.state) {
                PaletteState.CORRECT, PaletteState.WRONG, PaletteState.ANSWERED -> ContextCompat.getColor(ctx, R.color.eve_on_primary)
                PaletteState.UNATTEMPTED -> ContextCompat.getColor(ctx, R.color.eve_text_secondary)
            }

            b.cardCircle.setCardBackgroundColor(bgColor)
            b.tvCircleNumber.setTextColor(textColor)

            if (item.isActive) {
                b.cardCircle.strokeWidth = 4 // 2dp approx
                b.cardCircle.strokeColor = ContextCompat.getColor(ctx, R.color.eve_primary)
                b.cardCircle.cardElevation = 6f
            } else {
                b.cardCircle.strokeWidth = 1
                b.cardCircle.strokeColor = ContextCompat.getColor(ctx, R.color.eve_stroke)
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
