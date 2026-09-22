package com.eve.app.util

import android.content.Context
import android.widget.TextView

/**
 * Test aur Result/Review screens par ek chhota EN/हिं toggle. ThemeManager ke ulat, yeh
 * Activity recreate nahi karta (question text ViewPager/RecyclerView ke beech me hai,
 * recreate se progress/timer reset ho jayega) — bas SharedPreferences me choice save karta
 * hai aur caller ko batata hai taaki wo apna adapter refresh kar le.
 */
object LanguageManager {

    private const val PREFS = "eve_prefs"
    private const val KEY_HINDI = "key_hindi_questions"

    fun isHindi(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_HINDI, false)

    fun toggle(context: Context): Boolean {
        val newValue = !isHindi(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_HINDI, newValue)
            .apply()
        return newValue
    }

    /**
     * Button ka text current language ke hisaab se set karta hai ("हिं" = "Hindi me switch
     * karne ke liye dabao", "EN" = "English me switch karne ke liye dabao") aur tap par toggle
     * karke onChanged(nayaHindiValue) call karta hai.
     */
    fun setupToggleButton(context: Context, button: TextView, onChanged: (Boolean) -> Unit) {
        fun render() {
            val hindi = isHindi(context)
            button.text = if (hindi) "EN" else "हिं"
            button.contentDescription = if (hindi) "English me dikhao" else "Hindi me dikhao"
        }
        render()
        button.setOnClickListener {
            val newValue = toggle(context)
            render()
            onChanged(newValue)
        }
    }
}
