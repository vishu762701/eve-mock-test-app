/*
 * Derived from Telegram Android PasscodeActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.settings

import android.content.Intent
import android.os.Bundle
import com.eve.app.ui.common.TelegramHostActivity
import com.eve.app.util.PasscodeManager
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionIntroActivity

/**
 * Hosts the ported [org.telegram.ui.PasscodeActivity] (Settings, Setup PIN/Password,
 * and Unlock-to-Manage modes) and [org.telegram.ui.ActionIntroActivity] intro screen,
 * backed by Eve's [PasscodeManager] via [SharedConfig].
 */
class PasscodeActivity : TelegramHostActivity<BaseFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SharedConfig.loadFromEve(this)
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): BaseFragment {
        if (intent.getBooleanExtra(EXTRA_SHOW_INTRO, false) && !PasscodeManager.isPasscodeEnabled(this)) {
            return ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_SET_PASSCODE).apply {
                setOnPrimaryAction {
                    openMode(org.telegram.ui.PasscodeActivity.TYPE_SETUP_CODE)
                }
            }
        }
        val defaultMode = if (PasscodeManager.isPasscodeEnabled(this)) {
            org.telegram.ui.PasscodeActivity.TYPE_MANAGE_CODE_SETTINGS
        } else {
            org.telegram.ui.PasscodeActivity.TYPE_SETUP_CODE
        }
        val mode = intent.getIntExtra(EXTRA_PASSCODE_MODE, defaultMode)
        return org.telegram.ui.PasscodeActivity(mode).apply {
            setEveNavigator { nextMode -> openMode(nextMode) }
        }
    }

    private fun openMode(nextMode: Int) {
        val nextIntent = Intent(this, PasscodeActivity::class.java)
            .putExtra(EXTRA_PASSCODE_MODE, nextMode)
        startActivity(nextIntent)
        finish()
    }

    companion object {
        const val EXTRA_PASSCODE_MODE = "eve_passcode_mode"
        const val EXTRA_SHOW_INTRO = "eve_passcode_show_intro"
    }
}
