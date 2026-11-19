package com.eve.app.ui.profile

/**
 * CUT-INTERFACE bridge between ported [org.telegram.ui.ChangeUsernameActivity]
 * and EVE's Worker API (`checkUsername`, `updateProfile`, `getProfile`).
 */
interface TelegramUsernameDelegate {
    interface CheckCallback {
        fun onResult(available: Boolean, errorMessage: String?)
    }

    fun getCurrentUsername(): String
    fun getLinkPrefix(): String
    fun getMinimumLength(): Int
    fun checkUsername(name: String, callback: CheckCallback)
    fun cancelCheck()
    fun saveUsername(name: String)
    fun onLinkCopied(url: String)
    fun onErrorVibration()
    fun onStatusRoleChanged(role: Int)

    companion object {
        const val ROLE_NONE = 0
        const val ROLE_AVAILABLE = 1
        const val ROLE_INVALID = 2
        const val ROLE_CHECKING = 3
    }
}
