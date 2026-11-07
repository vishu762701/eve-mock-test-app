package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.model.AppContent
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.databinding.ActivityEditAboutBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.util.AppBulletin
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EditAboutActivity : EveBaseActivity() {
    private lateinit var binding: ActivityEditAboutBinding
    private val repo = AppContentRepository()
    private val adminRepo = AdminRepository()
    private var currentType = AppContentRepository.TYPE_PRIVACY
    private var initialContent = AppContent()
    private var initiatingUid = ""
    private var initiatingEmail = ""
    private var adminReady = false
    private var contentReady = false
    private var loading = false
    private var saving = false
    private var epoch = 0L
    private var readGeneration = 0L
    private var readJob: Job? = null
    private var saveJob: Job? = null
    private var saveEpoch = -1L
    private var afterSuccessfulSave: (() -> Unit)? = null
    private var reloadAfterStop = true
    private var reauthorizeOnResume = false
    private var retainedDraft: AppContent? = null
    private var suppressTabSelection = false
    private var unsavedDialog: TelegramNativeAlertDialog? = null

    private fun ownerCurrent(): Boolean {
        val live = FirebaseAuth.getInstance().currentUser
        return adminReady && !isFinishing && !isDestroyed && AdminIdentityGuard.allowed(
            true, initiatingUid, initiatingEmail, live?.uid, live?.email)
    }
    private fun current(requestEpoch: Long): Boolean = requestEpoch == epoch && ownerCurrent() &&
        lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    private fun requireCurrent(requestEpoch: Long) {
        if (!current(requestEpoch)) throw CancellationException("Content editor owner or lifecycle changed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val owner = FirebaseAuth.getInstance().currentUser
        initiatingUid = owner?.uid.orEmpty()
        initiatingEmail = owner?.email.orEmpty()
        currentType = savedInstanceState?.getString("content.type")?.takeIf { it in TYPES } ?: TYPES[0]
        retainedDraft = savedInstanceState?.getBundle("content.draft")?.let { readDraft(it) }
        initialContent = savedInstanceState?.getBundle("content.confirmed")?.let { readDraft(it) } ?: AppContent()
        binding = ActivityEditAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { handleBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })
        binding.progressBar.visibility = View.VISIBLE
        updateControls()
        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true,
            requiredRight = "manage_content", restorationScope = "edit_about", isRestoring = savedInstanceState != null) {
            adminReady = true
            binding.btnSave.setOnClickListener { saveCurrent() }
            restoreSelectedTab()
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (suppressTabSelection) return
                    val target = TYPES.getOrNull(tab?.position ?: -1) ?: return
                    if (target == currentType) return
                    if (!current(epoch) || saving || unsavedDialog?.isShowing == true) {
                        restoreSelectedTab(); return
                    }
                    if (isDirty()) {
                        showUnsavedConfirmation(leaving = false) { switchTab(target) }
                    } else switchTab(target)
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            })
            loadContent()
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != initiatingUid || live.email != initiatingEmail) {
                finish()
                return
            }
            contentReady = false
            updateControls()
            com.eve.app.util.AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "manage_content"
            ) {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                initiatingUid = live.uid
                initiatingEmail = live.email.orEmpty()
                if (reloadAfterStop) loadContent()
            }
        } else if (adminReady && reloadAfterStop) {
            if (!ownerCurrent()) {
                finish()
                return
            }
            loadContent()
        }
    }
    override fun onStop() {
        if (contentReady && ownerCurrent()) retainedDraft = getCurrentUiContent().takeUnless { sameFields(it, initialContent) }
        ++epoch
        ++readGeneration
        readJob?.cancel()
        loading = false
        contentReady = false
        reloadAfterStop = true
        reauthorizeOnResume = true
        afterSuccessfulSave = null
        unsavedDialog?.dismiss()
        unsavedDialog = null
        updateControls()
        super.onStop()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("content.type", currentType)
        val draft = if (contentReady && ownerCurrent()) getCurrentUiContent().takeUnless { sameFields(it, initialContent) } else retainedDraft
        draft?.let { outState.putBundle("content.draft", writeDraft(it)) }
        outState.putBundle("content.confirmed", writeDraft(initialContent))
        super.onSaveInstanceState(outState)
    }

    private fun restoreSelectedTab() {
        suppressTabSelection = true
        try { binding.tabLayout.getTabAt(TYPES.indexOf(currentType))?.select() }
        finally { suppressTabSelection = false }
    }
    private fun switchTab(type: String) {
        if (!current(epoch) || saving) return
        retainedDraft = null
        currentType = type
        restoreSelectedTab()
        loadContent()
    }
    private fun updateControls() {
        if (!::binding.isInitialized) return
        val editable = contentReady && !loading && !saving && ownerCurrent()
        binding.btnSave.isEnabled = editable
        for (field in listOf(binding.etTitle, binding.etBody, binding.etSupportEmail,
            binding.etPhone, binding.etWebsite, binding.etAddress)) field.isEnabled = editable
        binding.layoutContactFields.visibility = if (currentType == AppContentRepository.TYPE_CONTACT) View.VISIBLE else View.GONE
        binding.progressBar.visibility = if (loading || saving) View.VISIBLE else View.GONE
        binding.scrollViewContent.visibility = if (contentReady) View.VISIBLE else View.INVISIBLE
    }
    private fun loadContent() {
        if (!current(epoch)) return
        readJob?.cancel()
        val generation = ++readGeneration
        val requestEpoch = epoch
        val type = currentType
        reloadAfterStop = false
        loading = true
        contentReady = false
        updateControls()
        readJob = lifecycleScope.launch {
            try {
                // A new resumed read waits for any old in-flight write, without replaying it.
                // Only this new epoch owns the GET; the obsolete write never starts readback.
                saveJob?.takeIf { it.isActive }?.join()
                requireCurrent(requestEpoch)
                val content = repo.getConfirmedContent(type)
                if (!current(requestEpoch) || generation != readGeneration || currentType != type) return@launch
                initialContent = content
                populateUi(retainedDraft ?: content)
                retainedDraft = null
                contentReady = true
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) {
                if (current(requestEpoch) && generation == readGeneration) {
                    AppBulletin.showError(this@EditAboutActivity, "Couldn't load content: ${e.localizedMessage ?: "Unknown error"}")
                }
            } finally {
                if (generation == readGeneration) { loading = false; updateControls() }
            }
        }
    }

    private fun populateUi(content: AppContent) {
        binding.etTitle.setText(content.title)
        binding.etBody.setText(content.body)
        binding.etSupportEmail.setText(content.supportEmail)
        binding.etPhone.setText(content.phone)
        binding.etWebsite.setText(content.website)
        binding.etAddress.setText(content.address)
    }
    private fun getCurrentUiContent() = AppContent(
        title = binding.etTitle.text?.toString()?.trim().orEmpty(),
        body = binding.etBody.text?.toString()?.trim().orEmpty(),
        updatedAt = System.currentTimeMillis(), updatedBy = initiatingEmail,
        supportEmail = binding.etSupportEmail.text?.toString()?.trim().orEmpty(),
        phone = binding.etPhone.text?.toString()?.trim().orEmpty(),
        website = binding.etWebsite.text?.toString()?.trim().orEmpty(),
        address = binding.etAddress.text?.toString()?.trim().orEmpty())
    private fun sameFields(a: AppContent, b: AppContent): Boolean =
        a.title == b.title && a.body == b.body && a.supportEmail == b.supportEmail &&
            a.phone == b.phone && a.website == b.website && a.address == b.address
    private fun isDirty(): Boolean {
        val draft = if (contentReady) getCurrentUiContent() else retainedDraft ?: return false
        return !sameFields(draft, initialContent)
    }

    private fun saveCurrent(onSuccess: (() -> Unit)? = null) {
        if (!current(epoch) || !contentReady || loading) return
        if (saving) {
            // Back → Save may wait for this same operation; it cannot send a second PUT.
            if (saveEpoch == epoch && onSuccess != null) afterSuccessfulSave = onSuccess
            return
        }
        val draft = getCurrentUiContent()
        if (draft.title.isBlank()) { binding.tilTitle.error = "Title cannot be empty"; return }
        binding.tilTitle.error = null
        if (draft.body.isBlank()) { binding.tilBody.error = "Body content cannot be empty"; return }
        binding.tilBody.error = null
        val type = currentType
        val requestEpoch = epoch
        saveEpoch = requestEpoch
        saving = true
        afterSuccessfulSave = onSuccess
        ++readGeneration
        readJob?.cancel()
        updateControls()
        saveJob = lifecycleScope.launch {
            try {
                val permissions = AdminPermissionSnapshot.load(initiatingEmail, "manage_content",
                    membership = { adminRepo.isAdminFresh(initiatingUid, initiatingEmail) },
                    permissions = { requireCurrent(requestEpoch); adminRepo.getAdminMe() })
                requireCurrent(requestEpoch)
                if (permissions == null) throw SecurityException("Content editing permission is no longer available")
                repo.saveContent(type, draft)
                requireCurrent(requestEpoch)
                val confirmed = repo.getConfirmedContent(type)
                requireCurrent(requestEpoch)
                check(currentType == type && sameFields(confirmed, draft)) { "Save could not be confirmed by the server" }
                initialContent = confirmed
                retainedDraft = null
                AppBulletin.showSuccess(this@EditAboutActivity, "Saved successfully!")
                val action = afterSuccessfulSave
                afterSuccessfulSave = null
                // End ownership before a confirmed tab/back continuation starts its own read.
                saving = false
                action?.invoke()
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) {
                if (current(requestEpoch)) AppBulletin.showError(this@EditAboutActivity,
                    "Failed to save: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                saving = false
                afterSuccessfulSave = null
                if (current(requestEpoch)) updateControls()
            }
        }
    }

    private fun showUnsavedConfirmation(leaving: Boolean, proceed: () -> Unit) {
        if (!current(epoch) || unsavedDialog?.isShowing == true) return
        val requestEpoch = epoch
        val type = currentType
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage(if (leaving) "You have unsaved changes. Do you want to save before leaving?"
                else "You have unsaved changes. Do you want to save before switching?")
            .setPositiveButton("Save") { _, _ ->
                if (current(requestEpoch) && currentType == type) saveCurrent(proceed)
            }
            .setNegativeButton("Discard") { _, _ ->
                if (current(requestEpoch) && currentType == type && (leaving || !saving)) proceed()
            }
            .setNeutralButton("Cancel") { _, _ ->
                if (current(requestEpoch)) restoreSelectedTab()
            }.create()
        unsavedDialog = dialog
        dialog.setOnDismissListener { if (unsavedDialog === dialog) unsavedDialog = null }
        dialog.show()
    }
    private fun handleBack() {
        if (!current(epoch)) return
        if (isDirty()) showUnsavedConfirmation(leaving = true) { finish() } else if (!saving) finish()
    }

    private fun writeDraft(content: AppContent) = Bundle().apply {
        putString("title", content.title); putString("body", content.body)
        putString("supportEmail", content.supportEmail); putString("phone", content.phone)
        putString("website", content.website); putString("address", content.address)
    }
    private fun readDraft(state: Bundle) = AppContent(
        title = state.getString("title").orEmpty(), body = state.getString("body").orEmpty(),
        supportEmail = state.getString("supportEmail").orEmpty(), phone = state.getString("phone").orEmpty(),
        website = state.getString("website").orEmpty(), address = state.getString("address").orEmpty())
    companion object {
        private val TYPES = listOf(AppContentRepository.TYPE_PRIVACY, AppContentRepository.TYPE_TERMS, AppContentRepository.TYPE_CONTACT)
    }

    override fun onDestroy() {
        readJob?.cancel()
        readJob = null
        saveJob?.cancel()
        saveJob = null
        unsavedDialog?.dismiss()
        unsavedDialog = null
        super.onDestroy()
    }
}
