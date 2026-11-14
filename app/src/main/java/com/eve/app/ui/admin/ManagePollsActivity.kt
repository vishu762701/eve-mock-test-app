package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import com.eve.app.util.AppBulletin
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.util.AdminAccessGate
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Poll
import com.eve.app.data.repository.PollRepository
import com.eve.app.databinding.ActivityManagePollsBinding
import com.eve.app.databinding.DialogCreatePollBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Calendar

class ManagePollsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManagePollsBinding
    private val pollRepository = PollRepository()
    private lateinit var adapter: PollsAdminAdapter
    private var createPollDialog: TelegramNativeAlertDialog? = null
    private var customDatePickerDialog: DatePickerDialog? = null
    private var customTimePickerDialog: TimePickerDialog? = null
    private var reauthorizeOnResume = false
    private var authorizedUid: String? = null
    private var authorizedEmail: String? = null

    private val pollCreateLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadPolls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePollsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) { finish() }
        binding.progressBar.visibility = View.VISIBLE

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "moderate_feedback") {
            authorizedUid = FirebaseAuth.getInstance().currentUser?.uid
            authorizedEmail = FirebaseAuth.getInstance().currentUser?.email
            setupAdminControls()
            loadPolls()
        }
    }

    private fun setupAdminControls() {
        adapter = PollsAdminAdapter(
            onToggleStatus = { poll -> togglePollStatus(poll) },
            onDelete = { poll -> confirmDeletePoll(poll) }
        )

        binding.rvPolls.layoutManager = LinearLayoutManager(this)
        binding.rvPolls.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadPolls() }
        binding.btnCreatePollDialog.isEnabled = true
        com.eve.app.designsystem.DesignPress.bind(binding.btnCreatePollDialog) { showCreatePollDialog() }
    }

    override fun onStop() {
        createPollDialog?.dismiss()
        createPollDialog = null
        customDatePickerDialog?.dismiss()
        customDatePickerDialog = null
        customTimePickerDialog?.dismiss()
        customTimePickerDialog = null
        binding.btnCreatePollDialog.isEnabled = false
        reauthorizeOnResume = true
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding.emptyStateView.resetPlayOnNextShow()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
            if (!hasItems) {
                binding.progressBar.visibility = View.VISIBLE
                binding.swipeRefresh.visibility = View.GONE
            }
            AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "moderate_feedback") {
                authorizedUid = live.uid
                authorizedEmail = live.email
                binding.btnCreatePollDialog.isEnabled = true
                loadPolls(showProgress = false)
            }
        }
    }

    private fun loadPolls(showProgress: Boolean = true) {
        val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
        if (showProgress || !hasItems) {
            binding.progressBar.visibility = View.VISIBLE
            if (!hasItems) binding.swipeRefresh.visibility = View.GONE
        }
        binding.emptyStateView.hide()
        binding.errorStateView.hide()

        lifecycleScope.launch {
            try {
                val list = pollRepository.getPolls()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false

                if (list.isEmpty()) {
                    adapter.submitList(emptyList())
                    binding.emptyStateView.show(
                        title = "No polls created yet",
                        message = "Tap '+ New Poll' to create your first community poll."
                    )
                } else {
                    binding.emptyStateView.hide()
                    adapter.submitList(list)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                if (hasItems) {
                    binding.swipeRefresh.visibility = View.VISIBLE
                    com.eve.app.util.AppBulletin.showError(this@ManagePollsActivity, "Failed to refresh polls: ${e.localizedMessage.orEmpty()}")
                } else {
                    binding.swipeRefresh.visibility = View.GONE
                    binding.errorStateView.show(
                        type = com.eve.app.ui.common.ErrorStateView.ErrorType.SERVER_ERROR,
                        customMessage = "Failed to load polls. ${e.localizedMessage.orEmpty()}",
                        onRetry = { loadPolls() }
                    )
                }
            }
        }
    }

    private fun togglePollStatus(poll: Poll) {
        val newStatus = !poll.active
        lifecycleScope.launch {
            try {
                pollRepository.updatePollStatus(poll.id, newStatus)
                AppBulletin.showSuccess(
                    this@ManagePollsActivity,
                    if (newStatus) "Poll reopened" else "Poll closed"
                )
                loadPolls()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                AppBulletin.showError(this@ManagePollsActivity, "Failed to update poll: ${e.message}")
            }
        }
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDeletePoll(poll: Poll) {
        deleteFlow.confirmRemoval("poll:${poll.id}", "Poll and its votes", { pollRepository.deletePoll(poll.id); null }) { loadPolls() }
    }

    private fun showCreatePollDialog() {
        if (isFinishing || isDestroyed) return
        val intent = android.content.Intent(this, com.eve.app.ui.poll.PollCreateActivity::class.java)
        pollCreateLauncher.launch(intent)
    }

    override fun onDestroy() {
        createPollDialog?.dismiss()
        createPollDialog = null
        customDatePickerDialog?.dismiss()
        customDatePickerDialog = null
        customTimePickerDialog?.dismiss()
        customTimePickerDialog = null
        super.onDestroy()
    }

    private fun pickCustomDateTime(onDateTimeSelected: (Long) -> Unit) {
        if (isFinishing || isDestroyed) return
        val cal = Calendar.getInstance()
        customDatePickerDialog?.dismiss()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                if (isFinishing || isDestroyed) return@DatePickerDialog
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                customTimePickerDialog?.dismiss()
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        onDateTimeSelected(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                )
                timePicker.setOnDismissListener {
                    if (customTimePickerDialog === timePicker) customTimePickerDialog = null
                }
                customTimePickerDialog = timePicker
                timePicker.show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setOnDismissListener {
            if (customDatePickerDialog === datePicker) customDatePickerDialog = null
        }
        customDatePickerDialog = datePicker
        datePicker.show()
    }
}
