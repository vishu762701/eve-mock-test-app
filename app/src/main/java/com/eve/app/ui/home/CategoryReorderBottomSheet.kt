/*
 * Derived from Telegram Android FiltersView.java, FilterCreateActivity.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.designsystem.TelegramItemTouchHelperCallback
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.FontManager
import com.eve.app.util.VibrationHelper
import java.util.Collections

/**
 * Authentic Telegram folder / category reordering bottom sheet.
 * Upstream: FiltersView.java, FilterCreateActivity.java
 * Supports drag-to-reorder via TelegramItemTouchHelperCallback with authentic tabs_reorder handle and lift animation.
 */
class CategoryReorderBottomSheet(
    context: Context,
    private val categories: List<String>,
    private val onReordered: (List<String>) -> Unit
) : TelegramBottomSheet(context) {

    private fun dp(v: Float): Int = (v * context.resources.displayMetrics.density + 0.5f).toInt()

    private val workingList = categories.filter { it != Constants.CATEGORY_ALL }.toMutableList()
    private lateinit var adapter: CategoryOrderAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    init {
        setCustomView(buildContentView())
    }

    private fun buildContentView(): View {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(8f), dp(20f), dp(20f))
        }

        // Header Title
        val tvTitle = TextView(context).apply {
            text = "Edit Folders"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            typeface = FontManager.telegramBoldTypeface(context)
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4f))
        }
        root.addView(tvTitle)

        // Subtitle
        val tvSub = TextView(context).apply {
            text = "Reorder categories to customize your Home tabs"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = FontManager.telegramRegularTypeface(context)
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16f))
        }
        root.addView(tvSub)

        // Pinned "All" item header row
        val allRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(8f).toFloat()
                setColor(palette.surfaceInset)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8f)
            }
        }
        val tvAll = TextView(context).apply {
            text = "All (Pinned)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = FontManager.telegramMediumTypeface(context)
            setTextColor(palette.textSecondary)
        }
        allRow.addView(tvAll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(allRow)

        // RecyclerView with drag-to-reorder
        val recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240f)
            )
            layoutManager = LinearLayoutManager(context)
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        adapter = CategoryOrderAdapter(
            items = workingList,
            palette = palette,
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )
        recyclerView.adapter = adapter

        val callback = TelegramItemTouchHelperCallback(
            onMoveItem = { fromPos, toPos ->
                if (fromPos < 0 || toPos < 0 || fromPos >= workingList.size || toPos >= workingList.size) {
                    false
                } else {
                    Collections.swap(workingList, fromPos, toPos)
                    adapter.notifyItemMoved(fromPos, toPos)
                    true
                }
            }
        )
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        root.addView(recyclerView)

        // Button row: Reset and Save
        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16f), 0, 0)
        }

        val btnReset = AppCompatButton(context).apply {
            text = "Reset"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = FontManager.telegramMediumTypeface(context)
            setTextColor(palette.accent)
            background = null
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            setOnClickListener {
                VibrationHelper.vibrateLightHaptic(context)
                CategoryOrderManager.resetOrder(context)
                dismiss()
                onReordered(emptyList())
                AppBulletin.show(context, "Categories reset to default")
            }
        }
        buttonRow.addView(btnReset, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val btnDone = AppCompatButton(context).apply {
            text = "Done"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = FontManager.telegramBoldTypeface(context)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(palette.accent)
            }
            setPadding(dp(24f), dp(10f), dp(24f), dp(10f))
            setOnClickListener {
                VibrationHelper.vibrateLightHaptic(context)
                CategoryOrderManager.saveOrder(context, workingList)
                dismiss()
                val fullOrdered = listOf(Constants.CATEGORY_ALL) + workingList
                onReordered(fullOrdered)
                AppBulletin.show(context, "Category order saved")
            }
        }
        buttonRow.addView(btnDone, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(buttonRow)
        return root
    }

    private class CategoryViewHolder(
        val row: LinearLayout,
        val tvName: TextView,
        val btnReorder: ImageView
    ) : RecyclerView.ViewHolder(row)

    private class CategoryOrderAdapter(
        private val items: MutableList<String>,
        private val palette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.Adapter<CategoryViewHolder>() {

        override fun getItemCount(): Int = items.size

        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val context = parent.context
            val density = context.resources.displayMetrics.density
            fun dp(v: Float): Int = (v * density + 0.5f).toInt()

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8f).toFloat()
                    setColor(palette.surface)
                }
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(3f), 0, dp(3f))
                }
            }

            val tvName = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = FontManager.telegramMediumTypeface(context)
                setTextColor(palette.textPrimary)
            }
            row.addView(tvName, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val btnReorder = ImageView(context).apply {
                setImageResource(R.drawable.tabs_reorder)
                setColorFilter(palette.textSecondary)
                layoutParams = LinearLayout.LayoutParams(dp(28f), dp(28f))
                setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
            }
            row.addView(btnReorder)

            val holder = CategoryViewHolder(row, tvName, btnReorder)
            btnReorder.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(holder)
                }
                false
            }
            return holder
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.tvName.text = items[position]
        }
    }

    companion object {
        fun show(context: Context, categories: List<String>, onReordered: (List<String>) -> Unit) {
            val sheet = CategoryReorderBottomSheet(context, categories, onReordered)
            sheet.show()
        }
    }
}
