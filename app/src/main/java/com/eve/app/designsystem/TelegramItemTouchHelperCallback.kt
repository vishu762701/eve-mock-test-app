/*
 * Derived from Telegram Android PollCreateActivity.java:176-230, ChangeUsernameActivity.java:1120-1165
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.util.VibrationHelper
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Authentic Telegram drag-to-reorder and swipe touch helper callback.
 * Upstream: PollCreateActivity.java:176-230, ChangeUsernameActivity.java:1120-1165
 * Features:
 * - 1.03x scale lift and 8dp elevation on drag start with light haptic feedback
 * - Smooth spring settle back on drop
 * - Item swapping with swap callback
 */
open class TelegramItemTouchHelperCallback(
    private val onMoveItem: (fromPosition: Int, toPosition: Int) -> Boolean,
    private val onDragStateChanged: ((isDragging: Boolean) -> Unit)? = null,
    private val canDragItem: ((position: Int) -> Boolean)? = null,
    private val isDragEnabled: Boolean = true
) : ItemTouchHelper.Callback() {

    private var isCurrentlyDragging: Boolean = false

    override fun isLongPressDragEnabled(): Boolean = isDragEnabled

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.adapterPosition
        if (canDragItem != null && !canDragItem.invoke(position)) {
            return makeMovementFlags(0, 0)
        }
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPos = source.adapterPosition
        val toPos = target.adapterPosition
        if (canDragItem != null && (!canDragItem.invoke(fromPos) || !canDragItem.invoke(toPos))) {
            return false
        }
        val success = onMoveItem(fromPos, toPos)
        if (success) {
            VibrationHelper.vibrateLightHaptic(recyclerView.context)
        }
        return success
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Subclasses may implement swipe actions
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            isCurrentlyDragging = true
            VibrationHelper.vibrateLightHaptic(viewHolder.itemView.context)
            onDragStateChanged?.invoke(true)

            // Telegram lift animation: scale 1.03f, elevation 8dp
            viewHolder.itemView.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .translationZ(viewHolder.itemView.resources.displayMetrics.density * 8f)
                .setDuration(150L)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT)
                .start()
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (isCurrentlyDragging) {
                isCurrentlyDragging = false
                onDragStateChanged?.invoke(false)
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Telegram drop settling: scale 1.0f, elevation 0
        viewHolder.itemView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .translationZ(0f)
            .setDuration(180L)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .start()
    }
}
