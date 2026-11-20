package com.eve.app.ui.admin

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.AppContent
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.AppContentRepository
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.admin.tg.TelegramSegmentedTabs
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Admin Edit Legal & Contact Content screen powered by real ported Telegram UI components:
 * ActionBar, TelegramSegmentedTabs, HeaderCell, EditTextBoldCursor, ShadowSectionCell,
 * ButtonWithCounterView, and EmptyTextProgressView.
 */
class EditAboutActivity : EveBaseActivity() {

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
    private var tabContentLoaded = false
    private var suppressTabSelection = false
    private var unsavedDialog: AlertDialog? = null

    private lateinit var rootView: LinearLayout
    private lateinit var tabLayout: TelegramSegmentedTabs
    private lateinit var scrollViewContent: ScrollView
    private lateinit var loadingView: EmptyTextProgressView
    private lateinit var etTitle: EditTextBoldCursor
    private lateinit var etBody: EditTextBoldCursor
    private lateinit var layoutContactFields: LinearLayout
    private lateinit var etSupportEmail: EditTextBoldCursor
    private lateinit var etPhone: EditTextBoldCursor
    private lateinit var etWebsite: EditTextBoldCursor
    private lateinit var etAddress: EditTextBoldCursor
    private lateinit var btnSave: ButtonWithCounterView

    private fun ownerCurrent(): Boolean {
        val live = FirebaseAuth.getInstance().currentUser
        return adminReady && !isFinishing && !isDestroyed && AdminIdentityGuard.allowed(
            true, initiatingUid, initiatingEmail, live?.uid, live?.email
        )
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
        tabContentLoaded = savedInstanceState?.getBoolean("content.tab_loaded", false) ?: false

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        val actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Edit About & Legal")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) handleBack()
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        tabLayout = TelegramSegmentedTabs(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTabs(listOf("Privacy", "Terms", "Contact Us"))
        }
        rootView.addView(tabLayout, matchWrap())

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        scrollViewContent = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.INVISIBLE
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }

