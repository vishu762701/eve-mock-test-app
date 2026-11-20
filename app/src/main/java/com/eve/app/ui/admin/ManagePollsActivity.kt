package com.eve.app.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Poll
import com.eve.app.data.repository.PollRepository
import com.eve.app.databinding.DialogCreatePollBinding
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import java.util.Calendar

/**
 * Admin Manage Polls screen powered by real ported Telegram UI components:
 * ActionBar (with refresh), RecyclerListView, EmptyTextProgressView, and ButtonWithCounterView,
 * while preserving PollsAdminAdapter, AdminDeleteFlow, poll creation dialog, and RBAC.
 */
class ManagePollsActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
    }

    private val pollRepository = PollRepository()
    private lateinit var adapter: PollsAdminAdapter
    private var createPollDialog: AlertDialog? = null
    private var customDatePickerDialog: DatePickerDialog? = null
    private var customTimePickerDialog: TimePickerDialog? = null
    private var reauthorizeOnResume = false
    private var authorizedUid: String? = null
    private var authorizedEmail: String? = null

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView
    private lateinit var btnCreatePoll: ButtonWithCounterView

    private val pollCreateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadPolls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            setTitle("Manage Polls")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> if (::adapter.isInitialized) loadPolls()
                    }
                }
            })
        }
        rootView.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@ManagePollsActivity)
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
            visibility = View.GONE
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("No polls created yet\nTap '+ New Poll' to create your first community poll.")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        retryButton = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Retry", false)
            visibility = View.GONE
            setOnClickListener { loadPolls() }
        }
        contentFrame.addView(
            retryButton,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                48f,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                24f,
                0f,
                24f,
                24f
            )
        )

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(10f)
            setPadding(padH, padV, padH, padV)
        }
        btnCreatePoll = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("+ New Poll", false)
            isEnabled = false
            setOnClickListener { showCreatePollDialog() }
        }
        bottomBar.addView(
            btnCreatePoll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48f))
        )
        rootView.addView(
            bottomBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "moderate_feedback") {
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
        listView.adapter = adapter
        btnCreatePoll.isEnabled = true
    }

    override fun onStop() {
        createPollDialog?.dismiss()
        createPollDialog = null
        customDatePickerDialog?.dismiss()
        customDatePickerDialog = null
        customTimePickerDialog?.dismiss()
        customTimePickerDialog = null
        if (::btnCreatePoll.isInitialized) {
            btnCreatePoll.isEnabled = false
        }
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
            val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
            if (!hasItems) {
                listView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.showProgress()
            }
            AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "moderate_feedback") {
                authorizedUid = live.uid
                authorizedEmail = live.email
                btnCreatePoll.isEnabled = true
                loadPolls(showProgress = false)
            }
        }
    }

    private fun loadPolls(showProgress: Boolean = true) {
        val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
        retryButton.visibility = View.GONE
        if (showProgress && !hasItems) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }

        lifecycleScope.launch {
            try {
                val list = pollRepository.getPolls()
                if (list.isEmpty()) {
                    adapter.submitList(emptyList())
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No polls created yet\nTap '+ New Poll' to create your first community poll.")
                    emptyView.showTextView()
                    actionBar.setSubtitle(null)
                } else {
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                    adapter.submitList(list)
                    actionBar.setSubtitle("${list.size} ${if (list.size == 1) "poll" else "polls"}")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (hasItems) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    AppBulletin.showError(
                        this@ManagePollsActivity,
                        "Failed to refresh polls: ${e.localizedMessage.orEmpty()}"
                    )
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("Failed to load polls\n${e.localizedMessage.orEmpty()}")
                    emptyView.showTextView()
                    retryButton.visibility = View.VISIBLE
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
                loadPolls(showProgress = false)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                AppBulletin.showError(this@ManagePollsActivity, "Failed to update poll: ${e.message}")
            }
        }
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDeletePoll(poll: Poll) {
        deleteFlow.confirmRemoval("poll:${poll.id}", "Poll and its votes", { pollRepository.deletePoll(poll.id); null }) {
            loadPolls(showProgress = false)
        }
    }

    private fun showCreatePollDialog() {
        if (isFinishing || isDestroyed || createPollDialog?.isShowing == true) return
        val dialogBinding = DialogCreatePollBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val optionViews = mutableListOf<View>()
        var calculatedEndsAt = 0L

        fun addOptionRow(hintText: String = "Option text"): View {
            val row = layoutInflater.inflate(R.layout.item_poll_option_input, dialogBinding.optionsContainer, false)
            val et = row.findViewById<EditText>(R.id.etOptionText)
            val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveOption)
            et.hint = hintText

            btnRemove.setOnClickListener {
                if (optionViews.size > 2) {
                    dialogBinding.optionsContainer.removeView(row)
                    optionViews.remove(row)
                    dialogBinding.btnAddOptionRow.isEnabled = true
                } else {
                    AppBulletin.showError(this, "A poll must have at least 2 options")
                }
            }

            dialogBinding.optionsContainer.addView(row)
            optionViews.add(row)

            if (optionViews.size >= 6) {
                dialogBinding.btnAddOptionRow.isEnabled = false
            }
            return row
        }

        addOptionRow("Option 1")
        addOptionRow("Option 2")

        dialogBinding.btnAddOptionRow.setOnClickListener {
            if (optionViews.size < 6) {
                addOptionRow("Option ${optionViews.size + 1}")
            }
        }

        val durations = listOf("No Expiry", "1 Day", "3 Days", "7 Days", "Custom Date & Time")
        dialogBinding.spDuration.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            durations
        )

        dialogBinding.spDuration.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val now = System.currentTimeMillis()
                when (position) {
                    0 -> calculatedEndsAt = 0L
                    1 -> calculatedEndsAt = now + 1L * 24 * 60 * 60 * 1000
                    2 -> calculatedEndsAt = now + 3L * 24 * 60 * 60 * 1000
                    3 -> calculatedEndsAt = now + 7L * 24 * 60 * 60 * 1000
                    4 -> pickCustomDateTime { selectedTime ->
                        calculatedEndsAt = selectedTime
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSubmitPoll.setOnClickListener {
            val question = dialogBinding.etQuestion.text.toString().trim()
            if (question.isBlank()) {
                dialogBinding.etQuestion.error = "Question is required"
                return@setOnClickListener
            }

            val options = optionViews.map { row ->
                row.findViewById<EditText>(R.id.etOptionText).text.toString().trim()
            }.filter { it.isNotBlank() }

            if (options.size < 2) {
                AppBulletin.showError(this, "Please provide at least 2 non-empty options")
                return@setOnClickListener
            }

            val active = dialogBinding.switchActive.isChecked
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "admin"

            lifecycleScope.launch {
                try {
                    dialogBinding.btnSubmitPoll.isEnabled = false
                    pollRepository.createPoll(
                        question = question,
                        options = options,
                        endsAt = calculatedEndsAt,
                        active = active,
                        createdBy = userEmail
                    )
                    AppBulletin.showSuccess(this@ManagePollsActivity, "Poll created successfully")
                    dialog.dismiss()
                    loadPolls(showProgress = false)
                } catch (e: Exception) {
                    dialogBinding.btnSubmitPoll.isEnabled = true
                    AppBulletin.showError(this@ManagePollsActivity, "Failed to create poll: ${e.message}")
                }
            }
        }

        dialog.setOnDismissListener {
            if (createPollDialog === dialog) createPollDialog = null
        }
        dialog.show()
        createPollDialog = dialog
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
