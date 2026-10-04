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
import kotlinx.coroutines.launch

class MistakesActivity : EveBaseActivity() {

    private lateinit var binding: ActivityMistakesBinding
    private lateinit var adapter: MistakesAdapter

    private var allMistakes: List<MistakeItem> = emptyList()
    private val learnedIds = mutableSetOf<String>()
    private var currentFilter: String = "ALL" // "ALL", "WRONG", "SKIPPED"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityMistakesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

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
        val prefs = getSharedPreferences("eve_mistakes", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("learned_ids", emptySet())
        if (saved != null) {
            learnedIds.clear()
            learnedIds.addAll(saved)
        }
    }

    private fun saveLearnedPreferences() {
        val prefs = getSharedPreferences("eve_mistakes", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("learned_ids", HashSet(learnedIds)).apply()
    }

    private fun toggleLearned(questionId: String) {
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
            currentFilter = when {
                checkedIds.contains(binding.chipWrong.id) -> "WRONG"
                checkedIds.contains(binding.chipSkipped.id) -> "SKIPPED"
                else -> "ALL"
            }
            applyFilter()
        }
    }

    private fun loadMistakes() {
        binding.progressBar.visibility = View.VISIBLE
        binding.messageGroup.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getMistakes()
                binding.progressBar.visibility = View.GONE
                if (resp.success && resp.data != null) {
                    allMistakes = resp.data
                } else {
                    allMistakes = emptyList()
                }
            } catch (_: Exception) {
                binding.progressBar.visibility = View.GONE
                allMistakes = emptyList()
            }
            updateFilterCounts()
            applyFilter()
        }
    }

    private fun updateFilterCounts() {
        val wrongCount = allMistakes.count { it.selected.isNotBlank() }
        val skippedCount = allMistakes.count { it.selected.isBlank() }
        binding.chipAll.text = "All (${allMistakes.size})"
        binding.chipWrong.text = "Wrong ($wrongCount)"
        binding.chipSkipped.text = "Skipped ($skippedCount)"
    }

    private fun applyFilter() {
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
}
