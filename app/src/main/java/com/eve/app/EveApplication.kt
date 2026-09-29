package com.eve.app

import android.app.Application
import com.eve.app.util.ThemeManager

class EveApplication : Application() {

    companion object {
        lateinit var instance: EveApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Saved dark/light choice sabse pehle apply karo, koi Activity dikhne se pehle
        // (warna ek split-second ke liye galat theme flash ho sakti hai).
        ThemeManager.applySavedMode(this)
        com.eve.app.util.ThemeSwitchAnimator.ensureLifecycleRegistered(this)
        com.eve.app.worker.SubmitWorker.enqueueAllPending(this)
        val deliberateError: String = 12345
    }
}
