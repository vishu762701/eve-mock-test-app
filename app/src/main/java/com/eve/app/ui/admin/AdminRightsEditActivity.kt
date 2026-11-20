/*
 * Derived from Telegram Android ChatRightsEditActivity.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.AdminRepository
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.IconBackgroundColors
import org.telegram.ui.Components.LayoutHelper

/**
 * Admin Rights Edit screen powered by real ported Telegram UI components:
 * ActionBar (with Done checkmark), HeaderCell, TextDetailSettingsCell, EditTextBoldCursor,
 * org.telegram.ui.Cells.TextCheckCell, TextCell, ShadowSectionCell, TextInfoPrivacyCell,
 * and EmptyTextProgressView.
 */
class AdminRightsEditActivity : EveBaseActivity() {

    companion object {
        private const val MENU_DONE = 1
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_IS_OWNER = "extra_is_owner"
        const val EXTRA_RIGHTS = "extra_rights"
        const val EXTRA_CUSTOM_TITLE = "extra_custom_title"
        const val EXTRA_CAN_EDIT = "extra_can_edit"

        fun createIntent(
            context: Context,
            admin: DynamicAdminItem,
            canEdit: Boolean = true
        ): Intent {
            return Intent(context, AdminRightsEditActivity::class.java).apply {
                putExtra(EXTRA_EMAIL, admin.email)
                putExtra(EXTRA_IS_OWNER, admin.isOwner)
                putStringArrayListExtra(EXTRA_RIGHTS, ArrayList(admin.rights))
                putExtra(EXTRA_CUSTOM_TITLE, admin.customTitle)
                putExtra(EXTRA_CAN_EDIT, canEdit)
            }
        }
    }

    private val viewModel: AdminViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private var activeUid = ""
    private var activeEmail = ""
    private var gateReady = false
    private var mutationPending = false
    private var actionEpoch = 0L
    private var reauthorizeOnResume = false
    private var activeDialog: AlertDialog? = null

    private var targetEmail: String = ""
    private var isOwner: Boolean = false
    private var canEdit: Boolean = true
    private var initialRights = setOf<String>()
    private var initialCustomTitle: String = ""

    private lateinit var rootView: LinearLayout
    private lateinit var scrollRightsEdit: ScrollView
    private lateinit var listContainer: LinearLayout
    private lateinit var loadingView: EmptyTextProgressView
    private lateinit var etCustomTitle: EditTextBoldCursor
    private val rightCells = mutableMapOf<String, TextCheckCell>()

    private fun ownerCurrent(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return gateReady && !isFinishing && !isDestroyed &&
            AdminIdentityGuard.allowed(true, activeUid, activeEmail, user?.uid, user?.email)
    }

    private fun canAct(): Boolean =
        ownerCurrent() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private suspend fun requireFreshRightsPermission() {
        if (!canAct()) throw CancellationException("Admin rights owner changed")
        val rights = AdminPermissionSnapshot.load(
            activeEmail, "manage_admins_rights",
            membership = { adminRepo.isAdminFresh(activeUid, activeEmail) },
            permissions = { adminRepo.getAdminMe() }
        )
        if (!canAct()) throw CancellationException("Admin rights owner changed")
        if (rights == null) throw SecurityException("Admin rights permission is unavailable")
    }

