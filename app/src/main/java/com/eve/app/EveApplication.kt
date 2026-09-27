package com.eve.app

import android.app.Application
import com.eve.app.util.ThemeManager

class EveApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Saved dark/light choice sabse pehle apply karo, koi Activity dikhne se pehle
        // (warna ek split-second ke liye galat theme flash ho sakti hai).
        ThemeManager.applySavedMode(this)
        com.eve.app.util.ThemeSwitchAnimator.ensureLifecycleRegistered(this)
    }
}
