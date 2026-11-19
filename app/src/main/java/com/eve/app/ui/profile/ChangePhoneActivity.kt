/*
 * Derived from Telegram Android profile phone flow.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper

/**
 * Profile > Phone Number Editor powered by real ported Telegram ActionBar,
 * HeaderCell, EditTextBoldCursor, TextInfoPrivacyCell, and spring shake physics.
 */
class ChangePhoneActivity : EveBaseActivity() {
    private var editorOwnerUid: String? = null
    private var originalPhone: String = ""
    private var saveInFlight = false

    private lateinit var rootContainer: FrameLayout
    private lateinit var actionBar: ActionBar
    private lateinit var doneButton: View
    private lateinit var etPhone: EditTextBoldCursor
    private lateinit var tvStatus: TextView
    private lateinit var inputCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return

        Theme.syncFromEvePalette(this)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootContainer)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootContainer.addView(mainLayout)

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Phone Number")
            setAllowOverlayTitle(true)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) {
                        EveNavigationHelper.finishWithTransition(this@ChangePhoneActivity)
                    } else if (id == DONE_BUTTON_ID) {
                        savePhone()
                    }
                }
            })
        }
        doneButton = actionBar.createMenu().addItemWithWidth(
            DONE_BUTTON_ID,
            R.drawable.ic_ab_done,
            AndroidUtilities.dp(56f),
            getString(R.string.Done)
        )
        mainLayout.addView(actionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // 2. Real Telegram Section Card (HeaderCell + EditTextBoldCursor)
        inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        val header = HeaderCell(this).apply {
            setText("Phone Number")
        }
        inputCard.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        etPhone = EditTextBoldCursor(this).apply {
            id = R.id.etPhone
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
            setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated),
                Theme.getColor(Theme.key_text_RedRegular)
            )
            setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setCursorSize(AndroidUtilities.dp(20f))
            setCursorWidth(1.5f)
            maxLines = 1
            isSingleLine = true
            hint = "+91 9876543210"
            inputType = InputType.TYPE_CLASS_PHONE
            imeOptions = EditorInfo.IME_ACTION_DONE
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(0, AndroidUtilities.dp(6f), 0, AndroidUtilities.dp(10f))
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    savePhone()
                    true
                } else {
                    false
                }
            }
        }
        inputCard.addView(
            etPhone,
            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 21f, 4f, 21f, 14f)
        )

        tvStatus = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(palette.statusError)
            visibility = View.GONE
            setPadding(AndroidUtilities.dp(21f), 0, AndroidUtilities.dp(21f), AndroidUtilities.dp(12f))
        }
        inputCard.addView(tvStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        mainLayout.addView(inputCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        mainLayout.addView(ShadowSectionCell(this))
        val infoCell = TextInfoPrivacyCell(this).apply {
            setText("Enter your phone number with country code (7–15 digits). Leave empty to remove your phone number.")
        }
        mainLayout.addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val currentPhone = intent.getStringExtra("current_phone") ?: ""
        originalPhone = currentPhone.trim()
        if (savedInstanceState == null) {
            if (currentPhone.isNotEmpty()) {
                etPhone.setText(currentPhone)
            } else {
                etPhone.setText("+91 ")
            }
            etPhone.setSelection(etPhone.text?.length ?: 0)
            etPhone.doOnLayout {
                if (isFinishing || isDestroyed) return@doOnLayout
                etPhone.requestFocus()
                etPhone.setSelection(etPhone.text?.length ?: 0)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(etPhone, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvStatus.visibility = View.GONE
                applyEditorPalette(TelegramThemeEngine.resolveCurrentPalette(this@ChangePhoneActivity))
            }
        })
    }

    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        if (::etPhone.isInitialized) applyEditorPalette(palette)
    }

    private fun applyEditorPalette(palette: TelegramThemeEngine.ResolvedPalette) {
        Theme.syncFromEvePalette(this)
        rootContainer.setBackgroundColor(palette.windowBackground)
        inputCard.setBackgroundColor(palette.surface)
        tvStatus.setTextColor(palette.statusError)
        etPhone.setTextColor(palette.textPrimary)
        etPhone.setHintTextColor(palette.textSecondary)
        etPhone.setCursorColor(palette.textPrimary)
        etPhone.setErrorLineColor(palette.statusError)
    }

    private fun savePhone() {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        if (saveInFlight) return
        val input = etPhone.text?.toString()?.trim() ?: ""
        val digitsOnly = input.filter { it.isDigit() }

        if (input.isNotEmpty() && input != "+91" && input != "+91 ") {
            if (digitsOnly.length < 7 || digitsOnly.length > 15) {
                tvStatus.text = "Please enter a valid phone number (7-15 digits)"
                tvStatus.visibility = View.VISIBLE
                applyEditorPalette(TelegramThemeEngine.resolveCurrentPalette(this))
                TelegramShake.shakeViewSpring(etPhone)
                VibrationHelper.vibrateAppError(this)
                return
            }
        }
        tvStatus.visibility = View.GONE

        val phoneToSave = if (input == "+91" || input == "+91 ") "" else input
        if (phoneToSave == originalPhone) {
            finish()
            return
        }

        saveInFlight = true
        doneButton.isEnabled = false
        etPhone.isEnabled = false

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("phone" to phoneToSave), ProfileRequestOwner(ownerUid))
                if (res.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                    if (confirmed.success && confirmed.data?.phone?.trim() == phoneToSave && confirmed.data.id == ownerUid &&
                        FirebaseAuth.getInstance().currentUser?.uid == ownerUid) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        doneButton.isEnabled = true
                        AppBulletin.showError(
                            this@ChangePhoneActivity,
                            "Phone update could not be verified. Check the saved number before retrying."
                        )
                    }
                } else {
                    doneButton.isEnabled = true
                    AppBulletin.showError(this@ChangePhoneActivity, res.error ?: "Failed to update phone number")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                doneButton.isEnabled = true
                AppBulletin.showError(this@ChangePhoneActivity, e.localizedMessage ?: "Failed to update phone number")
            } finally {
                saveInFlight = false
                etPhone.isEnabled = true
            }
        }
    }

    companion object {
        private const val DONE_BUTTON_ID = 1
    }
}
