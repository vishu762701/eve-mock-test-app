/*
 * Derived from Telegram Android ChangeBioActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.databinding.ActivityChangeBioBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

/** Authenticated personal Bio editor; never claims a save before server GET confirms it. */
class ChangeBioActivity : EveBaseActivity() {
    private var editorOwnerUid: String? = null
    private lateinit var binding: ActivityChangeBioBinding
    private var originalBio: String = ""
    private var saveInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return
        binding = ActivityChangeBioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A missing field means production has not yet deployed Bio. Fail closed.
        val original = intent.getStringExtra("current_bio")
        if (original == null) {
            finish()
            return
        }
        originalBio = original.replace("\n", "").trim()
        val bioLength = BioLengthInputFilter()
        binding.etBio.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            // ChangeBioActivity.java:112-115: an inserted newline at the end of
            // the source is rejected and immediately performs Done.
            if (source.isNotEmpty() && source.indexOf('\n') == source.length - 1) {
                saveBio()
                return@InputFilter ""
            }
            val result = bioLength.filter(source, start, end, dest, dstart, dend)
            // ChangeBioActivity.java:116-127: a truncated result shakes the
            // counter and issues the source's 200 ms vibration.
            if (result != null && result.length != source.length) {
                TelegramShake.shakeView(binding.tvCount)
                VibrationHelper.vibrateOneShot(this, 200)
            }
            result
        })
        binding.etBio.setText(original)
        binding.etBio.setSelection(binding.etBio.text.length)
        updateCount(binding.etBio.text.toString())

        // ChangeBioActivity.java:107,109: the field's final IME option is Done.
        binding.etBio.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etBio.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveBio()
                true
            } else {
                false
            }
        }
        binding.etBio.doAfterTextChanged { editable ->
            updateCount(editable?.toString().orEmpty())
            binding.tvStatus.visibility = View.GONE
        }

        // ChangeBioActivity.java:245-252 requests focus and shows the keyboard
        // when the editor opens.
        if (savedInstanceState == null) {
            binding.etBio.doOnLayout {
                if (isFinishing || isDestroyed) return@doOnLayout
                binding.etBio.requestFocus()
                binding.etBio.setSelection(binding.etBio.text.length)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(binding.etBio, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }
        binding.btnDone.setOnClickListener { saveBio() }
    }

    /**
     * ChangeBioActivity.java:88-102 counts Unicode code points; EVE's Worker
     * additionally caps UTF-16 units, so the stricter remaining value is shown.
     */
    private fun updateCount(text: String) {
        binding.tvCount.text = BioLengthInputFilter.remaining(text).toString()
    }

    private fun saveBio() {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        if (saveInFlight) return
        // ChangeBioActivity.java:210: newlines are not persisted. The EVE Worker
        // also trims stored strings, so validate/compare the trimmed value to
        // avoid a false readback failure for leading/trailing spaces.
        val bio = binding.etBio.text.toString().replace("\n", "").trim()
        if (!BioLengthInputFilter.fits(bio)) {
            binding.tvStatus.text = "Bio cannot exceed 160 characters"
            binding.tvStatus.visibility = View.VISIBLE
            // ChangeBioActivity.java:112-128: a rejected input shakes the
            // counter with the source damped-sine shake and 200 ms vibration.
            TelegramShake.shakeView(binding.tvCount)
            VibrationHelper.vibrateOneShot(this, 200)
            return
        }
        // ChangeBioActivity.java:211-215 closes the editor without a network
        // write when the confirmed bio is unchanged.
        if (bio == originalBio) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveInFlight = true
        binding.btnDone.isEnabled = false
        binding.etBio.isEnabled = false
        lifecycleScope.launch {
            try {
                val saved = ApiClient.apiService.updateProfile(mapOf("bio" to bio), ProfileRequestOwner(ownerUid))
                if (!saved.success) {
                    AppBulletin.showError(
                        this@ChangeBioActivity,
                        saved.error ?: "Bio could not be saved"
                    )
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
            } catch (error: Exception) {
                AppBulletin.showError(
                    this@ChangeBioActivity,
                    error.localizedMessage ?: "Bio could not be saved"
                )
            } finally {
                saveInFlight = false
                if (!isFinishing && !isDestroyed) {
                    binding.btnDone.isEnabled = true
                    binding.etBio.isEnabled = true
                }
            }
        }
    }
}
