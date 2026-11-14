package com.eve.app.ui.admin.tg

import android.content.Context
import android.util.AttributeSet

/**
 * Compatibility subclass redirecting to canonical [com.eve.app.designsystem.TextCheckCell].
 */
open class TextCheckCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : com.eve.app.designsystem.TextCheckCell(context, attrs, defStyleAttr)
