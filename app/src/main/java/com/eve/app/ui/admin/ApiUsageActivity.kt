package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.ApiUsageRepository
import com.eve.app.databinding.ActivityApiUsageBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AppBulletin
import com.eve.app.data.remote.toUserFriendlyMessage

class ApiUsageActivity : EveBaseActivity() {

    private lateinit var binding: ActivityApiUsageBinding
    private val usageRepo = ApiUsageRepository()
    private var initiatingUid = ""
    private var initiatingEmail = ""
    private var adminReady = false
    private var readGeneration = 0L
    private var readJob: Job? = null
    private var loading = false
    private var reloadAfterStop = true
    private var reauthorizeOnResume = false

    private fun ownerCurrent(): Boolean {
        val live = FirebaseAuth.getInstance().currentUser
        return adminReady && !isFinishing && !isDestroyed && AdminIdentityGuard.allowed(
            true, initiatingUid, initiatingEmail, live?.uid, live?.email)
    }

    private fun canPublish(generation: Long): Boolean = generation == readGeneration &&
        ownerCurrent() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val owner = FirebaseAuth.getInstance().currentUser
        initiatingUid = owner?.uid.orEmpty()
        initiatingEmail = owner?.email.orEmpty()
        binding = ActivityApiUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showUnavailable(R.string.api_usage_loading)
        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true,
            requiredRight = "view_analytics_logs", restorationScope = "api_usage",
            isRestoring = savedInstanceState != null) {
            adminReady = true
            setupUi()
            loadUsage()
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
            com.eve.app.util.AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "view_analytics_logs"
            ) {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
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
        if (::binding.isInitialized) {
            binding.btnRefresh.isEnabled = true
            showUnavailable(R.string.api_usage_loading)
        }
        super.onStop()
    }

    private fun showUnavailable(message: Int) {
        binding.tvCurrentMonth.setText(message)
        for (view in listOf(binding.tvGeminiCallsCount, binding.tvDocsCreatedCount, binding.tvSubmissionsCount)) {
            view.setText(R.string.api_usage_unknown_count)
        }
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadUsage() }
        binding.swipeRefresh.setOnRefreshListener { loadUsage() }
    }

    private fun loadUsage() {
        if (loading || !ownerCurrent() || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val generation = ++readGeneration
        loading = true
        reloadAfterStop = false
        binding.btnRefresh.isEnabled = false
        showUnavailable(R.string.api_usage_loading)
        readJob = lifecycleScope.launch {
            try {
                // The authenticated Worker checks view_analytics_logs on every read.
                val usage = usageRepo.getUsageForCurrentMonth()
                if (!canPublish(generation)) return@launch
                val countedNote = if (usage.countedSince.isNotBlank()) " • Since ${usage.countedSince.take(10)}" else ""
                binding.tvCurrentMonth.text = "📅 ${usage.monthDisplayName}$countedNote"
                binding.tvGeminiCallsCount.text = usage.geminiCalls.toString()
                binding.tvDocsCreatedCount.text = usage.documentsCreatedProxy.toString()
                binding.tvSubmissionsCount.text = usage.testSubmissionsProxy.toString()
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) {
                if (canPublish(generation)) {
                    showUnavailable(R.string.api_usage_unavailable)
                    AppBulletin.showError(this@ApiUsageActivity, getString(R.string.api_usage_load_failed, e.toUserFriendlyMessage()))
                }
            } finally {
                if (generation == readGeneration) {
                    loading = false
                    binding.btnRefresh.isEnabled = true
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }
}
