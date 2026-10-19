package com.eve.app.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

/** The same shared actions used by Practice/PYQ and native visual verification. */
object StudentSelectionControls {
    fun action(context: Context): MaterialButton =
        (LayoutInflater.from(context).inflate(R.layout.item_student_action, null) as MaterialButton).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    fun year(context: Context): Chip = Chip(context).apply {
        isCheckable = true
        isCheckedIconVisible = false
        setEnsureMinTouchTargetSize(true)
        chipMinHeight = resources.getDimension(R.dimen.design_pill_height)
        chipCornerRadius = chipMinHeight / 2
        chipStrokeWidth = 0f
        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.accent)
        setTextColor(ContextCompat.getColorStateList(context, R.color.on_accent))
        com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.CHIP)
        chipStartPadding = resources.getDimension(R.dimen.design_pill_padding)
        chipEndPadding = chipStartPadding
    }
}
