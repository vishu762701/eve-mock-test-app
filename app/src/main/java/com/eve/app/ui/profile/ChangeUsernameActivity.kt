/*
 * Derived from Telegram Android ChangeUsernameActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivityChangeUsernameBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChangeUsernameActivity : EveBaseActivity() {

    private lateinit var binding: ActivityChangeUsernameBinding
    private var currentUsername: String = ""
    private var checkJob: Job? = null
    private var isUsernameValid: Boolean = false

    private val COLOR_GREEN = 0xFF4FA348.toInt()
    private val COLOR_RED = 0xFFE53935.toInt()
    private val COLOR_GRAY = 0xFF707579.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeUsernameBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            isUsernameValid = false
            return
        }

        binding.tvStatus.visibility = View.VISIBLE

        // Telegram-exact rules (ChangeUsernameActivity.java:1198-1280)
        if (name.startsWith("_") || name.endsWith("_")) {
            setStatus("Sorry, this username is invalid.", COLOR_RED)
            isUsernameValid = false
            return
        }

        val firstChar = name[0]
        if (firstChar in '0'..'9') {
            setStatus("Sorry, a username can't start with a number.", COLOR_RED)
            isUsernameValid = false
            return
        }

        for (ch in name) {
            val validChar = (ch in '0'..'9') || (ch in 'a'..'z') || (ch in 'A'..'Z') || ch == '_'
            if (!validChar) {
                setStatus("Sorry, this username is invalid.", COLOR_RED)
                isUsernameValid = false
                return
            }
        }

        if (name.length < 5) {
            setStatus("A username must have at least 5 characters.", COLOR_RED)
            isUsernameValid = false
            return
        }

        if (name.length > 32) {
            setStatus("The username must not exceed 32 characters.", COLOR_RED)
            isUsernameValid = false
            return
        }

        // Current username check
        if (name.equals(currentUsername, ignoreCase = true)) {
            setStatus(String.format("%1\$s is available.", name), COLOR_GREEN)
            isUsernameValid = true
            return
        }

        setStatus("Checking username...", COLOR_GRAY)
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
                        setStatus(String.format("%1\$s is available.", name), COLOR_GREEN)
                        isUsernameValid = true
                    } else {
                        val errMsg = data.error ?: "Sorry, this username is already taken."
                        setStatus(errMsg, COLOR_RED)
                        isUsernameValid = false
                    }
                } else {
                    setStatus(res.error ?: "Sorry, this username is already taken.", COLOR_RED)
                    isUsernameValid = false
                }
            } catch (e: Exception) {
                setStatus("Error checking username: ${e.localizedMessage ?: "Network error"}", COLOR_RED)
                isUsernameValid = false
            }
        }
    }

    private fun setStatus(text: String, color: Int) {
        binding.tvStatus.text = text
        binding.tvStatus.setTextColor(color)
        binding.lineUsername.setBackgroundColor(if (color == COLOR_RED) color else 0xFF229AF0.toInt())
    }

    private fun saveUsername() {
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
                val res = ApiClient.apiService.updateProfile(mapOf("username" to normalizedTarget))
                if (res.success) {
                    // Only finish after the same authenticated profile can read the saved handle.
                    val confirmed = ApiClient.apiService.getProfile()
                    val savedHandle = confirmed.data?.username?.removePrefix("@")
                    if (confirmed.success && savedHandle?.equals(normalizedTarget, ignoreCase = true) == true) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        binding.btnDone.isEnabled = true
                        binding.etUsername.isEnabled = true
                        AppBulletin.showError(this@ChangeUsernameActivity,
                            "Username update could not be verified. Check your profile before retrying.")
                    }
                } else {
                    binding.btnDone.isEnabled = true
                    binding.etUsername.isEnabled = true
                    AppBulletin.showError(this@ChangeUsernameActivity, res.error ?: "Failed to update username")
                }
            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                binding.etUsername.isEnabled = true
                AppBulletin.showError(this@ChangeUsernameActivity, e.localizedMessage ?: "Failed to update username")
            }
        }
    }
}
