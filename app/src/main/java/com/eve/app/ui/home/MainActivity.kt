package com.eve.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.admin.AdminActivity
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.NetworkUtil
import com.eve.app.util.ThemeManager
import com.eve.app.util.UiState
import com.eve.app.util.isHardcodedAdmin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val adminRepo = AdminRepository()

    private val adapter = ExamAdapter { exam ->
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                .putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                .putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Hi, ${user.displayName ?: "Student"}"

        ThemeManager.setupToggleButton(this, binding.btnThemeToggle)

        // Hardcoded admin ho to turant dikhao (fast path, koi network wait nahi)
        if (isHardcodedAdmin(user.email)) {
            showAdminButton()
        } else {
            // Firestore me dynamically add kiya gaya admin ho to bhi check karo
            lifecycleScope.launch {
                if (adminRepo.isAdmin(user.email)) showAdminButton()
            }
        }

        binding.btnLogout.setOnClickListener { logout() }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter

        binding.btnRetry.setOnClickListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    NetworkUtil.observe(this@MainActivity).collect { online ->
                        binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Admin ne naya exam add kiya ho to list refresh ho jaye
        if (::binding.isInitialized) viewModel.load()
    }

    private fun showAdminButton() {
        binding.btnAdmin.visibility = View.VISIBLE
        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    private fun render(state: UiState<HomeUiData>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressGroup.visibility = View.VISIBLE
                binding.messageGroup.visibility = View.GONE
                binding.chipGroupCategory.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                val data = state.data
                renderChips(data.categories, data.selectedCategory)
                adapter.submit(data.items)
                val empty = data.items.isEmpty()
                binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
                if (empty) {
                    binding.ivMessageIcon.setImageResource(com.eve.app.R.drawable.ic_state_empty)
                    if (data.categories.size <= 1) {
                        binding.tvMessage.text = "Abhi koi exam available nahi hai"
                        binding.tvMessageSub.text = "Admin ke naya exam add karte hi yahan dikhega"
                    } else {
                        binding.tvMessage.text = "Is category me abhi koi exam nahi hai"
                        binding.tvMessageSub.text = "Koi aur category try karo ya \"All\" par wapas jao"
                    }
                }
                binding.chipGroupCategory.visibility = if (data.categories.size <= 1) View.GONE else View.VISIBLE
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setImageResource(com.eve.app.R.drawable.ic_state_error)
                if (NetworkUtil.isOnline(this)) {
                    binding.tvMessage.text = "Kuch gadbad ho gayi"
                    binding.tvMessageSub.text = state.message
                } else {
                    binding.tvMessage.text = "No internet connection"
                    binding.tvMessageSub.text =
                        "Exams abhi tak cache nahi hue. Network wapas aane par retry karo."
                }
                binding.chipGroupCategory.visibility = View.GONE
            }
        }
    }

    private fun renderChips(categories: List<String>, selected: String) {
        // Ek hi jaisi list dobara build na ho isliye simple guard
        if (binding.chipGroupCategory.childCount == categories.size) {
            val same = (0 until binding.chipGroupCategory.childCount).all { i ->
                (binding.chipGroupCategory.getChildAt(i) as? Chip)?.text?.toString() == categories[i]
            }
            if (same) {
                (0 until binding.chipGroupCategory.childCount).forEach { i ->
                    val chip = binding.chipGroupCategory.getChildAt(i) as Chip
                    chip.isChecked = chip.text.toString() == selected
                }
                return
            }
        }
        binding.chipGroupCategory.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = category == selected
                setOnClickListener { viewModel.selectCategory(category) }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener { goToLogin() }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
