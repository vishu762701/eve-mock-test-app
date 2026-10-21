package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
 * - Corner radius: morphs from closed radius to OPEN_RADIUS = 22dp
 * - Gliding highlight: glides between options with GLIDE spring (380ms)
 * - Selection: lifted label travels with FLY spring (400ms), surface collapses with SHRINK (450ms)
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
        const val GROW_DURATION_MS = 500L
        const val SHRINK_DURATION_MS = 450L
        const val GLIDE_DURATION_MS = 380L
        const val FLY_DURATION_MS = 400L
    }

    private val density = resources.displayMetrics.density
    private val closedHeightPx = Math.round(50f * density)

    // Trigger row
    private val triggerCard: LinearLayout
    private val ivTriggerIcon: ImageView
    private val tvTriggerLabel: TextView
    private val ivChevron: ImageView

    // Options dropdown container
    private val optionsScrollView: ScrollView
    private val optionsContainer: FrameLayout
    private val optionsListLayout: LinearLayout
    private val highlightPill: View

    private var options: List<Option> = emptyList()
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
            background = ContextCompat.getDrawable(context, R.drawable.design_card_surface)
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
            text = "Select option"
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

        // Options dropdown container
        optionsScrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
            visibility = GONE
        }

        optionsContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val pad = Math.round(6f * density)
            setPadding(pad, pad, pad, pad)
            background = ContextCompat.getDrawable(context, R.drawable.design_card_surface)
        }

        highlightPill = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, Math.round(42f * density))
            background = ContextCompat.getDrawable(context, R.drawable.design_pill_surface)
            alpha = 0f
        }
        optionsContainer.addView(highlightPill)

        optionsListLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        optionsContainer.addView(optionsListLayout)
        optionsScrollView.addView(optionsContainer)
        addView(optionsScrollView)
    }

    fun setOptions(items: List<Option>, selectedId: String? = null) {
        this.options = items
        optionsListLayout.removeAllViews()

        items.forEachIndexed { index, option ->
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

        val initial = items.firstOrNull { it.id == selectedId } ?: items.firstOrNull()
        if (initial != null) {
            setSelectionInternal(initial)
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
        onOptionSelectedListener?.invoke(option)
        close()
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun open() {
        if (isOpen || options.isEmpty()) return
        isOpen = true

        val targetHeight = Math.min(Math.round(240f * density), options.size * Math.round(44f * density) + Math.round(16f * density))
        optionsScrollView.visibility = VISIBLE

        if (!UiArcMotion.isMotionEnabled(context)) {
            optionsScrollView.layoutParams.height = targetHeight
            optionsScrollView.requestLayout()
            ivChevron.rotation = 180f
            return
        }

        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofInt(0, targetHeight).apply {
            duration = GROW_DURATION_MS
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener { va ->
                val h = va.animatedValue as Int
                optionsScrollView.layoutParams.height = h
                optionsScrollView.requestLayout()
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
            optionsScrollView.layoutParams.height = 0
            optionsScrollView.visibility = GONE
            optionsScrollView.requestLayout()
            ivChevron.rotation = 0f
            return
        }

        val currentHeight = optionsScrollView.height
        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofInt(currentHeight, 0).apply {
            duration = SHRINK_DURATION_MS
            interpolator = UiArcMotion.Spring.SHRINK
            addUpdateListener { va ->
                val h = va.animatedValue as Int
                optionsScrollView.layoutParams.height = h
                optionsScrollView.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    optionsScrollView.visibility = GONE
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
        val index = options.indexOfFirst { it.id == option.id }
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