        list.addView(HeaderCell(this).apply {
            setText("Content")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        val mainWrap = inputWrap()
        etTitle = styledEditText("Page title", singleLine = true)
        etBody = styledEditText("Page body content", singleLine = false)
        mainWrap.addView(etTitle, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f)))
        mainWrap.addView(
            etBody,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = AndroidUtilities.dp(8f)
            }
        )
        list.addView(mainWrap, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        layoutContactFields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        layoutContactFields.addView(HeaderCell(this).apply {
            setText("Contact Details")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
        val contactWrap = inputWrap()
        etSupportEmail = styledEditText("Support email", singleLine = true)
        etPhone = styledEditText("Phone", singleLine = true)
        etWebsite = styledEditText("Website", singleLine = true)
        etAddress = styledEditText("Address", singleLine = false)
        listOf(etSupportEmail, etPhone, etWebsite, etAddress).forEachIndexed { i, f ->
            contactWrap.addView(
                f,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (i > 0) topMargin = AndroidUtilities.dp(8f)
                }
            )
        }
        layoutContactFields.addView(contactWrap, matchWrap())
        layoutContactFields.addView(ShadowSectionCell(this), matchWrap())
        list.addView(layoutContactFields, matchWrap())

        val saveWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, padV)
        }
        btnSave = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Save Changes", false)
            setOnClickListener { saveCurrent() }
        }
        saveWrap.addView(btnSave, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48f)))
        list.addView(saveWrap, matchWrap())

        scrollViewContent.addView(list, matchWrap())
        contentFrame.addView(
            scrollViewContent,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        loadingView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            showProgress()
        }
        contentFrame.addView(
            loadingView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })
        updateControls()

        AdminAccessGate.requireAdmin(
            this,
            rootView,
            requireFreshServer = true,
            requiredRight = "manage_content",
            restorationScope = "edit_about",
            isRestoring = savedInstanceState != null
        ) {
            adminReady = true
            restoreSelectedTab()
            tabLayout.onTabSelectedListener = { position ->
                if (!suppressTabSelection) {
                    val target = TYPES.getOrNull(position)
                    if (target != null && target != currentType) {
                        if (!current(epoch) || saving || unsavedDialog?.isShowing == true) {
                            restoreSelectedTab()
                        } else if (isDirty()) {
                            showUnsavedConfirmation(leaving = false) { switchTab(target) }
                        } else {
                            switchTab(target)
                        }
                    }
                }
            }
            loadContent()
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != initiatingUid || live.email != initiatingEmail) {
                finish()
                return
            }
            contentReady = false
            updateControls()
            AdminAccessGate.requireAdmin(
                this,
                rootView,
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
        outState.putBoolean("content.tab_loaded", tabContentLoaded)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSelectedTab() {
        suppressTabSelection = true
        try {
            tabLayout.setTabs(listOf("Privacy", "Terms", "Contact Us"), TYPES.indexOf(currentType).coerceAtLeast(0))
        } finally {
            suppressTabSelection = false
        }
    }

    private fun switchTab(type: String) {
        if (!current(epoch) || saving) return
        retainedDraft = null
        tabContentLoaded = false
        currentType = type
        restoreSelectedTab()
        loadContent()
    }

    private fun updateControls() {
        if (!::btnSave.isInitialized) return
        val editable = contentReady && !loading && !saving && ownerCurrent()
        btnSave.isEnabled = editable
        btnSave.setLoading(saving)
        for (field in listOf(etTitle, etBody, etSupportEmail, etPhone, etWebsite, etAddress)) {
            field.isEnabled = editable
        }
        layoutContactFields.visibility = if (currentType == AppContentRepository.TYPE_CONTACT) View.VISIBLE else View.GONE
        loadingView.visibility = if (loading && !contentReady) View.VISIBLE else View.GONE
        scrollViewContent.visibility = if (contentReady) View.VISIBLE else View.INVISIBLE
    }

    private fun loadContent() {
        if (!current(epoch)) return
        readJob?.cancel()
        val generation = ++readGeneration
        val requestEpoch = epoch
        val type = currentType
        reloadAfterStop = false
        loading = true
        if (!tabContentLoaded) contentReady = false
        updateControls()
        readJob = lifecycleScope.launch {
            try {
                saveJob?.takeIf { it.isActive }?.join()
                requireCurrent(requestEpoch)
                val content = repo.getConfirmedContent(type)
                if (!current(requestEpoch) || generation != readGeneration || currentType != type) return@launch
                initialContent = content
                populateUi(retainedDraft ?: content)
                retainedDraft = null
                tabContentLoaded = true
                contentReady = true
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (current(requestEpoch) && generation == readGeneration) {
                    if (tabContentLoaded) contentReady = true
                    AppBulletin.showError(
                        this@EditAboutActivity,
                        "Couldn't load content: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } finally {
                if (generation == readGeneration) {
                    loading = false
                    updateControls()
                }
            }
        }
    }

    private fun populateUi(content: AppContent) {
        etTitle.setText(content.title)
        etBody.setText(content.body)
        etSupportEmail.setText(content.supportEmail)
        etPhone.setText(content.phone)
        etWebsite.setText(content.website)
        etAddress.setText(content.address)
    }

    private fun getCurrentUiContent() = AppContent(
        title = etTitle.text?.toString()?.trim().orEmpty(),
        body = etBody.text?.toString()?.trim().orEmpty(),
        updatedAt = System.currentTimeMillis(),
        updatedBy = initiatingEmail,
        supportEmail = etSupportEmail.text?.toString()?.trim().orEmpty(),
        phone = etPhone.text?.toString()?.trim().orEmpty(),
        website = etWebsite.text?.toString()?.trim().orEmpty(),
        address = etAddress.text?.toString()?.trim().orEmpty()
    )

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
            if (saveEpoch == epoch && onSuccess != null) afterSuccessfulSave = onSuccess
            return
        }
        val draft = getCurrentUiContent()
        if (draft.title.isBlank()) {
            etTitle.error = "Title cannot be empty"
            return
        }
        etTitle.error = null
        if (draft.body.isBlank()) {
            etBody.error = "Body content cannot be empty"
            return
        }
        etBody.error = null
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
                val permissions = AdminPermissionSnapshot.load(
                    initiatingEmail, "manage_content",
                    membership = { adminRepo.isAdminFresh(initiatingUid, initiatingEmail) },
                    permissions = { requireCurrent(requestEpoch); adminRepo.getAdminMe() }
                )
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
                saving = false
                action?.invoke()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (current(requestEpoch)) {
                    AppBulletin.showError(
                        this@EditAboutActivity,
                        "Failed to save: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
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
        val dialog = AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage(
                if (leaving) "You have unsaved changes. Do you want to save before leaving?"
                else "You have unsaved changes. Do you want to save before switching?"
            )
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
        website = state.getString("website").orEmpty(), address = state.getString("address").orEmpty()
    )

    companion object {
        private val TYPES = listOf(
            AppContentRepository.TYPE_PRIVACY,
            AppContentRepository.TYPE_TERMS,
            AppContentRepository.TYPE_CONTACT
        )
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

    private fun inputWrap() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        val padH = AndroidUtilities.dp(21f)
        val padV = AndroidUtilities.dp(10f)
        setPadding(padH, padV, padH, padV)
    }

    private fun styledEditText(hintText: String, singleLine: Boolean) = EditTextBoldCursor(this).apply {
        hint = hintText
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
        setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setCursorSize(AndroidUtilities.dp(20f))
        setCursorWidth(1.5f)
        isSingleLine = singleLine
        if (!singleLine) minLines = 5
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
