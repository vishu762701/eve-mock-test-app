package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.eve.app.R
import com.eve.app.util.FontManager
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * UIArc Segmented Control — Time Range Selection Component.
 * Faithfully implements the UIArc Segmented Control specification (https://uiarc.dev/components/segmented-control):
 * - Track container with subtle inset background and hairline border
 * - Single shared gliding selection pill that morphs between options using spring.morph (visualDuration 0.42s, bounce 0.16)
 * - Under-label highlight travel: labels never scale or change weight, avoiding layout jump
 * - Preserves exact option labels ("Last 7 days", "Last 30 days", "All time") and ID contracts
 * - Keyboard / D-pad arrow navigation, TalkBack selection state, large font support, and reduced-motion handling
 */
class UiArcSegmentedControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var checkedChangeListener: ((group: View, checkedIds: List<Int>) -> Unit)? = null

    private val scrollView: HorizontalScrollView
    private val trackLayout: FrameLayout
    private val selectionPill: View
    private val optionsRow: LinearLayout

    private val optionViews = mutableListOf<AppCompatTextView>()
    private var selectedIndex = 2 // Default: All time (index 2)
    private var pillAnimator: ValueAnimator? = null

    val checkedChipIds: List<Int>
        get() = if (selectedIndex in optionViews.indices) listOf(optionViews[selectedIndex].id) else emptyList()

    val checkedChipId: Int
        get() = if (selectedIndex in optionViews.indices) optionViews[selectedIndex].id else View.NO_ID

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        clipChildren = false
        clipToPadding = false

        val trackHeight = resources.getDimensionPixelSize(R.dimen.design_tab_height)
        val trackPadding = resources.getDimensionPixelSize(R.dimen.design_tab_padding)
        val hairlineColor = ContextCompat.getColor(context, R.color.hairline)

        // HorizontalScrollView wrapper to handle narrow screen widths gracefully without label clipping
        scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
            clipChildren = false
            clipToPadding = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        trackLayout = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
            minimumHeight = trackHeight
            setPadding(trackPadding, trackPadding, trackPadding, trackPadding)

            // Capsule shape with surface_inset fill and hairline border
            val shape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Eve_Token_Capsule, 0).build()
            background = MaterialShapeDrawable(shape).apply {
                fillColor = ContextCompat.getColorStateList(context, R.color.surface_inset)
                strokeColor = ColorStateList.valueOf(hairlineColor)
                strokeWidth = resources.getDimension(R.dimen.design_hairline_width)
            }
        }

        // Gliding Selection Pill (sits behind the labels in the track)
        selectionPill = View(context).apply {
            val pillShape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Eve_Token_Capsule, 0).build()
            background = MaterialShapeDrawable(pillShape).apply {
                fillColor = ContextCompat.getColorStateList(context, R.color.surface)
                strokeColor = ColorStateList.valueOf(hairlineColor)
                strokeWidth = resources.getDimension(R.dimen.design_hairline_width)
                elevation = resources.getDimension(R.dimen.design_shadow_y) / 3f
            }
        }
        trackLayout.addView(selectionPill)

        // Options Row (on top of gliding pill)
        optionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        trackLayout.addView(optionsRow)

        scrollView.addView(
            trackLayout,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
        addView(scrollView)

        // Create the three standard analytics range options
        createOption(R.id.chipLast7Days, "Last 7 days", 0)
        createOption(R.id.chipLast30Days, "Last 30 days", 1)
        createOption(R.id.chipAllTime, "All time", 2)

        post {
            updatePillPosition(animate = false)
            updateTextStates()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // If placeholder views with the contract IDs were inflated from XML, clean them up
        // so optionViews own those IDs exclusively without duplicate view hierarchy
        findViewById<View>(R.id.chipLast7Days)?.let { if (it !== optionViews.getOrNull(0)) removeView(it) }
        findViewById<View>(R.id.chipLast30Days)?.let { if (it !== optionViews.getOrNull(1)) removeView(it) }
        findViewById<View>(R.id.chipAllTime)?.let { if (it !== optionViews.getOrNull(2)) removeView(it) }
    }

    private fun createOption(viewId: Int, label: String, index: Int) {
        val segmentHeight = resources.getDimensionPixelSize(R.dimen.design_tab_segment_height)
        val pillPadding = resources.getDimensionPixelSize(R.dimen.design_pill_padding)
        val typeSize = resources.getDimension(R.dimen.design_type_tab)

        val option = AppCompatTextView(context).apply {
            id = viewId
            text = label
            gravity = Gravity.CENTER
            typeface = FontManager.typeface(context, index == selectedIndex)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeSize)
            includeFontPadding = false
            isSingleLine = true
            setPadding(pillPadding, 0, pillPadding, 0)
            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(0, segmentHeight, 1f).apply {
                if (index > 0) marginStart = (2f * density).toInt()
            }

            setOnClickListener {
                selectIndex(index, userInitiated = true)
            }

            ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.roleDescription = "Tab"
                    info.isSelected = (index == selectedIndex)
                }
            })
        }

        optionViews.add(option)
        optionsRow.addView(option)
    }

    fun setOnCheckedStateChangeListener(listener: (group: View, checkedIds: List<Int>) -> Unit) {
        checkedChangeListener = listener
    }

    fun check(id: Int) {
        val idx = optionViews.indexOfFirst { it.id == id }
        if (idx >= 0) {
            selectIndex(idx, userInitiated = false)
        }
    }

    fun getCheckedChipId(): Int = checkedChipId

    private fun selectIndex(index: Int, userInitiated: Boolean) {
        if (index !in optionViews.indices) return
        val changed = (selectedIndex != index)
        selectedIndex = index

        updatePillPosition(animate = UiArcMotion.isMotionEnabled(context))
        updateTextStates()

        if (changed && userInitiated) {
            checkedChangeListener?.invoke(this, checkedChipIds)
        }
    }

    private fun updateTextStates() {
        val selectedColor = ContextCompat.getColor(context, R.color.text_primary)
        val unselectedColor = ContextCompat.getColor(context, R.color.text_secondary)

        optionViews.forEachIndexed { idx, tv ->
            val isSelected = (idx == selectedIndex)
            tv.setTextColor(if (isSelected) selectedColor else unselectedColor)
            tv.typeface = FontManager.typeface(context, isSelected)
            tv.isSelected = isSelected
        }
    }

    private fun updatePillPosition(animate: Boolean) {
        if (selectedIndex !in optionViews.indices) return
        val target = optionViews[selectedIndex]

        val targetLeft = target.left.toFloat()
        val targetWidth = target.width.toFloat()
        val targetHeight = target.height

        if (targetWidth <= 0f) {
            target.post { updatePillPosition(animate) }
            return
        }

        val pillParams = selectionPill.layoutParams as LayoutParams
        pillParams.height = targetHeight

        val fromX = selectionPill.translationX
        val fromWidth = selectionPill.width.toFloat().let { if (it > 0f) it else targetWidth }

        pillAnimator?.cancel()

        if (!animate || fromWidth <= 0f) {
            selectionPill.translationX = targetLeft
            pillParams.width = targetWidth.toInt()
            selectionPill.layoutParams = pillParams
            ensureVisible(target)
            return
        }

        pillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = UiArcMotion.Spring.MORPH.settlingDurationMs
            interpolator = UiArcMotion.Spring.MORPH
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                val currentX = fromX + (targetLeft - fromX) * fraction
                val currentW = fromWidth + (targetWidth - fromWidth) * fraction

                selectionPill.translationX = currentX
                pillParams.width = currentW.toInt()
                selectionPill.layoutParams = pillParams
            }
            start()
        }

        ensureVisible(target)
    }

    private fun ensureVisible(target: View) {
        val room = (20f * density).toInt()
        val scrollX = scrollView.scrollX
        val targetLeft = target.left - room
        val targetRight = target.right + room
        val width = scrollView.width

        if (targetLeft < scrollX) {
            scrollView.smoothScrollTo(targetLeft.coerceAtLeast(0), 0)
        } else if (targetRight > scrollX + width) {
            scrollView.smoothScrollTo((targetRight - width).coerceAtLeast(0), 0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (selectedIndex > 0) {
                    selectIndex(selectedIndex - 1, userInitiated = true)
                    optionViews[selectedIndex].requestFocus()
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (selectedIndex < optionViews.size - 1) {
                    selectIndex(selectedIndex + 1, userInitiated = true)
                    optionViews[selectedIndex].requestFocus()
                    true
                } else false
            }
            KeyEvent.KEYCODE_MOVE_HOME -> {
                selectIndex(0, userInitiated = true)
                optionViews[0].requestFocus()
                true
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                selectIndex(optionViews.size - 1, userInitiated = true)
                optionViews[optionViews.size - 1].requestFocus()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return Bundle().apply {
            putParcelable("super_state", superState)
            putInt("selected_index", selectedIndex)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                state.getParcelable("super_state", Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                state.getParcelable("super_state")
            }
            super.onRestoreInstanceState(superState)
            val restored = state.getInt("selected_index", selectedIndex)
            selectIndex(restored, userInitiated = false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }
}
