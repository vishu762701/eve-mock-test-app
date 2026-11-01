package com.eve.app

import android.app.Application
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.SystemBarHelper
import com.eve.app.util.ThemeSwitchAnimator

/**
 * JVM UI fixtures keep the real palette/inset/lifecycle setup without starting production
 * Firebase auth, presence, pending submissions or typography HTTP jobs. Application startup
 * and real service integration belong in device/integration tests, not isolated UI fixtures.
 */
class OfflineUiTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TelegramThemeEngine.init(this)
        ThemeSwitchAnimator.ensureLifecycleRegistered(this)
        SystemBarHelper.init(this)
    }
}
