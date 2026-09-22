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
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import com.eve.app.util.isHardcodedAdmin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
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

    private fun render(state: UiState<List<com.eve.app.data.model.Exam>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.tvMessage.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                adapter.submit(state.data)
                binding.tvMessage.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                binding.tvMessage.text = "Abhi koi exam available nahi hai"
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = state.message
            }
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
