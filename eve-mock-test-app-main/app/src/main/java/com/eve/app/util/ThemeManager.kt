package com.eve.app.util

import android.content.Context
import android.content.res.Configuration
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDelegate
import com.eve.app.R

/**
 * Ek single toggle: Light <-> Dark. Sun icon = "abhi light hai, dark karne ke liye dabao",
 * Moon icon = "abhi dark hai, light karne ke liye dabao". Choice SharedPreferences me save
 * hoti hai isliye app dobara khulne par bhi wahi mode yaad rehta hai. Pehli baar (koi saved
 * choice nahi) system ka dark/light setting follow hoti hai.
 */
object ThemeManager {

    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    /** App start hote hi (Application.onCreate me) call karo, kisi Activity dikhne se pehle. */
    fun applySavedMode(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = if (prefs.contains(KEY_DARK_MODE)) {
            if (prefs.getBoolean(KEY_DARK_MODE, false)) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isDarkMode(context: Context): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    /**
     * Mode flip karta hai aur save karta hai. AppCompatDelegate.setDefaultNightMode()
     * chalte hi saari running AppCompatActivity apne aap recreate ho jaati hain, isliye
     * icon/colors sab jagah turant update ho jaate hain — manual recreate() ki zaroorat nahi.
     */
    fun toggle(context: Context) {
        val goingDark = !isDarkMode(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, goingDark)
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (goingDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Ek chhota icon-only toggle button ko current mode ke hisaab se sun/moon icon set
     * karta hai aur click par mode switch karta hai. Har screen isi ek function ko reuse
     * karti hai taaki icon/behaviour hamesha same rahe (dark ho to moon dikhega, tap karte
     * hi light ho jayega aur sun dikhega — dono icon hamesha ek hi jagah/button me hote hain).
     */
    fun setupToggleButton(context: Context, button: ImageButton) {
        button.setImageResource(if (isDarkMode(context)) R.drawable.ic_moon else R.drawable.ic_sun)
        button.contentDescription = context.getString(
            if (isDarkMode(context)) R.string.theme_toggle_to_light else R.string.theme_toggle_to_dark
        )
        button.setOnClickListener { toggle(context) }
    }
}
