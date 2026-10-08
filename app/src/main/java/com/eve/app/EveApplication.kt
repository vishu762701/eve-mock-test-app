package com.eve.app

import kotlinx.coroutines.launch
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
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (auth.currentUser != null) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.eve.app.worker.SubmitWorker.enqueueAllPending(this@EveApplication)
                }
            }
        } catch (_: Throwable) {}
    }
}
