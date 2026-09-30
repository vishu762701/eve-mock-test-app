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
        ThemeManager.applySavedMode(this)
        com.eve.app.util.DebugCrashReporter.init(this)
        com.eve.app.util.FontManager.registerLifecycleCallbacks(this)
        com.eve.app.util.ThemeSwitchAnimator.ensureLifecycleRegistered(this)
        com.eve.app.util.SystemBarHelper.init(this)
        try {
            com.eve.app.worker.SubmitWorker.enqueueAllPending(this)
        } catch (_: Throwable) {}
    }
}
