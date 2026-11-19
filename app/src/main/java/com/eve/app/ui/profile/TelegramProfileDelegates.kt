package com.eve.app.ui.profile

/**
 * EVE-owned CUT-INTERFACE delegates replacing Telegram account/TLRPC calls
 * in ported profile editor fragments (ChangeNameActivity, ChangeBioActivity).
 */
object TelegramProfileDelegates {
    interface ChangeNameDelegate {
        fun getInitialFirstName(): String
        fun getInitialLastName(): String
        fun saveName(firstName: String, lastName: String)
    }

    interface ChangeBioDelegate {
        fun getInitialBio(): String
        fun getBioMaxLength(): Int
        fun saveBio(newBio: String)
    }
}
