/*
 * Derived from Telegram Android PasscodeActivity.java:1-850
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.eve.app.R
import com.eve.app.designsystem.HeaderCell
import com.eve.app.designsystem.PasscodeView
import com.eve.app.designsystem.ShadowSectionCell
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.designsystem.TextCell
import com.eve.app.designsystem.TextCheckCell
import com.eve.app.designsystem.TextDetailCell
import com.eve.app.designsystem.TextInfoPrivacyCell
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.PasscodeManager

/**
 * Exact Telegram Passcode Lock Screen & Settings Activity.
 * Upstream: PasscodeActivity.java
 */
class PasscodeActivity : EveBaseActivity() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var actionBar: TelegramActionBar
    private lateinit var settingsScrollView: ScrollView
    private lateinit var settingsContent: LinearLayout

    private var firstEnteredPin: String? = null
    private var isSettingNewPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(palette.windowBackground)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Telegram ActionBar (56dp)
        actionBar = TelegramActionBar(this).apply {
            setTitle("Passcode Lock")
            setBackButtonVisible(true)
            onBackPressed = { finish() }
        }
        mainLayout.addView(actionBar)

        // 2. Settings ScrollView
        settingsScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        settingsContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        settingsScrollView.addView(settingsContent)
        mainLayout.addView(settingsScrollView)

        rootContainer.addView(mainLayout)
        setContentView(rootContainer)

        refreshView()
    }

    private fun refreshView() {
        settingsContent.removeAllViews()

        if (PasscodeManager.isPasscodeEnabled(this) && !isSettingNewPin) {
            renderSettingsMode()
        } else {
            renderSetupMode()
        }
    }

    private fun renderSettingsMode() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        // Section 1: Main Lock Toggle & Change
        val cellLockToggle = TextCheckCell(this).apply {
            setTextAndCheck("Passcode Lock", checked = true, needDivider = true)
            setOnClickListener {
                promptDisablePasscode()
            }
        }
        settingsContent.addView(cellLockToggle)

        val cellChangePasscode = TextCell(this).apply {
            setText("Change Passcode", needDivider = false)
            setOnClickListener {
                isSettingNewPin = true
                firstEnteredPin = null
                refreshView()
            }
        }
        settingsContent.addView(cellChangePasscode)

        settingsContent.addView(ShadowSectionCell(this))

        // Section 2: Biometrics & Auto-lock
        val cellFingerprint = TextCheckCell(this).apply {
            setTextAndCheck(
                "Unlock with Fingerprint",
                checked = PasscodeManager.isFingerprintEnabled(this@PasscodeActivity),
                needDivider = true
            )
            setOnClickListener {
                val newState = !isChecked
                isChecked = newState
                PasscodeManager.setFingerprintEnabled(this@PasscodeActivity, newState)
            }
        }
        settingsContent.addView(cellFingerprint)

        val autoLockText = when (PasscodeManager.getAutoLockTimeout(this)) {
            PasscodeManager.TIMEOUT_DISABLED -> "Disabled"
            PasscodeManager.TIMEOUT_1_MINUTE -> "in 1 minute"
            PasscodeManager.TIMEOUT_5_MINUTES -> "in 5 minutes"
            PasscodeManager.TIMEOUT_1_HOUR -> "in 1 hour"
            PasscodeManager.TIMEOUT_5_HOURS -> "in 5 hours"
            else -> "in 5 minutes"
        }

        val cellAutoLock = TextDetailCell(this).apply {
            setTextAndValue("Auto-lock", autoLockText, needDivider = false)
            setOnClickListener {
                showAutoLockPicker()
            }
        }
        settingsContent.addView(cellAutoLock)

        settingsContent.addView(ShadowSectionCell(this))

        // Section 3: Privacy Info
        val privacyInfo = TextInfoPrivacyCell(this).apply {
            setText("When a passcode is set, a lock icon appears on the home screen. Tap it to immediately lock the app.")
        }
        settingsContent.addView(privacyInfo)
    }

    private fun renderSetupMode() {
        val passcodeView = PasscodeView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            if (firstEnteredPin == null) {
                titleTextView.text = "Set a Passcode"
                subtitleTextView.text = "Enter a 4-digit passcode"
            } else {
                titleTextView.text = "Re-enter Passcode"
                subtitleTextView.text = "Confirm your 4-digit passcode"
            }

            onPasscodeEntered = { pin ->
                handlePinInput(pin)
            }
        }

        settingsContent.addView(passcodeView)
    }

    private fun handlePinInput(pin: String): Boolean {
        if (firstEnteredPin == null) {
            firstEnteredPin = pin
            renderSetupMode()
            return true
        } else {
            if (pin == firstEnteredPin) {
                PasscodeManager.setPasscode(this, pin)
                PasscodeManager.unlock(this)
                AppBulletin.showSuccess(this, "Passcode lock enabled")
                isSettingNewPin = false
                firstEnteredPin = null
                refreshView()
                return true
            } else {
                AppBulletin.showError(this, "Passcodes do not match")
                firstEnteredPin = null
                renderSetupMode()
                return false
            }
        }
    }

    private fun promptDisablePasscode() {
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Disable Passcode")
            .setMessage("Are you sure you want to turn off the passcode lock?")
            .setPositiveButton("Turn Off") { _, _ ->
                PasscodeManager.disablePasscode(this)
                AppBulletin.showSuccess(this, "Passcode lock turned off")
                refreshView()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun showAutoLockPicker() {
        val options = arrayOf(
            "Disabled" to PasscodeManager.TIMEOUT_DISABLED,
            "in 1 minute" to PasscodeManager.TIMEOUT_1_MINUTE,
            "in 5 minutes" to PasscodeManager.TIMEOUT_5_MINUTES,
            "in 1 hour" to PasscodeManager.TIMEOUT_1_HOUR,
            "in 5 hours" to PasscodeManager.TIMEOUT_5_HOURS
        )
        val items = options.map { it.first }.toTypedArray()
        val currentTimeout = PasscodeManager.getAutoLockTimeout(this)
        val checkedIndex = options.indexOfFirst { it.second == currentTimeout }.coerceAtLeast(0)

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Auto-lock")
            .setSingleChoiceItems(items, checkedIndex) { dialog, which ->
                val selectedTimeout = options[which].second
                PasscodeManager.setAutoLockTimeout(this, selectedTimeout)
                dialog.dismiss()
                refreshView()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}
