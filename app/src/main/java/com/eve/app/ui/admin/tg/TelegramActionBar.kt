package com.eve.app.ui.admin.tg

import android.content.Context
import android.util.AttributeSet

/**
 * Compatibility subclass redirecting to canonical [com.eve.app.designsystem.TelegramActionBar].
 */
open class TelegramActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : com.eve.app.designsystem.TelegramActionBar(context, attrs, defStyleAttr)
