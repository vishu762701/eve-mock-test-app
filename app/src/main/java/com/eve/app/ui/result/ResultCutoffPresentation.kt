package com.eve.app.ui.result

import android.view.View
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.databinding.ActivityResultBinding

object ResultCutoffPresentation {
    fun bind(binding: ActivityResultBinding, category: String, score: Double, cutoffs: Map<String, Double>) {
        // Update Hero Card
        binding.tvCutoffSelectedCategory.text = category
        val cutoff = cutoffs[category]
        val scoreStr = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(java.util.Locale.US, "%.2f", score)
        binding.tvCutoffScore.text = "Score: $scoreStr"

        val configured = cutoff != null && cutoff >= 0
        binding.tvCutoffVerdict.visibility = if (configured) View.VISIBLE else View.GONE
        binding.tvCutoffScore.visibility = if (configured) View.VISIBLE else View.GONE
        if (configured) {
            checkNotNull(cutoff)
            val cutoffStr = if (cutoff % 1.0 == 0.0) cutoff.toInt().toString() else cutoff.toString()
            binding.tvCutoffValue.text = "Cutoff: $cutoffStr"
            if (score >= cutoff) {
                binding.tvCutoffVerdict.text = "Qualified ✓"
                binding.tvCutoffVerdict.setBackgroundResource(R.drawable.bg_tile_right)
                binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(binding.root.context, R.color.eve_tile_right_text))
                val diff = score - cutoff
                binding.tvCutoffRelationship.text = if (diff >= 0.05) {
                    "You cleared the $category cutoff mark by +${String.format(java.util.Locale.US, "%.1f", diff)} marks."
                } else {
                    "You achieved the exact qualifying score for $category."
                }
            } else {
                binding.tvCutoffVerdict.text = "Not Qualified ✗"
                binding.tvCutoffVerdict.setBackgroundResource(R.drawable.bg_tile_wrong)
                binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(binding.root.context, R.color.eve_tile_wrong_text))
                val diff = cutoff - score
                binding.tvCutoffRelationship.text = "You are ${String.format(java.util.Locale.US, "%.1f", diff)} marks below the $category cutoff threshold."
            }
        } else {
            binding.tvCutoffValue.text = "Cutoff unavailable"
            binding.tvCutoffVerdict.text = "No Cutoff Set"
            binding.tvCutoffVerdict.setBackgroundResource(R.drawable.bg_tile_surface2)
            binding.tvCutoffVerdict.setTextColor(ContextCompat.getColor(binding.root.context, R.color.eve_text_secondary))
            binding.tvCutoffRelationship.text = "No cutoff is configured for $category. Qualification cannot be determined."
        }
    }
}
