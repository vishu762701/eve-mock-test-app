package com.eve.app.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.eve.app.R

/**
 * Authoritative font manager providing the official iOS 26 typography font family (Inter)
 * with authentic weights: Regular (400), Medium (500), Semibold (600), and Bold (700).
 * System typeface fallbacks ensure full multilingual (Hindi/Devanagari) script coverage.
 */
object FontManager {
    fun typeface(context: Context, bold: Boolean): Typeface {
        val resId = if (bold) R.font.inter_bold else R.font.inter_regular
        return try {
            ResourcesCompat.getFont(context, resId) ?: if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        } catch (_: Exception) {
            if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    fun mediumTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.inter_medium) ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    fun semiboldTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.inter_semibold) ?: Typeface.create("sans-serif-medium", Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
    }

    fun regularTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    fun boldTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }
}
