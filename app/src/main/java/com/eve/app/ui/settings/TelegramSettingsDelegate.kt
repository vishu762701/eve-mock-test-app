package com.eve.app.ui.settings

import android.graphics.drawable.Drawable

/**
 * EVE-owned CUT-INTERFACE supplying user profile state, click navigation,
 * and dialog actions to org.telegram.ui.SettingsActivity without Telegram's MTProto/TLRPC stack.
 */
interface TelegramSettingsDelegate {
    fun getUserDisplayName(): String
    fun getUserSubtitle(): String
    fun getUserUidHash(): Long = 1L
    fun getUserPhoto(): Drawable? = null
    fun getVersionString(): String = "EVE Android"
    fun onRowClicked(id: Int)
    fun onQrClicked()
    fun onSearchClicked()
    fun onLogOutClicked()
    fun onDeleteAccountClicked()
    fun onAvatarClicked()
}
