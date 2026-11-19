package com.eve.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.eve.app.ui.common.TelegramHostActivity
import org.telegram.messenger.LocaleController
import org.telegram.ui.LanguageSelectActivity as TelegramLanguageSelectActivity

/**
 * Host Activity for Telegram LanguageSelectActivity port (C4).
 * Extends TelegramHostActivity (INavigationLayout) and supplies Eve's supported locales via TelegramLanguageDelegate.
 */
class LanguageSelectActivity : TelegramHostActivity<TelegramLanguageSelectActivity>(), TelegramLanguageDelegate {

    override fun createTelegramFragment(savedInstanceState: Bundle?): TelegramLanguageSelectActivity {
        return TelegramLanguageSelectActivity().apply {
            setEveDelegate(this@LanguageSelectActivity)
        }
    }

    override fun getLanguages(): ArrayList<LocaleController.LocaleInfo> {
        fun make(tag: String, nativeName: String, englishName: String, idx: Int) =
            LocaleController.LocaleInfo().apply {
                shortName = tag
                pluralLangCode = if (tag.isEmpty()) "en" else tag
                name = nativeName
                nameEnglish = englishName
                serverIndex = idx
            }
        return arrayListOf(
            make("", "System default", "Follow device language", 0),
            make("en", "English", "English", 1),
            make("hi", "हिन्दी", "Hindi", 2)
        )
    }

    override fun getCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "" else (locales.get(0)?.language ?: "")
    }

    override fun onLanguageSelected(localeInfo: LocaleController.LocaleInfo) {
        val tag = localeInfo.shortName ?: ""
        if (tag == getCurrentLanguageTag()) return
        val newLocales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(newLocales)
    }
}
