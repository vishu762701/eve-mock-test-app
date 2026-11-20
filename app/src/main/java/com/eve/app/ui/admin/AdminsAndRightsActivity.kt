package com.eve.app.ui.admin

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.data.remote.toUserFriendlyMessage
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Admins and Rights screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, EditTextBoldCursor, ButtonWithCounterView, RecyclerListView,
 * ShadowSectionCell, TextInfoPrivacyCell, and EmptyTextProgressView.
 */
class AdminsAndRightsActivity : EveBaseActivity() {

    private val viewModel: AdminViewModel by viewModels()
    private var authorized: Boolean = false
    private var authorizedUid: String = ""
    private var authorizedEmail: String = ""
    private var reauthorizeOnResume: Boolean = false
    private var removeAdminDialog: AlertDialog? = null

    private lateinit var rootView: LinearLayout
    private lateinit var scrollContent: ScrollView
    private lateinit var loadingView: EmptyTextProgressView
    private lateinit var etAdminEmail: EditTextBoldCursor
    private lateinit var rvAdmins: RecyclerListView
    private lateinit var emptyAdminsInfo: TextInfoPrivacyCell

    private val adminEmailAdapter = AdminEmailAdapter(
        onItemClick = { admin ->
            startActivity(AdminRightsEditActivity.createIntent(this, admin, canEdit = true))
        },
        onRemove = { admin -> confirmRemoveAdmin(admin) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Theme.syncFromEvePalette(this)

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
            setTitle("Admins and Rights")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        scrollContent = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }

        // Add Admin section
        list.addView(HeaderCell(this).apply {
            setText("Add Administrator")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        val addSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(21f)
            val padV = AndroidUtilities.dp(10f)
            setPadding(padH, padV, padH, AndroidUtilities.dp(16f))
        }
        etAdminEmail = EditTextBoldCursor(this).apply {
            hint = "Admin email address"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
            setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setCursorSize(AndroidUtilities.dp(20f))
            setCursorWidth(1.5f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            isSingleLine = true
        }
        addSection.addView(
            etAdminEmail,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f))
        )

        val btnAddAdmin = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Add Admin", false)
            setOnClickListener { addAdmin() }
        }
        addSection.addView(
            btnAddAdmin,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f)).apply {
                topMargin = AndroidUtilities.dp(12f)
            }
        )
        list.addView(addSection, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // Current Admins section
        list.addView(HeaderCell(this).apply {
            setText("Current Administrators")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        rvAdmins = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminsAndRightsActivity)
            adapter = adminEmailAdapter
            isNestedScrollingEnabled = false
        }
        list.addView(rvAdmins, matchWrap())

        emptyAdminsInfo = TextInfoPrivacyCell(this).apply {
            setText("No dynamic administrators configured yet.")
            visibility = View.GONE
        }
        list.addView(emptyAdminsInfo, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())
        list.addView(TextInfoPrivacyCell(this).apply {
            setText("Tap an administrator to configure granular RBAC rights.")
        }, matchWrap())

        scrollContent.addView(list, matchWrap())
        contentFrame.addView(
            scrollContent,
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

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_admins_rights") {
            val user = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            authorizedUid = user.uid
            authorizedEmail = user.email.orEmpty()
            authorized = true
            loadingView.visibility = View.GONE
            scrollContent.visibility = View.VISIBLE
            setupObservers()
            viewModel.loadAdmins()
        }
    }

    override fun onStop() {
        removeAdminDialog?.dismiss()
        removeAdminDialog = null
        reauthorizeOnResume = true
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            AdminAccessGate.requireAdmin(
                this,
                rootView,
                requireFreshServer = true,
                requiredRight = "manage_admins_rights"
            ) {
                if (!authorized) {
                    finish()
                    return@requireAdmin
                }
                authorizedUid = live.uid
                authorizedEmail = live.email.orEmpty()
                loadingView.visibility = View.GONE
                scrollContent.visibility = View.VISIBLE
                if (authorized) viewModel.loadAdmins()
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.admins.collect { adminList ->
                    if (adminList.isEmpty()) {
                        emptyAdminsInfo.visibility = View.VISIBLE
                        adminEmailAdapter.submit(emptyList())
                    } else {
                        emptyAdminsInfo.visibility = View.GONE
                        adminEmailAdapter.submit(adminList)
                    }
                }
            }
        }
    }

    private fun addAdmin() {
        val email = etAdminEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            AppBulletin.showError(this, "Please enter a valid email address")
            return
        }
        viewModel.addAdmin(email) {
            etAdminEmail.text?.clear()
            AppBulletin.showSuccess(this, "Admin added successfully! Tap the admin row to configure permissions.")
        }
    }

    private fun confirmRemoveAdmin(admin: DynamicAdminItem) {
        val email = admin.email
        removeAdminDialog?.dismiss()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Remove Admin?")
            .setMessage("Remove $email? This will revoke admin privileges and cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeAdmin(email) { result ->
                    result.onSuccess {
                        AppBulletin.showSuccess(this, "Admin removed")
                    }.onFailure {
                        AppBulletin.showError(this, it.toUserFriendlyMessage())
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE)
            .show()
        removeAdminDialog = dialog
        dialog.setOnDismissListener { if (removeAdminDialog === dialog) removeAdminDialog = null }
    }

    override fun onDestroy() {
        removeAdminDialog?.dismiss()
        removeAdminDialog = null
        super.onDestroy()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
