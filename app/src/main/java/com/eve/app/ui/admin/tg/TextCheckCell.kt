/*
 * Derived from Telegram Android TextCheckCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.util.AttributeSet

/**
 * Backward compatibility subclass for TextCheckCell in the admin package.
 * Canonical implementation lives in [com.eve.app.designsystem.TextCheckCell].
 */
open class TextCheckCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : com.eve.app.designsystem.TextCheckCell(context, attrs, defStyleAttr)
