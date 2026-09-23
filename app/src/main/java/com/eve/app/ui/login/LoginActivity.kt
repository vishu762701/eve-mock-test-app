package com.eve.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.databinding.ActivityLoginBinding
import com.eve.app.ui.home.MainActivity
import com.eve.app.util.AnalyticsHelper
import com.eve.app.util.CrashlyticsHelper
import com.eve.app.util.ThemeManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var isSignUpMode = false

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) {
                    showError("Google token not received")
                } else {
                    firebaseLoginWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                showError("Sign-in failed (code ${e.statusCode}). Please try again.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Session check: existing authenticated users remain logged in
        if (auth.currentUser != null) {
            goHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Play tasteful entrance animation
        playEntranceAnimation()

        // Toggle between Sign In and Sign Up mode
        binding.btnToggleMode.setOnClickListener {
            toggleMode()
        }

        // Email / Password submission
        binding.btnSubmit.setOnClickListener {
            handleEmailPasswordAuth()
        }

        // Google Sign-In
        binding.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, gso)
            client.signOut().addOnCompleteListener {
                signInLauncher.launch(client.signInIntent)
            }
        }
    }

    private fun playEntranceAnimation() {
        binding.cardLogin.alpha = 0f
        binding.cardLogin.translationY = 48f
        binding.cardLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    private fun toggleMode() {
        isSignUpMode = !isSignUpMode
        binding.tvError.visibility = View.GONE

        if (isSignUpMode) {
            binding.tilName.visibility = View.VISIBLE
            binding.btnSubmit.text = "Create Account"
            binding.btnToggleMode.text = "Already have an account? Sign In"
        } else {
            binding.tilName.visibility = View.GONE
            binding.btnSubmit.text = "Sign In"
            binding.btnToggleMode.text = "Don't have an account? Sign Up"
        }
    }

    private fun handleEmailPasswordAuth() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
        val name = binding.etName.text?.toString()?.trim().orEmpty()

        if (isSignUpMode && name.isEmpty()) {
            showError("Please enter your name.")
            binding.etName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            showError("Please enter your email address.")
            binding.etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.")
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("Please enter your password.")
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters.")
            binding.etPassword.requestFocus()
            return
        }

        binding.tvError.visibility = View.GONE
        setLoading(true)

        lifecycleScope.launch {
            try {
                if (isSignUpMode) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null && name.isNotEmpty()) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        user.updateProfile(profileUpdates).await()
                    }
                    onAuthSuccess(user)
                } else {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    onAuthSuccess(result.user)
                }
            } catch (e: Exception) {
                setLoading(false)
                handleAuthError(e)
            }
        }
    }

    private fun firebaseLoginWithGoogle(idToken: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                onAuthSuccess(result.user)
            } catch (e: Exception) {
                setLoading(false)
                handleAuthError(e)
            }
        }
    }

    private suspend fun onAuthSuccess(user: FirebaseUser?) {
        if (user != null) {
            recordUserStats(user)
            AnalyticsHelper.logLogin(this@LoginActivity)
            CrashlyticsHelper.identify(user.uid, isAdmin = false)
        }
        goHome()
    }

    private fun handleAuthError(e: Exception) {
        val friendlyMessage = when (e) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
        showError(friendlyMessage)
    }

    /**
     * Admin Dashboard ke "total signups" aur "online abhi" counters isi doc par based hain.
     * `createdAt` sirf pehli baar (signup) set hoti hai — dobara login par overwrite nahi hoti,
     * taaki purana signup date sahi rahe. `lastActive` har login par update hoti hai (aur
     * MainActivity.onResume mein bhi, jab tak app foreground mein rehta hai).
     */
    private suspend fun recordUserStats(user: FirebaseUser) {
        try {
            val ref = FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
            val existing = ref.get().await()
            val data = hashMapOf<String, Any>(
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "lastActive" to System.currentTimeMillis()
            )
            if (!existing.exists()) {
                data["createdAt"] = System.currentTimeMillis()
            }
            ref.set(data, SetOptions.merge()).await()
        } catch (_: Exception) {
            // Stats optional hain — inke fail hone se login block nahi hona chahiye.
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.btnToggleMode.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.etName.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
