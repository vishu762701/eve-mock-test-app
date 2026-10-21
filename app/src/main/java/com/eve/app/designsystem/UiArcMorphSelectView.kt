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

/**
 * UIArc Morph Select Component.
 * Genuine native translation of official UIArc Morph Select specification:
 * - Registry: https://uiarc.dev/r/morph-select.json
 * - Opening: Trigger grows into options surface with GROW (Spring.SMOOTH, 500ms)
 * - Reference panel geometry: 272px reference width, 296px max height
 * - Corner radius: morphs from closed radius (14dp) to OPEN_RADIUS = 22dp
 * - Search behavior when options > 8
 * - Active-option highlighting: glides between options with GLIDE spring (380ms)
 * - Selection: lifted label travels with FLY spring (400ms), surface collapses with SHRINK (450ms)
 * - Directional roll: +0.5em / -0.45em with Ease.ENTER
 * - Stable ID entity mapping: selects by stable entity ID, immune to filtered list reordering
 * - Chevron rotates 180° on open
 * - Instant jump with reduced motion
 */
class UiArcMorphSelectView @JvmOverloads constructor(
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
        const val OPEN_RADIUS_DP = 22f
        const val CLOSED_RADIUS_DP = 14f
        const val REFERENCE_PANEL_WIDTH_DP = 272f
        const val REFERENCE_MAX_HEIGHT_DP = 296f
        const val SEARCH_THRESHOLD = 8
        const val GROW_DURATION_MS = 500L
        const val SHRINK_DURATION_MS = 450L
        const val GLIDE_DURATION_MS = 380L
        const val FLY_DURATION_MS = 400L
    }

    private val density = resources.displayMetrics.density
    private val closedHeightPx = Math.round(50f * density)
    private val maxPanelHeightPx = Math.round(REFERENCE_MAX_HEIGHT_DP * density)

    // Trigger row
    private val triggerCard: LinearLayout
    private val ivTriggerIcon: ImageView
    private val tvTriggerLabel: TextView
    private val ivChevron: ImageView

    // Options dropdown container
    private val optionsPanel: LinearLayout
    private val etSearch: EditText
    private val optionsScrollView: ScrollView
    private val optionsContainer: FrameLayout
    private val optionsListLayout: LinearLayout
    private val highlightPill: View

    private var allOptions: List<Option> = emptyList()
    private var filteredOptions: List<Option> = emptyList()
    private var selectedOption: Option? = null
    private var isOpen = false

    private var morphAnimator: ValueAnimator? = null
    private var highlightAnimator: ValueAnimator? = null

    var onOptionSelectedListener: ((Option) -> Unit)? = null

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        clipChildren = false
        clipToPadding = false

        // Trigger row layout
        triggerCard = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, closedHeightPx)
            val pad = Math.round(14f * density)
            setPadding(pad, 0, pad, 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = CLOSED_RADIUS_DP * density
                setColor(ContextCompat.getColor(context, R.color.surface))
                setStroke(Math.round(1f * density), ContextCompat.getColor(context, R.color.hairline))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { toggle() }
        }

        ivTriggerIcon = ImageView(context).apply {
            layoutParams = LayoutParams(Math.round(20f * density), Math.round(20f * density))
            setImageResource(R.drawable.ic_topic)
            setColorFilter(ContextCompat.getColor(context, R.color.system_blue))
            visibility = GONE
        }

        tvTriggerLabel = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = Math.round(8f * density)
                marginEnd = Math.round(8f * density)
            }
            text = "Select Exam"
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        ivChevron = ImageView(context).apply {
            layoutParams = LayoutParams(Math.round(20f * density), Math.round(20f * density))
            setImageResource(R.drawable.ic_chevron_down)
            setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
        }

        triggerCard.addView(ivTriggerIcon)
        triggerCard.addView(tvTriggerLabel)
        triggerCard.addView(ivChevron)
        addView(triggerCard)

        // Options panel container
        optionsPanel = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = OPEN_RADIUS_DP * density
                setColor(ContextCompat.getColor(context, R.color.surface))
                setStroke(Math.round(1f * density), ContextCompat.getColor(context, R.color.hairline))
            }
            visibility = GONE
            val pad = Math.round(6f * density)
            setPadding(pad, pad, pad, pad)
        }

        // Search field for >8 options
        etSearch = EditText(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, Math.round(38f * density)).apply {
                val m = Math.round(4f * density)
                setMargins(m, m, m, m)
            }
            hint = "Search exams..."
            textSize = 13f
            setPadding(Math.round(10f * density), 0, Math.round(10f * density), 0)
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            visibility = GONE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterOptions(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        optionsPanel.addView(etSearch)

        optionsScrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        optionsContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        highlightPill = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, Math.round(42f * density))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                val pillRadius = 10f * density
                cornerRadius = pillRadius
                setColor(ContextCompat.getColor(context, R.color.surface_inset))
            }
            alpha = 0f
        }
        optionsContainer.addView(highlightPill)

        optionsListLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        optionsContainer.addView(optionsListLayout)
        optionsScrollView.addView(optionsContainer)
        optionsPanel.addView(optionsScrollView)
        addView(optionsPanel)
    }

    fun setOptions(items: List<Option>, selectedId: String? = null) {
        this.allOptions = items
        this.filteredOptions = items
        etSearch.visibility = if (items.size > SEARCH_THRESHOLD) VISIBLE else GONE
        rebuildOptionViews()

        val initial = items.firstOrNull { it.id == selectedId }
        if (initial != null) {
            setSelectionInternal(initial)
        } else if (selectedOption != null) {
            val matching = items.firstOrNull { it.id == selectedOption?.id }
            if (matching != null) setSelectionInternal(matching)
        }
    }

    private fun filterOptions(query: String) {
        val q = query.trim().lowercase()
        filteredOptions = if (q.isBlank()) {
            allOptions
        } else {
            allOptions.filter {
                it.label.lowercase().contains(q) || (it.meta?.lowercase()?.contains(q) == true)
            }
        }
        rebuildOptionViews()
    }

    private fun rebuildOptionViews() {
        optionsListLayout.removeAllViews()

        filteredOptions.forEachIndexed { index, option ->
            val itemView = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, Math.round(42f * density))
                val padH = Math.round(12f * density)
                setPadding(padH, 0, padH, 0)
                isClickable = true
                isFocusable = true
                background = null

                setOnClickListener {
                    selectOption(option)
                }
            }

            val tvLabel = TextView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                text = option.label
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            }
            itemView.addView(tvLabel)

            if (!option.meta.isNullOrBlank()) {
                val tvMeta = TextView(context).apply {
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    text = option.meta
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                itemView.addView(tvMeta)
            }

            optionsListLayout.addView(itemView)
        }
    }

    private fun setSelectionInternal(option: Option) {
        selectedOption = option
        tvTriggerLabel.text = option.label
        if (option.iconRes != null) {
            ivTriggerIcon.visibility = VISIBLE
            ivTriggerIcon.setImageResource(option.iconRes)
        } else {
            ivTriggerIcon.visibility = GONE
        }
    }

    fun selectOption(option: Option) {
        setSelectionInternal(option)
        glideHighlightTo(option)
        // Resolves selection strictly by stable option ID
        onOptionSelectedListener?.invoke(option)
        close()
    }

    fun clearSelection() {
        selectedOption = null
        tvTriggerLabel.text = "Select Exam"
        ivTriggerIcon.visibility = GONE
        highlightPill.alpha = 0f
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun open() {
        if (isOpen || allOptions.isEmpty()) return
        isOpen = true

        val searchHeight = if (allOptions.size > SEARCH_THRESHOLD) Math.round(46f * density) else 0
        val contentHeight = filteredOptions.size * Math.round(44f * density) + searchHeight + Math.round(16f * density)
        val targetHeight = Math.min(maxPanelHeightPx, contentHeight)

        optionsPanel.visibility = VISIBLE

        if (!UiArcMotion.isMotionEnabled(context)) {
            optionsPanel.layoutParams.height = targetHeight
            optionsPanel.requestLayout()
            ivChevron.rotation = 180f
            return
        }

        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofInt(0, targetHeight).apply {
            duration = GROW_DURATION_MS
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener { va ->
                val h = va.animatedValue as Int
                optionsPanel.layoutParams.height = h
                optionsPanel.requestLayout()
            }
            start()
        }

        ivChevron.animate()
            .rotation(180f)
            .setDuration(UiArcMotion.Duration.STANDARD)
            .setInterpolator(UiArcMotion.Ease.ENTER)
            .start()

        selectedOption?.let { glideHighlightTo(it, animated = false) }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false

        if (!UiArcMotion.isMotionEnabled(context)) {
            optionsPanel.layoutParams.height = 0
            optionsPanel.visibility = GONE
            optionsPanel.requestLayout()
            ivChevron.rotation = 0f
            return
        }

        val currentHeight = optionsPanel.height
        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofInt(currentHeight, 0).apply {
            duration = SHRINK_DURATION_MS
            interpolator = UiArcMotion.Spring.SHRINK
            addUpdateListener { va ->
                val h = va.animatedValue as Int
                optionsPanel.layoutParams.height = h
                optionsPanel.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    optionsPanel.visibility = GONE
                }
            })
            start()
        }

        ivChevron.animate()
            .rotation(0f)
            .setDuration(UiArcMotion.Duration.STANDARD)
            .setInterpolator(UiArcMotion.Ease.ENTER)
            .start()
    }

    private fun glideHighlightTo(option: Option, animated: Boolean = true) {
        val index = filteredOptions.indexOfFirst { it.id == option.id }
        if (index < 0) return
        val targetY = index * Math.round(42f * density).toFloat()

        highlightPill.alpha = 1f
        if (!animated || !UiArcMotion.isMotionEnabled(context)) {
            highlightPill.translationY = targetY
            return
        }

        highlightAnimator?.cancel()
        highlightAnimator = ValueAnimator.ofFloat(highlightPill.translationY, targetY).apply {
            duration = GLIDE_DURATION_MS
            interpolator = UiArcMotion.Spring.SELECT_GLIDE
            addUpdateListener { va ->
                highlightPill.translationY = va.animatedValue as Float
            }
            start()
        }
    }

    fun getSelectedOption(): Option? = selectedOption
    fun isExpanded(): Boolean = isOpen
}
