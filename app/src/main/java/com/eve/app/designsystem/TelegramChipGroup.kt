/*
 * Derived from Telegram Android FiltersView.java, FilterTabsView.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import com.eve.app.R
import kotlin.math.max

/**
 * Authentic Telegram-style chip and pill group replacing Material ChipGroup.
 * Custom ViewGroup implementing genuine multi-row flow and single-line layout
 * without any AndroidX/Material ChipGroup inheritance.
 */
open class TelegramChipGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    var chipSpacingHorizontal: Int = dp(8f)
        set(value) {
            field = value
            requestLayout()
        }

    var chipSpacingVertical: Int = dp(8f)
        set(value) {
            field = value
            requestLayout()
        }

    var isSingleLine: Boolean = false
        set(value) {
            field = value
            requestLayout()
        }

    var isSingleSelection: Boolean = true
    var isSelectionRequired: Boolean = false

    private var checkedId: Int = View.NO_ID

    val checkedChipId: Int
        get() = checkedId

    val checkedChipIds: List<Int>
        get() {
            if (isSingleSelection) {
                return if (checkedId != View.NO_ID) listOf(checkedId) else emptyList()
            }
            val result = mutableListOf<Int>()
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is Checkable && child.isChecked) {
                    result.add(child.id)
                }
            }
            return result
        }

    fun interface OnCheckedStateChangeListener {
        fun onCheckedChanged(group: TelegramChipGroup, checkedIds: List<Int>)
    }

    private var onCheckedStateChangeListener: OnCheckedStateChangeListener? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TelegramChipGroup, defStyleAttr, 0)
            chipSpacingHorizontal = a.getDimensionPixelSize(R.styleable.TelegramChipGroup_chipSpacingHorizontal, chipSpacingHorizontal)
            chipSpacingVertical = a.getDimensionPixelSize(R.styleable.TelegramChipGroup_chipSpacingVertical, chipSpacingVertical)
            isSingleLine = a.getBoolean(R.styleable.TelegramChipGroup_singleLine, false)
            isSingleSelection = a.getBoolean(R.styleable.TelegramChipGroup_singleSelection, true)
            a.recycle()
        }
    }

    fun setOnCheckedStateChangeListener(listener: OnCheckedStateChangeListener?) {
        this.onCheckedStateChangeListener = listener
    }

    fun check(id: Int) {
        if (id == checkedId) return
        if (isSingleSelection && checkedId != View.NO_ID) {
            setCheckedStateForView(checkedId, false)
        }
        checkedId = id
        if (id != View.NO_ID) {
            setCheckedStateForView(id, true)
        }
        notifyCheckChanged()
    }

    fun clearCheck() {
        if (isSingleSelection && checkedId != View.NO_ID) {
            setCheckedStateForView(checkedId, false)
        }
        checkedId = View.NO_ID
        notifyCheckChanged()
    }

    private fun notifyCheckChanged() {
        onCheckedStateChangeListener?.onCheckedChanged(this, checkedChipIds)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (child.id == View.NO_ID) {
            child.id = View.generateViewId()
        }
        if (child is android.widget.CompoundButton) {
            child.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isSingleSelection) {
                    if (isChecked) {
                        check(buttonView.id)
                    } else if (isSelectionRequired && checkedId == buttonView.id) {
                        buttonView.isChecked = true
                    }
                } else {
                    notifyCheckChanged()
                }
            }
        } else {
            child.setOnClickListener {
                if (isSingleSelection) {
                    check(child.id)
                } else if (child is Checkable) {
                    child.toggle()
                    notifyCheckChanged()
                }
            }
        }
        if (child is Checkable && child.isChecked) {
            if (isSingleSelection) {
                check(child.id)
            }
        }
    }

    private fun setCheckedStateForView(viewId: Int, isChecked: Boolean) {
        val target = findViewById<View>(viewId)
        if (target is Checkable) {
            target.isChecked = isChecked
        }
    }

    fun addChip(
        chipText: String,
        chipId: Int = View.generateViewId(),
        checked: Boolean = false
    ): TelegramChip {
        val chip = TelegramChip(context).apply {
            id = chipId
            text = chipText
            isChecked = checked
        }
        addView(chip)
        if (checked) {
            check(chipId)
        }
        return chip
    }

    fun selectChipByText(text: String) {
        for (i in 0 until childCount) {
            val chip = getChildAt(i) as? android.widget.TextView ?: continue
            if (chip.text.toString().equals(text, ignoreCase = true)) {
                check(chip.id)
                break
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var width = 0
        var height = 0
        var currentLineWidth = 0
        var currentLineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (!isSingleLine && currentLineWidth + childWidth > widthSize && currentLineWidth > 0) {
                width = max(width, currentLineWidth - chipSpacingHorizontal)
                height += currentLineHeight + chipSpacingVertical
                currentLineWidth = childWidth + chipSpacingHorizontal
                currentLineHeight = childHeight
            } else {
                currentLineWidth += childWidth + chipSpacingHorizontal
                currentLineHeight = max(currentLineHeight, childHeight)
            }
        }

        width = max(width, currentLineWidth - chipSpacingHorizontal)
        height += currentLineHeight

        setMeasuredDimension(
            resolveSize(width + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(height + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val widthSize = r - l - paddingRight
        var currentLeft = paddingLeft
        var currentTop = paddingTop
        var currentLineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (!isSingleLine && currentLeft + childWidth > widthSize && currentLeft > paddingLeft) {
                currentLeft = paddingLeft
                currentTop += currentLineHeight + chipSpacingVertical
                currentLineHeight = childHeight
            }

            child.layout(currentLeft, currentTop, currentLeft + childWidth, currentTop + childHeight)
            currentLeft += childWidth + chipSpacingHorizontal
            currentLineHeight = max(currentLineHeight, childHeight)
        }
    }
}
