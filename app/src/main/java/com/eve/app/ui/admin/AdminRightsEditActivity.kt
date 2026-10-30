/*
 * Derived from Telegram Android ChatRightsEditActivity.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.eve.app.R
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.databinding.ActivityAdminRightsEditBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.admin.tg.TextCheckCell
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.AvatarDrawable

class AdminRightsEditActivity : EveBaseActivity() {

    companion object {
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

    private lateinit var binding: ActivityAdminRightsEditBinding
    private val viewModel: AdminViewModel by viewModels()

    private var targetEmail: String = ""
    private var isOwner: Boolean = false
    private var canEdit: Boolean = true

    private var initialRights = setOf<String>()
    private var initialCustomTitle: String = ""

    private val rightCells = mutableMapOf<String, TextCheckCell>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminRightsEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetEmail = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        isOwner = intent.getBooleanExtra(EXTRA_IS_OWNER, false)
        canEdit = intent.getBooleanExtra(EXTRA_CAN_EDIT, true)

        val rightsList = intent.getStringArrayListExtra(EXTRA_RIGHTS) ?: arrayListOf()
        initialRights = rightsList.toSet()
        initialCustomTitle = intent.getStringExtra(EXTRA_CUSTOM_TITLE) ?: (if (isOwner) "Owner" else "Admin")

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            setupActionBar()
            setupIdentityHeader()
            setupCustomTitleSection()
            setupRightsSection()
            setupDismissSection()
            setupBackPressHandling()
        }
    }

    private fun setupActionBar() {
        val title = if (isOwner) "Owner Permissions" else "Edit Admin"
        binding.telegramActionBar.setTitle(title)
        binding.telegramActionBar.onBackPressed = { handleBackOrDiscard() }

        if (canEdit && !isOwner) {
            binding.telegramActionBar.showDoneAction { saveChanges() }
        }
    }

    private fun setupIdentityHeader() {
        binding.tvEmail.text = targetEmail
        binding.tvRoleSubtitle.text = if (isOwner) "Owner" else "Administrator"

        val initials = AvatarDrawable.extractInitials(targetEmail)
        val colorIndex = AvatarDrawable.getColorIndex(targetEmail)
        binding.ivAvatar.setImageDrawable(AvatarDrawable(this, initials, colorIndex))
    }

    private fun setupCustomTitleSection() {
        binding.headerCustomTitle.setText("CUSTOM TITLE")
        binding.etCustomTitle.setText(initialCustomTitle)
        binding.privacyCustomTitle.setText("A custom title will be shown next to this admin's name.")

        if (isOwner || !canEdit) {
            binding.etCustomTitle.isEnabled = false
            binding.etCustomTitle.isFocusable = false
        }
    }

    private fun setupRightsSection() {
        binding.headerRights.setText("WHAT CAN THIS ADMIN DO?")

        setupCheck(binding.checkManageExams, "manage_exams", "Manage Exams", "Create, edit, rename, and manage exams")
        setupCheck(binding.checkManageQuestions, "manage_questions", "Manage Questions", "Add, edit, batch import, and delete questions")
        setupCheck(binding.checkManageGeneratedTests, "manage_generated_tests", "Manage Generated Tests", "Trigger AI tests, update status and schedule")
        setupCheck(binding.checkManageSyllabus, "manage_syllabus", "Manage Syllabus", "Update and remove exam syllabus")
        setupCheck(binding.checkManageUsers, "manage_users", "Manage Users", "View user attempts, stats, ban and unban accounts")
        setupCheck(binding.checkManageAdminsRights, "manage_admins_rights", "Manage Admins & Rights", "Add administrators and edit their permissions")
        setupCheck(binding.checkManagePremium, "manage_premium", "Manage Premium", "Grant, extend, revoke premium, configure plans")
        setupCheck(binding.checkSendBroadcasts, "send_broadcasts", "Send Broadcasts", "Publish and delete broadcast notifications")
        setupCheck(binding.checkModerateFeedback, "moderate_feedback", "Moderate Feedback", "Reply to feedback messages, manage posts and polls")
        setupCheck(binding.checkManageConfigMaintenance, "manage_config_maintenance", "App Config & Maintenance", "Configure app settings and maintenance mode")
        setupCheck(binding.checkManageContent, "manage_content", "Manage Content", "Manage home banners and floating community links")
        setupCheck(binding.checkViewAnalyticsLogs, "view_analytics_logs", "View Analytics & Logs", "Inspect exam analytics, API usage, and system monitor")
        setupCheck(binding.checkExportData, "export_data", "Export Data", "Export question banks and exam records", needDivider = false)

        val privacyText = if (isOwner) {
            "Built-in owner permissions and role cannot be modified."
        } else {
            "Administrators will only have access to features and endpoints enabled above."
        }
        binding.privacyRights.setText(privacyText)
    }

    private fun setupCheck(
        cell: TextCheckCell,
        permission: String,
        title: String,
        subtitle: String,
        needDivider: Boolean = true
    ) {
        val checked = isOwner || initialRights.contains(permission)
        cell.setTextAndCheck(title, checked, needDivider)
        cell.setSubtitle(subtitle)

        if (isOwner || !canEdit) {
            cell.isEnabled = false
            cell.checkBox.isEnabled = false
            cell.setOnClickListener(null)
        }

        rightCells[permission] = cell
    }

    private fun setupDismissSection() {
        if (isOwner || !canEdit) {
            binding.shadowDismiss.visibility = View.GONE
            binding.btnDismissAdmin.visibility = View.GONE
            binding.privacyDismiss.visibility = View.GONE
            return
        }

        binding.shadowDismiss.visibility = View.VISIBLE
        binding.btnDismissAdmin.visibility = View.VISIBLE
        binding.privacyDismiss.visibility = View.VISIBLE

        binding.btnDismissAdmin.setTextAndIcon("Dismiss Admin", R.drawable.ic_delete, needDivider = false)
        binding.btnDismissAdmin.makeRed()
        binding.btnDismissAdmin.setOnClickListener { confirmDismissAdmin() }

        binding.privacyDismiss.setText("Revoke administrator privileges and remove from admin list.")
    }

    private fun confirmDismissAdmin() {
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Dismiss Admin?")
            .setMessage("Are you sure you want to dismiss $targetEmail? This will revoke all administrative privileges.")
            .setPositiveButton("Dismiss") { _, _ ->
                viewModel.removeAdmin(targetEmail)
                AppBulletin.showSuccess(this, "Administrator dismissed")
                finish()
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE)
            .show()
    }

    private fun getCurrentSelectedRights(): List<String> {
        val list = mutableListOf<String>()
        for ((permission, cell) in rightCells) {
            if (cell.isChecked) {
                list.add(permission)
            }
        }
        return list
    }

    private fun hasUnsavedChanges(): Boolean {
        if (isOwner || !canEdit) return false
        val currentRights = getCurrentSelectedRights().toSet()
        val currentTitle = binding.etCustomTitle.text?.toString()?.trim().orEmpty()
        return currentRights != initialRights || currentTitle != initialCustomTitle
    }

    private fun handleBackOrDiscard() {
        if (hasUnsavedChanges()) {
            TelegramNativeAlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Do you want to discard them?")
                .setPositiveButton("Discard") { _, _ -> finish() }
                .setNegativeButton("Keep Editing", null)
                .makeRed(DialogInterface.BUTTON_POSITIVE)
                .show()
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
        val selectedRights = getCurrentSelectedRights()
        val customTitle = binding.etCustomTitle.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

        viewModel.updateAdminRights(targetEmail, selectedRights, customTitle) {
            AppBulletin.showSuccess(this, "Admin permissions updated successfully")
            finish()
        }
    }
}
