/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBox2.java:45-210 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/RadioButton.java:30-160 @ 3de37c8cfb03
 * Derived from Telegram Android FilterCreateActivity.java and ActionBarMenuItem.java
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * 1:1 Telegram expandable option selector component.
 * Expand/collapse transition with CubicBezierInterpolator.EASE_OUT_QUINT and Telegram styling.
 */
open class TelegramMorphSelectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    data class Option(
        val id: String,
        val label: String,
        val meta: String? = null,
        val iconRes: Int? = null
    )

    companion object {
        const val SEARCH_THRESHOLD = 8
    }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val triggerCard: FrameLayout
    private val tvSelectedLabel: TextView
    private val tvSelectedMeta: TextView
    private val ivChevron: ImageView

    private val panelContainer: LinearLayout
    private val searchBox: EditText
    private val optionsScrollView: ScrollView
    private val optionsLayout: LinearLayout
    private val tvError: TextView

    private val allOptions = mutableListOf<Option>()
    private val filteredOptions = mutableListOf<Option>()
    private var selectedOption: Option? = null
    private var isOpen = false
    private var isAnimating = false

    var onOptionSelectedListener: ((Option) -> Unit)? = null

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        triggerCard = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            minimumHeight = dp(52f)
            setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(palette.surfaceInset)
                setStroke(dp(1f), ContextCompat.getColor(context, R.color.hairline))
            }
            setOnClickListener { toggle() }
        }

        val triggerContent = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val textBlock = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        tvSelectedLabel = TextView(context).apply {
            typeface = FontManager.telegramMediumTypeface(context)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(palette.textPrimary)
            text = "Select option…"
        }

        tvSelectedMeta = TextView(context).apply {
            typeface = FontManager.regularTypeface(context)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(palette.textSecondary)
            visibility = View.GONE
        }

        textBlock.addView(tvSelectedLabel)
        textBlock.addView(tvSelectedMeta)
        triggerContent.addView(textBlock)

        ivChevron = ImageView(context).apply {
            setImageResource(R.drawable.msg_arrowright)
            rotation = 90f
            setColorFilter(palette.textSecondary)
            layoutParams = LayoutParams(dp(20f), dp(20f)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }
        triggerContent.addView(ivChevron)
        triggerCard.addView(triggerContent)
        addView(triggerCard)

        panelContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(palette.surface)
                setStroke(dp(1f), ContextCompat.getColor(context, R.color.hairline))
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6f)
            }
        }

        searchBox = EditText(context).apply {
            hint = "Search…"
            visibility = View.GONE
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                dp(8f).toFloat(),
                palette.surfaceInset
            )
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterOptions(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        panelContainer.addView(searchBox)

        optionsScrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        optionsLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        optionsScrollView.addView(optionsLayout)
        panelContainer.addView(optionsScrollView)
        addView(panelContainer)

        tvError = TextView(context).apply {
            visibility = View.GONE
            setTextColor(ContextCompat.getColor(context, R.color.system_red))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(16f), dp(4f), dp(16f), 0)
        }
        addView(tvError)
    }

    fun setError(error: CharSequence?) {
        if (error.isNullOrBlank()) {
            tvError.visibility = View.GONE
            tvError.text = null
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = error
        }
    }

    fun setOptions(items: List<Option>, selectedId: String? = null) {
        allOptions.clear()
        allOptions.addAll(items)
        filteredOptions.clear()
        filteredOptions.addAll(items)

        searchBox.visibility = if (items.size > SEARCH_THRESHOLD) View.VISIBLE else View.GONE
        rebuildOptionViews()

        if (selectedId != null) {
            val match = items.firstOrNull { it.id == selectedId }
            if (match != null) {
                setSelectionInternal(match)
            } else {
                clearSelection()
            }
        } else {
            clearSelection()
        }
    }

    private fun filterOptions(query: String) {
        filteredOptions.clear()
        if (query.isBlank()) {
            filteredOptions.addAll(allOptions)
        } else {
            val q = query.trim().lowercase()
            filteredOptions.addAll(allOptions.filter {
                it.label.lowercase().contains(q) || (it.meta?.lowercase()?.contains(q) == true)
            })
        }
        rebuildOptionViews()
    }

    private fun rebuildOptionViews() {
        optionsLayout.removeAllViews()
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        for (opt in filteredOptions) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
                background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                    dp(8f).toFloat(),
                    palette.actionSelector
                )
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    selectOption(opt)
                    close()
                }
            }

            val tv = TextView(context).apply {
                text = opt.label
                typeface = FontManager.regularTypeface(context)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(palette.textPrimary)
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tv)

            if (!opt.meta.isNullOrBlank()) {
                val tvM = TextView(context).apply {
                    text = opt.meta
                    typeface = FontManager.regularTypeface(context)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(palette.textSecondary)
                }
                row.addView(tvM)
            }

            optionsLayout.addView(row)
        }
    }

    private fun setSelectionInternal(option: Option) {
        selectedOption = option
        tvSelectedLabel.text = option.label
        if (!option.meta.isNullOrBlank()) {
            tvSelectedMeta.text = option.meta
            tvSelectedMeta.visibility = View.VISIBLE
        } else {
            tvSelectedMeta.visibility = View.GONE
        }
    }

    fun selectOption(option: Option) {
        setSelectionInternal(option)
        setError(null)
        onOptionSelectedListener?.invoke(option)
    }

    fun clearSelection() {
        selectedOption = null
        tvSelectedLabel.text = "Select option…"
        tvSelectedMeta.visibility = View.GONE
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun open() {
        if (isOpen || isAnimating) return
        isOpen = true
        panelContainer.visibility = View.VISIBLE

        if (!TelegramAnimationRuntime.isMotionEnabled(context)) {
            ivChevron.rotation = 270f
            panelContainer.alpha = 1f
            return
        }

        isAnimating = true
        ivChevron.animate().rotation(270f).setDuration(240L).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start()
        panelContainer.alpha = 0f
        panelContainer.translationY = -dp(6f).toFloat()
        panelContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240L)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                }
            })
            .start()
    }

    fun close() {
        if (!isOpen || isAnimating) return
        isOpen = false

        if (!TelegramAnimationRuntime.isMotionEnabled(context)) {
            ivChevron.rotation = 90f
            panelContainer.visibility = View.GONE
            return
        }

        isAnimating = true
        ivChevron.animate().rotation(90f).setDuration(200L).setInterpolator(CubicBezierInterpolator.DEFAULT).start()
        panelContainer.animate()
            .alpha(0f)
            .translationY(-dp(6f).toFloat())
            .setDuration(200L)
            .setInterpolator(CubicBezierInterpolator.DEFAULT)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    panelContainer.visibility = View.GONE
                    isAnimating = false
                }
            })
            .start()
    }

    fun getSelectedOption(): Option? = selectedOption
    fun isExpanded(): Boolean = isOpen
}
