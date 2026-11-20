/*
 * Derived from Telegram Android ChangeUsernameActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.TelegramHostActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChangeUsernameActivity : TelegramHostActivity<org.telegram.ui.ChangeUsernameActivity>() {
    private var editorOwnerUid: String? = null
    private var currentUsername: String = ""
    private var checkJob: Job? = null
    private var saveInFlight = false
    private var statusRole: Int = TelegramUsernameDelegate.ROLE_NONE

    override fun onBeforeCreateFragment(savedInstanceState: Bundle?): Boolean {
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return false
        currentUsername = (intent.getStringExtra("current_username") ?: "").removePrefix("@")
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (editorOwnerUid != null && currentUsername.isNotEmpty()) {
            fragment.fragmentView?.post {
                if (!isFinishing && !isDestroyed) {
                    fragment.recheckUsername()
                }
            }
        }
    }

    override fun createFragment(): org.telegram.ui.ChangeUsernameActivity {
        return org.telegram.ui.ChangeUsernameActivity().apply {
            setEveDelegate(object : TelegramUsernameDelegate {
                override fun getCurrentUsername(): String = currentUsername

                override fun getLinkPrefix(): String = "@"

                override fun getMinimumLength(): Int = 5

                override fun checkUsername(name: String, callback: TelegramUsernameDelegate.CheckCallback) {
                    checkJob?.cancel()
                    checkJob = lifecycleScope.launch {
                        try {
                            val res = ApiClient.apiService.checkUsername(name)
                            val field = fragment.getUsernameField()
                            if (field != null && field.text.toString().trim().removePrefix("@") != name) {
                                return@launch
                            }
                            if (res.success && res.data != null) {
                                val data = res.data
                                if (data.available) {
                                    callback.onResult(true, null)
                                } else {
                                    callback.onResult(false, data.error ?: getString(R.string.username_taken))
                                }
                            } else {
                                callback.onResult(false, res.error ?: getString(R.string.username_taken))
                            }
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (e: Exception) {
                            callback.onResult(
                                false,
                                getString(
                                    R.string.username_check_error,
                                    e.localizedMessage ?: getString(R.string.username_network_error)
                                )
                            )
                        }
                    }
                }

                override fun cancelCheck() {
                    checkJob?.cancel()
                }

                override fun saveUsername(name: String) {
                    performSaveUsername(name)
                }

                override fun onLinkCopied(url: String) {
                    AppBulletin.showSuccess(this@ChangeUsernameActivity, getString(R.string.username_copied))
                }

                override fun onErrorVibration() {
                    VibrationHelper.vibrateAppError(this@ChangeUsernameActivity)
                }

                override fun onStatusRoleChanged(role: Int) {
                    statusRole = role
                    applyStatusPalette(TelegramThemeEngine.resolveCurrentPalette(this@ChangeUsernameActivity))
                }
            })
        }
    }

    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        applyStatusPalette(palette)
    }

    private fun applyStatusPalette(palette: TelegramThemeEngine.ResolvedPalette) {
        val statusView = runCatching { fragment.getStatusTextView() }.getOrNull() ?: return
        val ink = when (statusRole) {
            TelegramUsernameDelegate.ROLE_AVAILABLE -> palette.statusSuccess
            TelegramUsernameDelegate.ROLE_INVALID -> palette.statusError
            else -> palette.statusPending
        }
        statusView.setTextColor(ink)
    }

    private fun performSaveUsername(rawName: String) {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) {
            finish()
            return
        }
        if (saveInFlight) return
        val target = rawName.trim().removePrefix("@")
        if (target.equals(currentUsername, ignoreCase = true)) {
            finish()
            return
        }
        if (target.isEmpty() || target.length < 5 || (!fragment.isLastNameAvailable() && !target.equals(currentUsername, ignoreCase = true))) {
            fragment.shakeIfOff()
            return
        }

        checkJob?.cancel()
        val normalizedTarget = target.lowercase(java.util.Locale.ROOT)
        saveInFlight = true
        val field = fragment.getUsernameField()
        val done = fragment.getDoneButton()
        done?.isEnabled = false
        field?.isEnabled = false

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(
                    mapOf("username" to normalizedTarget),
                    ProfileRequestOwner(ownerUid)
                )
                if (res.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                    val savedHandle = confirmed.data?.username?.removePrefix("@")
                    if (confirmed.success && savedHandle?.equals(normalizedTarget, ignoreCase = true) == true &&
                        confirmed.data?.id == ownerUid &&
                        FirebaseAuth.getInstance().currentUser?.uid == ownerUid
                    ) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        done?.isEnabled = true
                        field?.isEnabled = true
                        AppBulletin.showError(
                            this@ChangeUsernameActivity,
                            getString(R.string.username_save_unverified)
                        )
                        fragment.shakeIfOff()
                    }
                } else {
                    done?.isEnabled = true
                    field?.isEnabled = true
                    AppBulletin.showError(
                        this@ChangeUsernameActivity,
                        res.error ?: getString(R.string.username_save_failed)
                    )
                    fragment.shakeIfOff()
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                done?.isEnabled = true
                field?.isEnabled = true
                AppBulletin.showError(
                    this@ChangeUsernameActivity,
                    e.localizedMessage ?: getString(R.string.username_save_failed)
                )
                fragment.shakeIfOff()
            } finally {
                saveInFlight = false
            }
        }
    }

    override fun onDestroy() {
        checkJob?.cancel()
        super.onDestroy()
    }
}
