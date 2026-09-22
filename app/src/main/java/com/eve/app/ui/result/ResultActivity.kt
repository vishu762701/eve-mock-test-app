package com.eve.app.ui.result

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.AnswerItem
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.ui.home.MainActivity
import com.eve.app.util.Constants

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val items: List<AnswerItem> =
            IntentCompat.getParcelableArrayListExtra(intent, Constants.EXTRA_ANSWERS, AnswerItem::class.java)
                ?: emptyList()

        val total = items.size
        val correct = items.count { it.isCorrect }
        val unattempted = items.count { !it.isAttempted }
        val wrong = total - correct - unattempted
        val score = correct - wrong * Constants.NEGATIVE_MARK

        val scoreText = if (score % 1.0 == 0.0) score.toInt().toString() else String.format("%.2f", score)
        binding.tvScore.text = "$scoreText / $total"
        binding.tvStats.text = "Correct: $correct   Wrong: $wrong   Unattempted: $unattempted"

        binding.rvAnswers.layoutManager = LinearLayoutManager(this)
        binding.rvAnswers.adapter = AnswerAdapter(items)

        binding.btnHome.setOnClickListener { goHome() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
