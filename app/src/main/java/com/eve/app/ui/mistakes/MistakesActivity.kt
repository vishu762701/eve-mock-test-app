package com.eve.app.ui.mistakes

import com.eve.app.ui.common.EveBaseActivity

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.MistakeItem
import com.eve.app.databinding.ActivityMistakesBinding
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MistakesActivity : EveBaseActivity() {

    private lateinit var binding: ActivityMistakesBinding
    private lateinit var adapter: MistakesAdapter

    private var allMistakes: List<MistakeItem> = emptyList()
    private val learnedIds = mutableSetOf<String>()
    private var currentFilter: String = "ALL" // "ALL", "WRONG", "SKIPPED"
    private val mistakesAuth = FirebaseAuth.getInstance()
    private var mistakesOwnerUid: String? = null
    private var mistakeLoadJob: Job? = null
    private var mistakeLoadFailed = false

    private fun hasMistakesOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(mistakesOwnerUid, mistakesAuth.currentUser?.uid)
    private val mistakesAccountListener = FirebaseAuth.AuthStateListener {
        if (mistakesOwnerUid != null && !hasMistakesOwner()) {
            mistakeLoadJob?.cancel()
            learnedIds.clear()
            allMistakes = emptyList()
            if (::binding.isInitialized) binding.rvMistakes.visibility = View.GONE
            if (::adapter.isInitialized) adapter.submitList(emptyList())
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = mistakesAuth.currentUser?.uid
        mistakesOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("mistakes_owner_uid")
        if (!hasMistakesOwner()) { finish(); return }
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityMistakesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.messageGroup.setOnClickListener {
            if (mistakeLoadFailed && hasMistakesOwner()) loadMistakes()
        }

        loadLearnedPreferences()

        adapter = MistakesAdapter(
            onToggleLearned = { item ->
                toggleLearned(item.questionId)
            },
            isLearned = { id ->
                learnedIds.contains(id)
            },
            isHindi = {
                LanguageManager.isHindi(this)
            }
        )

        binding.rvMistakes.layoutManager = LinearLayoutManager(this)
        binding.rvMistakes.adapter = adapter

        setupFilters()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    NetworkUtil.observe(this@MistakesActivity).collect { online ->
                        binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }

        loadMistakes()
    }

    private fun loadLearnedPreferences() {
        if (!hasMistakesOwner()) { finish(); return }
        val prefs = getSharedPreferences(
            MistakesPreferenceScope.namespaceForUser(requireNotNull(mistakesOwnerUid)),
            Context.MODE_PRIVATE
        )
        val saved = prefs.getStringSet("learned_ids", emptySet())
        if (saved != null) {
            learnedIds.clear()
            learnedIds.addAll(saved)
        }
    }

    private fun saveLearnedPreferences() {
        if (!hasMistakesOwner()) { finish(); return }
        val prefs = getSharedPreferences(
            MistakesPreferenceScope.namespaceForUser(requireNotNull(mistakesOwnerUid)),
            Context.MODE_PRIVATE
        )
        prefs.edit().putStringSet("learned_ids", HashSet(learnedIds)).apply()
    }

    private fun toggleLearned(questionId: String) {
        if (!hasMistakesOwner()) { finish(); return }
        if (questionId.isBlank()) return
        if (learnedIds.contains(questionId)) {
            learnedIds.remove(questionId)
        } else {
            learnedIds.add(questionId)
        }
        saveLearnedPreferences()
        adapter.notifyDataSetChanged()
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!hasMistakesOwner()) { finish(); return@setOnCheckedStateChangeListener }
            currentFilter = when {
                checkedIds.contains(binding.chipWrong.id) -> "WRONG"
                checkedIds.contains(binding.chipSkipped.id) -> "SKIPPED"
                else -> "ALL"
            }
            applyFilter()
        }
    }

    private fun loadMistakes() {
        if (!hasMistakesOwner()) { finish(); return }
        mistakeLoadJob?.cancel()
        val requestUid = requireNotNull(mistakesOwnerUid)
        mistakeLoadFailed = false
        binding.progressBar.visibility = View.VISIBLE
        binding.messageGroup.visibility = View.GONE
        binding.messageGroup.isClickable = false
        binding.messageGroup.isFocusable = false
        binding.messageGroup.contentDescription = null

        mistakeLoadJob = lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getMistakes()
                if (!hasMistakesOwner() || requestUid != mistakesOwnerUid) {
                    finish()
                    return@launch
                }
                if (!resp.success || resp.data == null) {
                    throw IllegalStateException(resp.error ?: "Server mistakes response is incomplete")
                }
                allMistakes = resp.data
                binding.progressBar.visibility = View.GONE
                updateFilterCounts()
                applyFilter()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (!hasMistakesOwner() || requestUid != mistakesOwnerUid) {
                    finish()
                    return@launch
                }
                showMistakeReadFailure()
            }
        }
    }

    private fun showMistakeReadFailure() {
        mistakeLoadFailed = true
        binding.progressBar.visibility = View.GONE
        binding.rvMistakes.visibility = View.GONE
        binding.messageGroup.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = "Couldn't load mistakes"
        binding.tvEmptySubtitle.text = "Check your connection and tap to retry."
        binding.messageGroup.isClickable = true
        binding.messageGroup.isFocusable = true
        binding.messageGroup.contentDescription = "Retry loading Mistake Notebook"
    }

    private fun updateFilterCounts() {
        val wrongCount = allMistakes.count { it.selected.isNotBlank() }
        val skippedCount = allMistakes.count { it.selected.isBlank() }
        binding.chipAll.text = "All (${allMistakes.size})"
        binding.chipWrong.text = "Wrong ($wrongCount)"
        binding.chipSkipped.text = "Skipped ($skippedCount)"
    }

    override fun onStart() {
        super.onStart()
        if (!hasMistakesOwner()) { finish(); return }
        mistakesAuth.addAuthStateListener(mistakesAccountListener)
    }

    override fun onStop() {
        mistakesAuth.removeAuthStateListener(mistakesAccountListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasMistakesOwner()) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("mistakes_owner_uid", mistakesOwnerUid)
    }

    private fun applyFilter() {
        if (!hasMistakesOwner()) { finish(); return }
        if (mistakeLoadFailed) return
        val filtered = when (currentFilter) {
            "WRONG" -> allMistakes.filter { it.selected.isNotBlank() }
            "SKIPPED" -> allMistakes.filter { it.selected.isBlank() }
            else -> allMistakes
        }

        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.messageGroup.visibility = View.VISIBLE
            binding.rvMistakes.visibility = View.GONE
            if (allMistakes.isEmpty()) {
                binding.tvEmptyTitle.text = "All Caught Up!"
                binding.tvEmptySubtitle.text = "No mistakes recorded. Questions you answer incorrectly or skip will automatically appear here."
            } else {
                binding.tvEmptyTitle.text = "No Matching Questions"
                binding.tvEmptySubtitle.text = "There are no questions in this category."
            }
        } else {
            binding.messageGroup.visibility = View.GONE
            binding.rvMistakes.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mistakeLoadJob?.cancel()
    }
}
