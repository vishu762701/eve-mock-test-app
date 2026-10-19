package com.eve.app.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.eve.app.R

/** The owner's single bundled Poppins family. Missing fonts are never substituted. */
object FontManager {
    fun typeface(context: Context, bold: Boolean): Typeface = requireNotNull(
        ResourcesCompat.getFont(context, if (bold) R.font.poppins_bold else R.font.poppins_regular)
    )

    fun mediumTypeface(context: Context): Typeface = requireNotNull(
        ResourcesCompat.getFont(context, R.font.poppins_medium)
    )
}
