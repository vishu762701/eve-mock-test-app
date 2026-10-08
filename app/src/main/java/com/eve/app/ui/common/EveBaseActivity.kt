package com.eve.app.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eve.app.util.FontManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.eve.app.uistudio.StudioScreens
import com.eve.app.uistudio.StudioRenderer
import com.eve.app.data.repository.UiStudioRepository

/**
 * Base activity for all activities in EVE Mock Test App.
 * Handles:
 * 1. Runtime font theme overlay application before view inflation (API 24+ compatible).
 * 2. Automatic back-stack font synchronization on onResume via in-memory fontVersion.
 */
abstract class EveBaseActivity : AppCompatActivity() {

    private var activityFontVersion: Int = 0
    private var studioRoot: View? = null
    private var studioListener: ViewTreeObserver.OnPreDrawListener? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val screen = StudioScreens.all.firstOrNull { it.activity == javaClass.simpleName } ?: return
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        studioRoot = root
        val repository = UiStudioRepository.getInstance()
        // Rebinding may change fill/alpha without a layout. Reconcile before visible pixels draw.
        val listener = ViewTreeObserver.OnPreDrawListener {
            StudioRenderer.apply(root, screen.key, repository.currentConfig)
            true
        }
        studioListener = listener
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                root.viewTreeObserver.addOnPreDrawListener(listener)
                try {
                    launch { repository.fetchPublishedConfig(forceRefresh = true) }
                    repository.activeConfigFlow.collect { StudioRenderer.apply(root, screen.key, it) }
                } finally {
                    if (root.viewTreeObserver.isAlive) root.viewTreeObserver.removeOnPreDrawListener(listener)
                }
            }
        }
    }

    override fun onDestroy() {
        studioListener?.let { studioRoot?.viewTreeObserver?.removeOnPreDrawListener(it) }
        studioRoot = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        FontManager.apply(this)
        activityFontVersion = FontManager.fontVersion
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (activityFontVersion != FontManager.fontVersion) {
            activityFontVersion = FontManager.fontVersion
            recreate()
        }
    }
}
