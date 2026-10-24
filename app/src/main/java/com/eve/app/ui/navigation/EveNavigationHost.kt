package com.eve.app.ui.navigation

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.home.MainActivity
import com.eve.app.ui.profile.ProfileActivity
import com.eve.app.ui.settings.SettingsActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Attaches and coordinates the Telegram-style floating bottom navigation pill
 * on EVE's 4 primary destinations (Home, History, Settings, Profile).
 * Enforces single-top reordering (no activity stack explosion) and scroll-to-top on reselect.
 */
object EveNavigationHost {

    fun attach(
        activity: AppCompatActivity,
        currentTab: Int,
        onReselect: (() -> Unit)? = null
    ): EveBottomPillBar {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: throw IllegalStateException("Activity content view not found")

        // Reuse existing pill if already attached
        val existing = root.findViewWithTag<EveBottomPillBar>("eve_bottom_pill_bar")
        val pill = existing ?: EveBottomPillBar(activity).apply {
            tag = "eve_bottom_pill_bar"
            val density = resources.displayMetrics.density
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (8 * density).toInt() // MAIN_TABS_MARGIN
            }
            layoutParams = lp
            root.addView(this)
        }

        pill.selectTab(currentTab, animated = false, notify = false)

        // Load profile avatar
        val user = FirebaseAuth.getInstance().currentUser
        pill.setUserAvatar(user?.photoUrl?.toString())

        // Tab selection listener
        pill.setTabSelectionListener(object : MainTabsLayout.TabSelectionListener {
            override fun onTabSelected(position: Int) {
                if (position == currentTab) {
                    onReselect?.invoke()
                    return
                }

                val targetClass = when (position) {
                    EveBottomPillBar.TAB_HOME -> MainActivity::class.java
                    EveBottomPillBar.TAB_HISTORY -> HistoryActivity::class.java
                    EveBottomPillBar.TAB_SETTINGS -> SettingsActivity::class.java
                    EveBottomPillBar.TAB_PROFILE -> ProfileActivity::class.java
                    else -> null
                }

                if (targetClass != null && targetClass != activity::class.java) {
                    val intent = Intent(activity, targetClass).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    activity.startActivity(intent)
                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }

            override fun onTabReselected(position: Int) {
                onReselect?.invoke()
            }
        })

        // Listen for theme palette changes
        val themeListener: (TelegramThemeEngine.ResolvedPalette) -> Unit = { palette ->
            pill.updateThemePalette(palette)
        }
        TelegramThemeEngine.addThemeChangeListener(themeListener)

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                pill.selectTab(currentTab, animated = false, notify = false)
                val currentUser = FirebaseAuth.getInstance().currentUser
                pill.setUserAvatar(currentUser?.photoUrl?.toString())
            }

            override fun onDestroy(owner: LifecycleOwner) {
                TelegramThemeEngine.removeThemeChangeListener(themeListener)
            }
        })

        return pill
    }
}
