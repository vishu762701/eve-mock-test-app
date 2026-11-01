package com.eve.app.ui.home

import android.content.Intent
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.util.Constants
import com.eve.app.ui.profile.ProfileActivity
import com.eve.app.ui.performance.PerformanceActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.syllabus.SyllabusActivity
import com.eve.app.ui.practice.PracticeActivity
import com.eve.app.ui.pyq.PyqActivity
import com.eve.app.ui.settings.SettingsActivity

/** Shared production action wiring, also exercised by native touch tests. */
object HomeDrawerActions {
    fun bind(binding: ActivityMainBinding, navigateFromDrawer: (Intent) -> Unit) {
        val context = binding.root.context
        binding.ivDrawerAvatar.setOnClickListener {
            navigateFromDrawer(Intent(context, ProfileActivity::class.java))
        }
        (binding.ivDrawerAvatar.parent as android.view.View).apply {
            isFocusable = true
            contentDescription = "Open profile"
            setOnClickListener { navigateFromDrawer(Intent(context, ProfileActivity::class.java)) }
        }
        binding.layoutDrawerProfile.setOnClickListener {
            navigateFromDrawer(Intent(context, ProfileActivity::class.java))
        }
        binding.layoutDrawerPremium.setOnClickListener {
            navigateFromDrawer(Intent(context, com.eve.app.ui.premium.PremiumActivity::class.java))
        }
        binding.layoutDrawerPerformance.setOnClickListener {
            navigateFromDrawer(Intent(context, PerformanceActivity::class.java))
        }
        binding.layoutDrawerLeaderboard.setOnClickListener {
            navigateFromDrawer(Intent(context, LeaderboardActivity::class.java).apply {
                putExtra(Constants.EXTRA_EXAM_ID, "overall")
                putExtra(Constants.EXTRA_EXAM_NAME, "Overall Leaderboard")
            })
        }
        binding.layoutDrawerSyllabus.setOnClickListener {
            navigateFromDrawer(Intent(context, SyllabusActivity::class.java))
        }
        binding.layoutDrawerTopic.setOnClickListener {
            navigateFromDrawer(Intent(context, PracticeActivity::class.java))
        }
        binding.layoutDrawerPyq.setOnClickListener {
            navigateFromDrawer(Intent(context, PyqActivity::class.java))
        }
        binding.layoutDrawerMistakes.setOnClickListener {
            navigateFromDrawer(Intent(context, com.eve.app.ui.mistakes.MistakesActivity::class.java))
        }
        binding.layoutDrawerSettings.setOnClickListener {
            navigateFromDrawer(Intent(context, SettingsActivity::class.java))
        }
    }
}
