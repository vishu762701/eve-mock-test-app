package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.eve.app.R

object FontManager {

    private const val PREFS = "eve_prefs"
    const val KEY_FONT_CHOICE = "key_font_choice"

    const val FONT_EVE_DEFAULT = "EVE_DEFAULT"
    const val FONT_DEVICE = "DEVICE"

    var fontVersion: Int = 0
        private set

    fun getFontChoice(prefs: SharedPreferences): String {
        return prefs.getString(KEY_FONT_CHOICE, FONT_EVE_DEFAULT) ?: FONT_EVE_DEFAULT
    }

    fun setFontChoice(prefs: SharedPreferences, choice: String) {
        val current = prefs.getString(KEY_FONT_CHOICE, FONT_EVE_DEFAULT) ?: FONT_EVE_DEFAULT
        if (current != choice) {
            prefs.edit().putString(KEY_FONT_CHOICE, choice).apply()
            fontVersion++
        }
    }

    fun getFontChoice(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return getFontChoice(prefs)
    }

    fun setFontChoice(context: Context, choice: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setFontChoice(prefs, choice)
    }

    fun resetForTesting() {
        fontVersion = 0
    }

    fun apply(activity: Activity) {
        val choice = getFontChoice(activity)
        if (choice == FONT_DEVICE) {
            activity.theme.applyStyle(R.style.ThemeOverlay_Eve_DeviceFont, true)
        }
    }

    fun registerLifecycleCallbacks(app: android.app.Application) {
        app.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
                apply(activity)
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun typeface(context: Context, bold: Boolean): Typeface {
        val choice = getFontChoice(context)
        return if (choice == FONT_DEVICE) {
            if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        } else {
            val fontRes = if (bold) R.font.source_serif_4_bold else R.font.source_serif_4_regular
            try {
                ResourcesCompat.getFont(context, fontRes)
                    ?: if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            } catch (_: Throwable) {
                if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }
    }
}