    private fun runAuthorizedMutation(
        operation: ((Result<Unit>) -> Unit) -> Unit,
        success: () -> Unit
    ) {
        if (!canAct() || mutationPending || viewModel.busy.value) return
        mutationPending = true
        val epoch = ++actionEpoch
        lifecycleScope.launch {
            var dispatched = false
            try {
                requireFreshRightsPermission()
                if (!canAct() || epoch != actionEpoch) return@launch
                if (viewModel.busy.value) throw IllegalStateException("Another admin change is still pending")
                dispatched = true
                operation { result ->
                    if (epoch == actionEpoch) mutationPending = false
                    if (epoch == actionEpoch && canAct()) {
                        result.onSuccess { success() }
                            .onFailure { AppBulletin.showError(this@AdminRightsEditActivity, it.toUserFriendlyMessage()) }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (canAct() && epoch == actionEpoch) {
                    AppBulletin.showError(this@AdminRightsEditActivity, e.toUserFriendlyMessage())
                }
            } finally {
                if (!dispatched && epoch == actionEpoch) mutationPending = false
            }
        }
    }

    override fun onStop() {
        activeDialog?.dismiss()
        activeDialog = null
        ++actionEpoch
        mutationPending = false
        reauthorizeOnResume = true
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (gateReady && !ownerCurrent()) {
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != activeUid || live.email != activeEmail) {
                finish()
                return
            }
            if (!gateReady) {
                loadingView.visibility = View.VISIBLE
                scrollRightsEdit.visibility = View.GONE
            }
            AdminAccessGate.requireAdmin(
                this,
                rootView,
                requireFreshServer = true,
                requiredRight = "manage_admins_rights"
            ) {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                loadingView.visibility = View.GONE
                scrollRightsEdit.visibility = View.VISIBLE
                activeUid = live.uid
                activeEmail = live.email.orEmpty()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetEmail = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        isOwner = intent.getBooleanExtra(EXTRA_IS_OWNER, false)
        canEdit = intent.getBooleanExtra(EXTRA_CAN_EDIT, true)
        val rightsList = intent.getStringArrayListExtra(EXTRA_RIGHTS) ?: arrayListOf()
        initialRights = rightsList.toSet()
        initialCustomTitle = intent.getStringExtra(EXTRA_CUSTOM_TITLE) ?: (if (isOwner) "Owner" else "Admin")

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
            setTitle(if (isOwner) "Owner Permissions" else "Edit Admin")
            if (canEdit && !isOwner) {
                createMenu().addItem(MENU_DONE, R.drawable.ic_ab_done)
            }
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> handleBackOrDiscard()
                        MENU_DONE -> saveChanges()
                    }
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        scrollRightsEdit = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollRightsEdit.addView(listContainer, matchWrap())
        contentFrame.addView(
            scrollRightsEdit,
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

        setupBackPressHandling()

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_admins_rights") {
            val principal = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            activeUid = principal.uid
            activeEmail = principal.email.orEmpty()
            gateReady = true
            if (!ownerCurrent()) return@requireAdmin
            loadingView.visibility = View.GONE
            scrollRightsEdit.visibility = View.VISIBLE
            buildSections()
        }
    }

    private fun buildSections() {
        listContainer.removeAllViews()
        rightCells.clear()

        // 1. Identity Header
        addHeader("Administrator")
        listContainer.addView(TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue(targetEmail, if (isOwner) "Owner" else "Administrator", false)
        }, matchWrap())
        addShadow()

        // 2. Custom Title
        addHeader("CUSTOM TITLE")
        val titleWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(21f)
            val padV = AndroidUtilities.dp(8f)
            setPadding(padH, padV, padH, padV)
        }
        etCustomTitle = EditTextBoldCursor(this).apply {
            hint = "Custom title"
            setText(initialCustomTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
            setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setCursorSize(AndroidUtilities.dp(20f))
            setCursorWidth(1.5f)
            isSingleLine = true
            if (isOwner || !canEdit) {
                isEnabled = false
                isFocusable = false
            }
        }
        titleWrap.addView(
            etCustomTitle,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f))
        )
        listContainer.addView(titleWrap, matchWrap())
        addInfo("A custom title will be shown next to this admin's name.")

        // 3. Rights Toggles
        addHeader("WHAT CAN THIS ADMIN DO?")
        val rightsSpec = listOf(
            Triple("manage_exams", "Manage Exams", "Create, edit, rename, and manage exams"),
            Triple("manage_questions", "Manage Questions", "Add, edit, batch import, and delete questions"),
            Triple("manage_generated_tests", "Manage Generated Tests", "Trigger AI tests, update status and schedule"),
            Triple("manage_syllabus", "Manage Syllabus", "Update and remove exam syllabus"),
            Triple("manage_users", "Manage Users", "View user attempts, stats, ban and unban accounts"),
            Triple("manage_admins_rights", "Manage Admins & Rights", "Add administrators and edit their permissions"),
            Triple("manage_premium", "Manage Premium", "Grant, extend, revoke premium, configure plans"),
            Triple("send_broadcasts", "Send Broadcasts", "Publish and delete broadcast notifications"),
            Triple("moderate_feedback", "Moderate Feedback", "Reply to feedback messages, manage posts and polls"),
            Triple("manage_config_maintenance", "App Config & Maintenance", "Configure app settings and maintenance mode"),
            Triple("manage_content", "Manage Content", "Manage home banners and floating community links"),
            Triple("view_analytics_logs", "View Analytics & Logs", "Inspect exam analytics, API usage, and system monitor"),
            Triple("export_data", "Export Data", "Export question banks and exam records")
        )
        rightsSpec.forEachIndexed { idx, (perm, title, sub) ->
            val cell = TextCheckCell(this).apply {
                setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                foreground = Theme.getSelectorDrawable(false)
                val checked = isOwner || initialRights.contains(perm)
                setTextAndValueAndCheck(title, sub, checked, true, idx < rightsSpec.lastIndex)
                if (isOwner || !canEdit) {
                    setEnabled(false, null)
                } else {
                    setOnClickListener { setChecked(!isChecked) }
                }
            }
            rightCells[perm] = cell
            listContainer.addView(cell, matchWrap())
        }
        addInfo(
            if (isOwner) "Built-in owner permissions and role cannot be modified."
            else "Administrators will only have access to features and endpoints enabled above."
        )

