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

    /** Keep semantic fill/icon colors independent from accessible normal-text shades. */
    fun semanticText(context: Context, color: Int): Int {
        for ((base, text) in listOf(
            R.color.system_green to R.color.system_green_text,
            R.color.system_red to R.color.system_red_text,
            R.color.system_orange to R.color.system_orange_text,
            R.color.system_blue to R.color.system_blue_text,
            R.color.system_yellow to R.color.system_yellow_text,
            R.color.system_purple to R.color.system_purple_text,
            R.color.system_indigo to R.color.system_indigo_text)) {
            if (color == ContextCompat.getColor(context, base)) return ContextCompat.getColor(context, text)
        }
        return color
    }

    fun primary(context: Context): Int = color(context, R.attr.designTextPrimary)
    fun secondary(context: Context): Int = color(context, R.attr.designTextSecondary)
    fun accent(context: Context): Int = color(context, R.attr.designAccent)
    fun track(context: Context): Int = color(context, R.attr.designTrack)
}
