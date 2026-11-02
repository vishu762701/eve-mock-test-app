/*
 * Derived from Telegram Android ChangeUsernameActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.databinding.ActivityChangeUsernameBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChangeUsernameActivity : EveBaseActivity() {
    private var editorOwnerUid: String? = null

    private lateinit var binding: ActivityChangeUsernameBinding
    private var currentUsername: String = ""
    private var checkJob: Job? = null
    private var isUsernameValid: Boolean = false

    private enum class StatusRole { AVAILABLE, INVALID, CHECKING }
    private var statusRole: StatusRole? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return
        binding = ActivityChangeUsernameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvStatus.tag = "semantic_status"

        currentUsername = intent.getStringExtra("current_username") ?: ""
        if (currentUsername.startsWith("@")) {
            currentUsername = currentUsername.substring(1)
        }

        binding.etUsername.setText(currentUsername)
        binding.etUsername.setSelection(binding.etUsername.text.length)

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }

        binding.btnDone.setOnClickListener {
            saveUsername()
        }

        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = (s?.toString() ?: "").trim().removePrefix("@")
                checkUsernameLocallyAndRemotely(input)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (currentUsername.isNotEmpty()) {
            checkUsernameLocallyAndRemotely(currentUsername)
        }
    }

    private fun checkUsernameLocallyAndRemotely(name: String) {
        checkJob?.cancel()
        if (name.isEmpty()) {
            binding.tvStatus.visibility = View.GONE
            statusRole = null
            applyStatusPalette(TelegramThemeEngine.resolveCurrentPalette(this))
            isUsernameValid = false
            return
        }

        binding.tvStatus.visibility = View.VISIBLE

        // ADAPTED ChangeUsernameActivity.java:1205-1280: EVE retains its server minimum of 5, not Telegram service-specific 4.
        if (name.startsWith("_") || name.endsWith("_")) {
            setStatus(getString(R.string.username_invalid), StatusRole.INVALID)
            isUsernameValid = false
            return
        }

        val firstChar = name[0]
        if (firstChar in '0'..'9') {
            setStatus(getString(R.string.username_invalid_number), StatusRole.INVALID)
            isUsernameValid = false
            return
        }

        for (ch in name) {
            val validChar = (ch in '0'..'9') || (ch in 'a'..'z') || (ch in 'A'..'Z') || ch == '_'
            if (!validChar) {
                setStatus(getString(R.string.username_invalid), StatusRole.INVALID)
                isUsernameValid = false
                return
            }
        }

        if (name.length < 5) {
            setStatus(getString(R.string.username_too_short), StatusRole.INVALID)
            isUsernameValid = false
            return
        }

        if (name.length > 32) {
            setStatus(getString(R.string.username_too_long), StatusRole.INVALID)
            isUsernameValid = false
            return
        }

        // Current username check
        if (name.equals(currentUsername, ignoreCase = true)) {
            setStatus(getString(R.string.username_available, name), StatusRole.AVAILABLE)
            isUsernameValid = true
            return
        }

        setStatus(getString(R.string.username_checking), StatusRole.CHECKING)
        isUsernameValid = false

        checkJob = lifecycleScope.launch {
            delay(300) // Telegram debounce 300ms
            try {
                val res = ApiClient.apiService.checkUsername(name)
                // A cancelled or delayed availability response must never validate newer input.
                if (binding.etUsername.text.toString().trim().removePrefix("@") != name) return@launch
                if (res.success && res.data != null) {
                    val data = res.data
                    if (data.available) {
                        setStatus(getString(R.string.username_available, name), StatusRole.AVAILABLE)
                        isUsernameValid = true
                    } else {
                        val errMsg = data.error ?: getString(R.string.username_taken)
                        setStatus(errMsg, StatusRole.INVALID)
                        isUsernameValid = false
                    }
                } else {
                    setStatus(res.error ?: getString(R.string.username_taken), StatusRole.INVALID)
                    isUsernameValid = false
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                setStatus(getString(R.string.username_check_error, e.localizedMessage ?: getString(R.string.username_network_error)), StatusRole.INVALID)
                isUsernameValid = false
            }
        }
    }

    private fun setStatus(text: String, role: StatusRole) {
        binding.tvStatus.text = text
        statusRole = role
        applyStatusPalette(TelegramThemeEngine.resolveCurrentPalette(this))
    }

    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        if (::binding.isInitialized) applyStatusPalette(palette)
    }

    private fun applyStatusPalette(palette: TelegramThemeEngine.ResolvedPalette) {
        val ink = when (statusRole) {
            StatusRole.AVAILABLE -> palette.statusSuccess
            StatusRole.INVALID -> palette.statusError
            StatusRole.CHECKING, null -> palette.statusPending
        }
        binding.tvStatus.setTextColor(ink)
        binding.lineUsername.setBackgroundColor(if (statusRole == StatusRole.INVALID) ink else palette.accent)
    }

    private fun saveUsername() {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        val target = binding.etUsername.text.toString().trim().removePrefix("@")
        if (!isUsernameValid && !target.equals(currentUsername, ignoreCase = true)) {
            return
        }

        checkJob?.cancel()
        val normalizedTarget = target.lowercase(java.util.Locale.ROOT)
        binding.btnDone.isEnabled = false
        binding.etUsername.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("username" to normalizedTarget), ProfileRequestOwner(ownerUid))
                if (res.success) {
                    // Only finish after the same authenticated profile can read the saved handle.
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                    val savedHandle = confirmed.data?.username?.removePrefix("@")
                    if (confirmed.success && savedHandle?.equals(normalizedTarget, ignoreCase = true) == true && confirmed.data?.id == ownerUid &&
                        FirebaseAuth.getInstance().currentUser?.uid == ownerUid) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        binding.btnDone.isEnabled = true
                        binding.etUsername.isEnabled = true
                        AppBulletin.showError(this@ChangeUsernameActivity,
                            getString(R.string.username_save_unverified))
                    }
                } else {
                    binding.btnDone.isEnabled = true
                    binding.etUsername.isEnabled = true
                    AppBulletin.showError(this@ChangeUsernameActivity, res.error ?: getString(R.string.username_save_failed))
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                binding.etUsername.isEnabled = true
                AppBulletin.showError(this@ChangeUsernameActivity, e.localizedMessage ?: getString(R.string.username_save_failed))
            }
        }
    }
}
