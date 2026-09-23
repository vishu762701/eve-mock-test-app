package com.eve.app.ui.result

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.AnswerItem
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.ui.home.MainActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.util.Constants
import com.eve.app.util.LanguageManager
import com.eve.app.util.SecurityHelper

class ResultActivity : AppCompatActivity() {

    private lateinit var allItems: List<AnswerItem>
    private val adapter = AnswerAdapter()
    private var fromHistory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityHelper.applyScreenProtection(this)
        val binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        allItems =
            IntentCompat.getParcelableArrayListExtra(intent, Constants.EXTRA_ANSWERS, AnswerItem::class.java)
                ?: emptyList()

        // Test History (Phase 10): yeh screen ek purane attempt ka review bhi ho sakti hai
        fromHistory = intent.getBooleanExtra(Constants.EXTRA_FROM_HISTORY, false)
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME)
        val attemptDate = intent.getStringExtra(Constants.EXTRA_ATTEMPT_DATE)
        if (fromHistory && !examName.isNullOrBlank()) {
            binding.tvResultSubtitle.visibility = android.view.View.VISIBLE
            binding.tvResultSubtitle.text = if (!attemptDate.isNullOrBlank()) {
                "$examName  •  $attemptDate"
            } else {
                examName
            }
            binding.btnHome.text = "Close"
        }

        val total = allItems.size
        val correct = allItems.count { it.isCorrect }
        val unattempted = allItems.count { !it.isAttempted }
        val wrong = total - correct - unattempted
        val score = correct - wrong * Constants.NEGATIVE_MARK

        val scoreText = if (score % 1.0 == 0.0) score.toInt().toString() else String.format("%.2f", score)
        binding.tvScore.text = "$scoreText / $total"
        binding.tvStats.text = "Correct: $correct   Wrong: $wrong   Unattempted: $unattempted"

        binding.chipAll.text = "All ($total)"
        binding.chipCorrect.text = "Correct ($correct)"
        binding.chipWrong.text = "Wrong ($wrong)"
        binding.chipNotAttempted.text = "Not Attempted ($unattempted)"

        binding.rvAnswers.layoutManager = LinearLayoutManager(this)
        binding.rvAnswers.adapter = adapter
        adapter.submit(allItems)
        adapter.setHindi(LanguageManager.isHindi(this))

        LanguageManager.setupToggleButton(this, binding.btnLanguage) { hindi ->
            adapter.setHindi(hindi)
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtered = when (checkedIds.firstOrNull()) {
                binding.chipCorrect.id -> allItems.filter { it.isCorrect }
                binding.chipWrong.id -> allItems.filter { it.isAttempted && !it.isCorrect }
                binding.chipNotAttempted.id -> allItems.filter { !it.isAttempted }
                else -> allItems
            }
            adapter.submit(filtered)
            binding.rvAnswers.scrollToPosition(0)
        }

        // Phase 16: exam ka examId ho (fresh submit ya history review, dono me milta hai) to
        // Leaderboard button dikhao — is exam ke top scorers + apna rank dekhne ke liye.
        val examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID)
        if (!examId.isNullOrBlank()) {
            binding.btnLeaderboard.visibility = android.view.View.VISIBLE
            binding.btnLeaderboard.setOnClickListener {
                startActivity(
                    Intent(this, LeaderboardActivity::class.java)
                        .putExtra(Constants.EXTRA_EXAM_ID, examId)
                        .putExtra(Constants.EXTRA_EXAM_NAME, examName)
                )
            }
        }

        binding.btnHome.setOnClickListener { close() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        close()
    }

    /** History se review khola tha to bas finish karo (History list par wapas), warna Home pe jao. */
    private fun close() {
        if (fromHistory) {
            finish()
            return
        }
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
