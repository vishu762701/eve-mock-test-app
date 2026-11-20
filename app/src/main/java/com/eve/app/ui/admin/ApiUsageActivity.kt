package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.ApiUsageRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminIdentityGuard
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
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper

/**
 * Admin API Usage screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextDetailSettingsCell, ShadowSectionCell,
 * TextInfoPrivacyCell, and EmptyTextProgressView.
 */
class ApiUsageActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
    }

    private val usageRepo = ApiUsageRepository()
    private var initiatingUid = ""
    private var initiatingEmail = ""
    private var adminReady = false
    private var readGeneration = 0L
    private var readJob: Job? = null
    private var loading = false
    private var hasLoadedData = false
    private var reloadAfterStop = true
    private var reauthorizeOnResume = false

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var scrollView: ScrollView
    private lateinit var loadingView: EmptyTextProgressView
    private lateinit var monthCell: TextDetailSettingsCell
    private lateinit var geminiCell: TextDetailSettingsCell
    private lateinit var docsCell: TextDetailSettingsCell
    private lateinit var submissionsCell: TextDetailSettingsCell

    private fun ownerCurrent(): Boolean {
        val live = FirebaseAuth.getInstance().currentUser
        return adminReady && !isFinishing && !isDestroyed && AdminIdentityGuard.allowed(
            true, initiatingUid, initiatingEmail, live?.uid, live?.email
        )
    }

    private fun canPublish(generation: Long): Boolean = generation == readGeneration &&
        ownerCurrent() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val owner = FirebaseAuth.getInstance().currentUser
        initiatingUid = owner?.uid.orEmpty()
        initiatingEmail = owner?.email.orEmpty()

        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("API Usage")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> loadUsage()
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

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }

        list.addView(HeaderCell(this).apply {
            setText("Billing Period")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        monthCell = TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue(getString(R.string.api_usage_loading), "Current month", false)
        }
        list.addView(monthCell, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        list.addView(HeaderCell(this).apply {
            setText("Monthly Counters")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        geminiCell = TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue(getString(R.string.api_usage_unknown_count), "Gemini AI Calls", true)
        }
        list.addView(geminiCell, matchWrap())

        docsCell = TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue(getString(R.string.api_usage_unknown_count), "Documents Created (Proxy)", true)
        }
        list.addView(docsCell, matchWrap())

        submissionsCell = TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue(getString(R.string.api_usage_unknown_count), "Test Submissions (Proxy)", false)
        }
        list.addView(submissionsCell, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        list.addView(TextInfoPrivacyCell(this).apply {
            setText("Usage counters are fetched live from the authenticated Worker endpoint.")
        }, matchWrap())

        scrollView.addView(list, matchWrap())
        contentFrame.addView(
            scrollView,
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

        showUnavailable(R.string.api_usage_loading)
        AdminAccessGate.requireAdmin(
            this,
            rootView,
            requireFreshServer = true,
            requiredRight = "view_analytics_logs",
            restorationScope = "api_usage",
            isRestoring = savedInstanceState != null
        ) {
            adminReady = true
            loadingView.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
            loadUsage()
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
            if (!hasLoadedData) {
                loadingView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
            }
            AdminAccessGate.requireAdmin(
                this,
                rootView,
                requireFreshServer = true,
                requiredRight = "view_analytics_logs"
            ) {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                loadingView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                initiatingUid = live.uid
                initiatingEmail = live.email.orEmpty()
                if (reloadAfterStop) loadUsage()
            }
        } else if (adminReady && reloadAfterStop) {
            if (!ownerCurrent()) {
                finish()
                return
            }
            loadUsage()
        }
    }

    override fun onStop() {
        ++readGeneration
        readJob?.cancel()
        loading = false
        reloadAfterStop = true
        reauthorizeOnResume = true
        if (::monthCell.isInitialized && !hasLoadedData) {
            showUnavailable(R.string.api_usage_loading)
        }
        super.onStop()
    }

    override fun onDestroy() {
        ++readGeneration
        readJob?.cancel()
        readJob = null
        super.onDestroy()
    }

    private fun showUnavailable(message: Int) {
        val unknown = getString(R.string.api_usage_unknown_count)
        monthCell.setTextAndValue(getString(message), "Current month", false)
        geminiCell.setTextAndValue(unknown, "Gemini AI Calls", true)
        docsCell.setTextAndValue(unknown, "Documents Created (Proxy)", true)
        submissionsCell.setTextAndValue(unknown, "Test Submissions (Proxy)", false)
    }

    private fun loadUsage() {
        if (loading || !ownerCurrent() || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val generation = ++readGeneration
        loading = true
        reloadAfterStop = false
        if (!hasLoadedData) {
            showUnavailable(R.string.api_usage_loading)
        }
        readJob = lifecycleScope.launch {
            try {
                // The authenticated Worker checks view_analytics_logs on every read.
                val usage = usageRepo.getUsageForCurrentMonth()
                if (!canPublish(generation)) return@launch
                hasLoadedData = true
                val countedNote = if (usage.countedSince.isNotBlank()) " • Since ${usage.countedSince.take(10)}" else ""
                monthCell.setTextAndValue("${usage.monthDisplayName}$countedNote", "Current month", false)
                actionBar.setSubtitle(usage.monthDisplayName)
                geminiCell.setTextAndValue(usage.geminiCalls.toString(), "Gemini AI Calls", true)
                docsCell.setTextAndValue(usage.documentsCreatedProxy.toString(), "Documents Created (Proxy)", true)
                submissionsCell.setTextAndValue(usage.testSubmissionsProxy.toString(), "Test Submissions (Proxy)", false)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (canPublish(generation)) {
                    hasLoadedData = false
                    showUnavailable(R.string.api_usage_unavailable)
                    AppBulletin.showError(
                        this@ApiUsageActivity,
                        getString(R.string.api_usage_load_failed, e.toUserFriendlyMessage())
                    )
                }
            } finally {
                if (generation == readGeneration) {
                    loading = false
                }
            }
        }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
