package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.databinding.ActivityChangeBioBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

/** Authenticated personal Bio editor; never claims a save before server GET confirms it. */
class ChangeBioActivity : EveBaseActivity() {
    private var editorOwnerUid: String? = null
    private lateinit var binding: ActivityChangeBioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeBioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        editorOwnerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (editorOwnerUid == null) { finish(); return }
        ProfileEditorAccountGuard.bind(this, requireNotNull(editorOwnerUid))

        // A missing field means production has not yet deployed Bio. Fail closed.
        val original = intent.getStringExtra("current_bio")
        if (original == null) {
            finish()
            return
        }
        binding.etBio.setText(original)
        binding.etBio.setSelection(binding.etBio.text.length)
        binding.tvCount.text = "${binding.etBio.text.length}/160"
        binding.etBio.doAfterTextChanged {
            binding.tvCount.text = "${it?.length ?: 0}/160"
            binding.tvStatus.visibility = View.GONE
        }

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }
        binding.btnDone.setOnClickListener { saveBio() }
    }

    private fun saveBio() {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        val bio = binding.etBio.text.toString().trim()
        if (bio.length > 160) {
            binding.tvStatus.text = "Bio cannot exceed 160 characters"
            binding.tvStatus.visibility = View.VISIBLE
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.btnDone.isEnabled = false
        lifecycleScope.launch {
            try {
                val saved = ApiClient.apiService.updateProfile(mapOf("bio" to bio), ProfileRequestOwner(ownerUid))
                if (!saved.success) {
                    binding.btnDone.isEnabled = true
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
                    binding.btnDone.isEnabled = true
                    AppBulletin.showError(
                        this@ChangeBioActivity,
                        "Bio update could not be verified. Check the saved value before retrying."
                    )
                }
            } catch (error: Exception) {
                binding.btnDone.isEnabled = true
                AppBulletin.showError(
                    this@ChangeBioActivity,
                    error.localizedMessage ?: "Bio could not be saved"
                )
            }
        }
    }
}
