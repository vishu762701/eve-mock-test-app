package com.eve.app.ui.settings

import org.telegram.messenger.LocaleController

/**
 * EVE-owned CUT-INTERFACE supplying Eve's supported app locales (System default, English, Hindi)
 * and applying selection via AppCompatDelegate without Telegram's remote langpack/translate controllers.
 */
interface TelegramLanguageDelegate {
    fun getLanguages(): ArrayList<LocaleController.LocaleInfo>
    fun getCurrentLanguageTag(): String
    fun onLanguageSelected(localeInfo: LocaleController.LocaleInfo)
}
