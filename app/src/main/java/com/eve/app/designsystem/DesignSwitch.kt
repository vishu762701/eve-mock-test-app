package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet

/**
 * Backward compatibility wrapper delegating to authentic TelegramSwitch.
 */
open class DesignSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TelegramSwitch(context, attrs, defStyleAttr)