        // 4. Dismiss Section
        if (!isOwner && canEdit) {
            val dismissCell = TextCell(this).apply {
                setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                foreground = Theme.getSelectorDrawable(false)
                setTextAndColorfulIcon("Dismiss Admin", R.drawable.ic_delete, IconBackgroundColors.RED.bottom, false)
                setTextColor(Theme.getColor(Theme.key_text_RedRegular))
                setOnClickListener { confirmDismissAdmin() }
            }
            listContainer.addView(dismissCell, matchWrap())
            addInfo("Revoke administrator privileges and remove from admin list.")
        }
    }

    private fun confirmDismissAdmin() {
        if (!canAct() || mutationPending || !canEdit || isOwner || targetEmail.isBlank()) return
        activeDialog?.dismiss()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Dismiss Admin?")
            .setMessage("Are you sure you want to dismiss $targetEmail? This will revoke all administrative privileges.")
            .setPositiveButton("Dismiss") { _, _ ->
                if (canAct() && !mutationPending) {
                    runAuthorizedMutation(
                        operation = { callback -> viewModel.removeAdmin(targetEmail, callback) },
                        success = {
                            AppBulletin.showSuccess(this, "Administrator dismissed")
                            finish()
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE)
            .show()
        activeDialog = dialog
        dialog.setOnDismissListener { if (activeDialog === dialog) activeDialog = null }
    }

    private fun getCurrentSelectedRights(): List<String> =
        rightCells.entries.filter { it.value.isChecked }.map { it.key }

    private fun hasUnsavedChanges(): Boolean {
        if (isOwner || !canEdit || !::etCustomTitle.isInitialized) return false
        val currentRights = getCurrentSelectedRights().toSet()
        val currentTitle = etCustomTitle.text?.toString()?.trim().orEmpty()
        return currentRights != initialRights || currentTitle != initialCustomTitle
    }

    private fun handleBackOrDiscard() {
        if (mutationPending) return
        if (hasUnsavedChanges()) {
            activeDialog?.dismiss()
            val dialog = AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Do you want to discard them?")
                .setPositiveButton("Discard") { _, _ -> finish() }
                .setNegativeButton("Keep Editing", null)
                .makeRed(DialogInterface.BUTTON_POSITIVE)
                .show()
            activeDialog = dialog
            dialog.setOnDismissListener { if (activeDialog === dialog) activeDialog = null }
        } else {
            finish()
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackOrDiscard()
            }
        })
    }

    private fun saveChanges() {
        if (!canAct() || mutationPending || isOwner || !canEdit || targetEmail.isBlank() || !::etCustomTitle.isInitialized) return
        val selectedRights = getCurrentSelectedRights()
        val customTitle = etCustomTitle.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        runAuthorizedMutation(
            operation = { callback ->
                viewModel.updateAdminRights(targetEmail, selectedRights, customTitle, callback)
            },
            success = {
                AppBulletin.showSuccess(this, "Admin permissions updated successfully")
                finish()
            }
        )
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
        activeDialog = null
        super.onDestroy()
    }

    private fun addHeader(title: String) {
        listContainer.addView(HeaderCell(this).apply {
            setText(title)
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
    }

    private fun addShadow() {
        listContainer.addView(ShadowSectionCell(this), matchWrap())
    }

    private fun addInfo(text: String) {
        listContainer.addView(TextInfoPrivacyCell(this).apply {
            setText(text)
        }, matchWrap())
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
