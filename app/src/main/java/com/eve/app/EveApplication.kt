package com.eve.app

import kotlinx.coroutines.launch
import android.app.Application

class EveApplication : Application() {

    companion object {
        lateinit var instance: EveApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize TelegramThemeEngine as single authoritative theme engine before any Activity is created
        com.eve.app.theme.TelegramThemeEngine.init(this)
        com.eve.app.util.DebugCrashReporter.init(this)
        com.eve.app.util.ThemeSwitchAnimator.ensureLifecycleRegistered(this)
        com.eve.app.util.SystemBarHelper.init(this)
        com.eve.app.util.ProfilePresenceClient.register()
        try {
            val typoRepo = com.eve.app.typography.TypographyRepository.getInstance(this)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                typoRepo.fetchPublishedConfig()
            }
        } catch (_: Throwable) {}
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                com.eve.app.util.ProfilePresenceClient.onAuthChanged(auth.currentUser != null)
                if (auth.currentUser != null) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.eve.app.worker.SubmitWorker.enqueueAllPending(this@EveApplication)
                }
            }
        } catch (_: Throwable) {}
    }
}
