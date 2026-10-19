package com.eve.app.designsystem

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.eve.app.R

/** Theme-resolved paints for custom Views. Never cache across a theme recreation. */
object DesignTokens {
    fun color(context: Context, @AttrRes attribute: Int): Int {
        val value = TypedValue()
        check(context.theme.resolveAttribute(attribute, value, true)) {
            "Missing design color attribute: $attribute"
        }
        return if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId) else value.data
    }

    fun primary(context: Context): Int = color(context, R.attr.textPrimary)
    fun secondary(context: Context): Int = color(context, R.attr.textSecondary)
    fun accent(context: Context): Int = color(context, R.attr.accent)
    fun track(context: Context): Int = color(context, R.attr.track)
}
