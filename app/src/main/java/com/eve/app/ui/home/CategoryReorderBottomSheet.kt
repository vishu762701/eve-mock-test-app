/*
 * Derived from Telegram Android FiltersView.java, FilterCreateActivity.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.eve.app.R
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.VibrationHelper

/**
 * Authentic Telegram folder / category reordering bottom sheet.
 * Upstream: FiltersView.java, FilterCreateActivity.java
 */
class CategoryReorderBottomSheet(
    context: Context,
    private val categories: List<String>,
    private val onReordered: (List<String>) -> Unit
) : TelegramBottomSheet(context) {

    private fun dp(v: Float): Int = (v * context.resources.displayMetrics.density + 0.5f).toInt()

    private val workingList = categories.filter { it != Constants.CATEGORY_ALL }.toMutableList()
    private val listContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    override fun onCreateContentView(): View {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(8f), dp(20f), dp(20f))
        }

        // Header Title
        val tvTitle = TextView(context).apply {
            text = "Edit Folders"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4f))
        }
        root.addView(tvTitle)

        // Subtitle
        val tvSub = TextView(context).apply {
            text = "Reorder categories to customize your Home tabs"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16f))
        }
        root.addView(tvSub)

        // Scrollable category list
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240f)
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        refreshList()
        scrollView.addView(listContainer)
        root.addView(scrollView)

        // Button row: Reset and Save
        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16f), 0, 0)
        }

        val btnReset = AppCompatButton(context).apply {
            text = "Reset"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
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
            typeface = Typeface.DEFAULT_BOLD
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

    private fun refreshList() {
        listContainer.removeAllViews()
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        // Pinned "All" item
        val allRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(10f), dp(8f), dp(10f))
        }
        val tvAll = TextView(context).apply {
            text = "All (Pinned)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.textSecondary)
        }
        allRow.addView(tvAll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        listContainer.addView(allRow)

        // Separator
        val divider = View(context).apply {
            setBackgroundColor(palette.hairline)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(dp(8f), dp(4f), dp(8f), dp(4f))
            }
        }
        listContainer.addView(divider)

        for (index in workingList.indices) {
            val category = workingList[index]
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8f).toFloat()
                    setColor(palette.surface)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(4f), 0, dp(4f))
                }
            }

            val tvName = TextView(context).apply {
                text = category
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(palette.textPrimary)
            }
            row.addView(tvName, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Move Up button
            if (index > 0) {
                val btnUp = ImageView(context).apply {
                    setImageResource(R.drawable.ic_back)
                    rotation = 90f
                    setColorFilter(palette.accent)
                    setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
                    isClickable = true
                    setOnClickListener {
                        VibrationHelper.vibrateLightHaptic(context)
                        val item = workingList.removeAt(index)
                        workingList.add(index - 1, item)
                        refreshList()
                    }
                }
                row.addView(btnUp, LinearLayout.LayoutParams(dp(36f), dp(36f)))
            }

            // Move Down button
            if (index < workingList.size - 1) {
                val btnDown = ImageView(context).apply {
                    setImageResource(R.drawable.ic_back)
                    rotation = 270f
                    setColorFilter(palette.accent)
                    setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
                    isClickable = true
                    setOnClickListener {
                        VibrationHelper.vibrateLightHaptic(context)
                        val item = workingList.removeAt(index)
                        workingList.add(index + 1, item)
                        refreshList()
                    }
                }
                row.addView(btnDown, LinearLayout.LayoutParams(dp(36f), dp(36f)))
            }

            listContainer.addView(row)
        }
    }

    companion object {
        fun show(context: Context, categories: List<String>, onReordered: (List<String>) -> Unit) {
            val sheet = CategoryReorderBottomSheet(context, categories, onReordered)
            sheet.show()
        }
    }
}
