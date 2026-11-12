/*
 * Derived from Telegram Android profile phone flow.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.databinding.ActivityChangePhoneBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChangePhoneActivity : EveBaseActivity() {
    private var editorOwnerUid: String? = null

    private lateinit var binding: ActivityChangePhoneBinding
    private var originalPhone: String = ""
    private var saveInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return
        binding = ActivityChangePhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentPhone = intent.getStringExtra("current_phone") ?: ""
        originalPhone = currentPhone.trim()
        if (currentPhone.isNotEmpty()) {
            binding.etPhone.setText(currentPhone)
        } else {
            binding.etPhone.setText("+91 ")
        }
        binding.etPhone.setSelection(binding.etPhone.text.length)

        binding.etPhone.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etPhone.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                savePhone()
                true
            } else {
                false
            }
        }

        binding.etPhone.doAfterTextChanged {
            binding.tvStatus.visibility = View.GONE
        }

        if (savedInstanceState == null) {
            binding.etPhone.doOnLayout {
                if (isFinishing || isDestroyed) return@doOnLayout
                binding.etPhone.requestFocus()
                binding.etPhone.setSelection(binding.etPhone.text.length)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(binding.etPhone, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }

        binding.btnDone.setOnClickListener {
            savePhone()
        }
    }

    private fun savePhone() {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        if (saveInFlight) return
        val input = binding.etPhone.text.toString().trim()
        val digitsOnly = input.filter { it.isDigit() }

        if (input.isNotEmpty() && input != "+91" && input != "+91 ") {
            if (digitsOnly.length < 7 || digitsOnly.length > 15) {
                binding.tvStatus.text = "Please enter a valid phone number (7-15 digits)"
                binding.tvStatus.visibility = View.VISIBLE
                TelegramShake.shakeViewSpring(binding.etPhone, 10f)
                VibrationHelper.vibrateAppError(this)
                return
            }
        }
        binding.tvStatus.visibility = View.GONE

        val phoneToSave = if (input == "+91" || input == "+91 ") "" else input
        if (phoneToSave == originalPhone) {
            finish()
            return
        }

        saveInFlight = true
        binding.btnDone.isEnabled = false
        binding.etPhone.isEnabled = false

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("phone" to phoneToSave), ProfileRequestOwner(ownerUid))
                if (res.success) {
                    // A successful mutation response is not evidence that this account can read the value back.
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                    if (confirmed.success && confirmed.data?.phone?.trim() == phoneToSave && confirmed.data.id == ownerUid &&
                        FirebaseAuth.getInstance().currentUser?.uid == ownerUid) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        binding.btnDone.isEnabled = true
                        AppBulletin.showError(
                            this@ChangePhoneActivity,
                            "Phone update could not be verified. Check the saved number before retrying."
                        )
                    }
                } else {
                    binding.btnDone.isEnabled = true
                    AppBulletin.showError(this@ChangePhoneActivity, res.error ?: "Failed to update phone number")
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                AppBulletin.showError(this@ChangePhoneActivity, e.localizedMessage ?: "Failed to update phone number")
            } finally {
                saveInFlight = false
                binding.etPhone.isEnabled = true
            }
        }
    }
}
