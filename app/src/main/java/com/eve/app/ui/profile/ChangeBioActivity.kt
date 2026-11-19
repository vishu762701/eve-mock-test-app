package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.ui.common.TelegramHostActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.ui.ChangeBioActivity as TelegramChangeBioActivity

/**
 * Host Activity for Telegram ChangeBioActivity port (C2).
 * Extends TelegramHostActivity (INavigationLayout) and implements TelegramProfileDelegates.ChangeBioDelegate.
 */
class ChangeBioActivity : TelegramHostActivity<TelegramChangeBioActivity>(), TelegramProfileDelegates.ChangeBioDelegate {
    private var editorOwnerUid: String? = null
    private var originalBio: String = ""
    private var initialRawBio: String = ""
    private var saveInFlight = false

    override fun onBeforeCreateFragment(savedInstanceState: Bundle?): Boolean {
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return false
        val original = intent.getStringExtra("current_bio")
        if (original == null) {
            finish()
            return false
        }
        initialRawBio = original
        originalBio = original.replace("\n", "").trim()
        return true
    }

    override fun createTelegramFragment(savedInstanceState: Bundle?): TelegramChangeBioActivity {
        return TelegramChangeBioActivity().apply {
            setEveDelegate(this@ChangeBioActivity)
        }
    }

    override fun onFragmentHosted(fragment: TelegramChangeBioActivity) {
        fragment.bioField?.id = R.id.etBio
    }

    override fun getInitialBio(): String = initialRawBio

    override fun getBioMaxLength(): Int = BioLengthInputFilter.MAX_LENGTH

    override fun saveBio(newBio: String) {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        if (saveInFlight) return

        val bio = newBio.replace("\n", "").trim()
        if (!BioLengthInputFilter.fits(bio)) {
            TelegramShake.shakeView(fragment.fragmentView)
            VibrationHelper.vibrateOneShot(this, 200)
            AppBulletin.showError(this, "Bio cannot exceed 160 characters")
            return
        }
        if (bio == originalBio) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveInFlight = true
        fragment.bioField?.isEnabled = false
        lifecycleScope.launch {
            try {
                val saved = ApiClient.apiService.updateProfile(mapOf("bio" to bio), ProfileRequestOwner(ownerUid))
                if (!saved.success) {
                    AppBulletin.showError(this@ChangeBioActivity, saved.error ?: "Bio could not be saved")
                    return@launch
                }
                val readback = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                if (readback.data?.id == ownerUid && ProfileSaveConfirmation.matches(
                        userId, FirebaseAuth.getInstance().currentUser?.uid,
                        readback.success, bio, readback.data?.bio
                    )) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    AppBulletin.showError(
                        this@ChangeBioActivity,
                        "Bio update could not be verified. Check the saved value before retrying."
                    )
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (error: Exception) {
                AppBulletin.showError(
                    this@ChangeBioActivity,
                    error.localizedMessage ?: "Bio could not be saved"
                )
            } finally {
                saveInFlight = false
                if (!isFinishing && !isDestroyed) {
                    fragment.bioField?.isEnabled = true
                }
            }
        }
    }
}
