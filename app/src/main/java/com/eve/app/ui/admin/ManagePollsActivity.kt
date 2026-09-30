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
import com.eve.app.util.AppUndoBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Poll
import com.eve.app.data.repository.PollRepository
import com.eve.app.databinding.ActivityManagePollsBinding
import com.eve.app.databinding.DialogCreatePollBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class ManagePollsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManagePollsBinding
    private val pollRepository = PollRepository()
    private lateinit var adapter: PollsAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePollsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = PollsAdminAdapter(
            onToggleStatus = { poll -> togglePollStatus(poll) },
            onDelete = { poll -> confirmDeletePoll(poll) }
        )

        binding.rvPolls.layoutManager = LinearLayoutManager(this)
        binding.rvPolls.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadPolls() }
        binding.btnCreatePollDialog.setOnClickListener { showCreatePollDialog() }

        loadPolls()
    }

    private fun loadPolls() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateView.hide()
        binding.errorStateView.hide()

        lifecycleScope.launch {
            try {
                val list = pollRepository.getPolls()
                binding.progressBar.visibility = View.GONE
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
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.errorStateView.show(
                    type = com.eve.app.ui.common.ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = "Failed to load polls. ${e.localizedMessage.orEmpty()}",
                    onRetry = { loadPolls() }
                )
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
            } catch (e: Exception) {
                AppBulletin.showError(this@ManagePollsActivity, "Failed to update poll: ${e.message}")
            }
        }
    }

    private fun confirmDeletePoll(poll: Poll) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Poll?")
            .setMessage("Are you sure you want to permanently delete this poll and all its votes?")
            .setPositiveButton("Delete") { _, _ ->
                val originalList = adapter.getItems().toMutableList()
                val filtered = originalList.filter { it.id != poll.id }
                adapter.submitList(filtered)
                if (filtered.isEmpty()) {
                    binding.emptyStateView.show(
                        title = "No polls created yet",
                        message = "Tap '+ New Poll' to create your first community poll."
                    )
                } else {
                    binding.emptyStateView.hide()
                }

                AppUndoBar.show(
                    context = this@ManagePollsActivity,
                    message = "Poll deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        adapter.submitList(originalList)
                        binding.emptyStateView.hide()
                        AppBulletin.show(this@ManagePollsActivity, "Delete cancelled")
                    },
                    onExecuteDelete = {
                        lifecycleScope.launch {
                            try {
                                pollRepository.deletePoll(poll.id)
                                loadPolls()
                            } catch (e: Exception) {
                                adapter.submitList(originalList)
                                binding.emptyStateView.hide()
                                AppBulletin.showError(this@ManagePollsActivity, "Failed to delete: ${e.message}")
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreatePollDialog() {
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

        // Add 2 initial options
        addOptionRow("Option 1")
        addOptionRow("Option 2")

        dialogBinding.btnAddOptionRow.setOnClickListener {
            if (optionViews.size < 6) {
                addOptionRow("Option ${optionViews.size + 1}")
            }
        }

        // Duration options
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
                    loadPolls()
                } catch (e: Exception) {
                    dialogBinding.btnSubmitPoll.isEnabled = true
                    AppBulletin.showError(this@ManagePollsActivity, "Failed to create poll: ${e.message}")
                }
            }
        }

        dialog.show()
    }

    private fun pickCustomDateTime(onDateTimeSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
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
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
