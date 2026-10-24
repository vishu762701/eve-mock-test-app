package com.eve.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.eve.app.BuildConfig
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivityLoginBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.home.MainActivity
import com.eve.app.util.AnalyticsHelper
import com.eve.app.util.CrashlyticsHelper
import com.eve.app.util.DebugCrashReporter
import com.eve.app.designsystem.DesignMotion
import com.eve.app.util.SystemBarHelper
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : EveBaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var isSignUpMode = false
    private var isCheckingAuth = true
    private var originalSubmitText = "Sign In"
    private var originalGoogleText = "Continue with Google"
    private var lastAttemptedGoogle = false
    private var authInFlight = false

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) {
                    setLoading(false)
                    showError("Google token not received")
                } else {
                    firebaseLoginWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                setLoading(false)
                if (e.statusCode != 12501) {
                    showError("Sign-in failed (code ${e.statusCode}). Please try again.")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { isCheckingAuth }
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) {
            if (BuildConfig.DEBUG && DebugCrashReporter.hasCrash(this)) {
                isCheckingAuth = false
                binding = ActivityLoginBinding.inflate(layoutInflater)
                setContentView(binding.root)
                SystemBarHelper.syncSystemBars(this)
                binding.btnBack.setOnClickListener { finish() }
                DebugCrashReporter.showCrashDialog(this) {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                }
                return
            }

            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
            overridePendingTransition(0, 0)
            finish()
            isCheckingAuth = false
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Telegram LoginActivity.java:776-781 uses a 20dp arc in a 32dp container.
        val loginArcSize = (20f * resources.displayMetrics.density).toInt()
        binding.progressGoogle.setSize(loginArcSize)
        binding.progressSubmit.setSize(loginArcSize)
        SystemBarHelper.syncSystemBars(this)
        isCheckingAuth = false

        if (BuildConfig.DEBUG && DebugCrashReporter.hasCrash(this)) {
            DebugCrashReporter.showCrashDialog(this)
        }

        if (savedInstanceState?.getBoolean("eve_login_signup_mode", false) == true) {
            toggleMode(animate = false)
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { handleEmailPasswordAuth() }
        binding.btnGoogle.setOnClickListener { launchGoogleSignIn() }
        binding.btnToggleMode.setOnClickListener { toggleMode() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                handleEmailPasswordAuth()
                true
            } else {
                false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("eve_login_signup_mode", isSignUpMode)
    }

    private fun launchGoogleSignIn() {
        if (authInFlight) return
        lastAttemptedGoogle = true
        setLoading(true, isGoogle = true)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, options)
        // signOut is best-effort account-picker preparation, not a mandatory
        // authentication dependency. Preserve the existing Google provider flow.
        client.signOut().addOnCompleteListener {
            if (isFinishing || isDestroyed) return@addOnCompleteListener
            try {
                signInLauncher.launch(client.signInIntent)
            } catch (e: RuntimeException) {
                setLoading(false, isGoogle = true)
                showError("Google sign-in is unavailable right now.")
            }
        }
    }

    private fun updateModeText(view: TextView, label: String, animate: Boolean) {
        view.animate().cancel()
        view.text = label
        view.alpha = 1f
        view.translationY = 0f
        // The new content is visible and accessible immediately. Translate/fade
        // the same existing TextView; never create ghost focusable form controls.
        if (!animate || !com.eve.app.designsystem.UiArcMotion.isMotionEnabled(this)) return
        view.translationY = 4f * resources.displayMetrics.density
        view.alpha = 0.75f
        view.animate().translationY(0f).alpha(1f)
            .setDuration(com.eve.app.designsystem.UiArcMotion.Duration.STANDARD)
            .setInterpolator(com.eve.app.designsystem.UiArcMotion.Ease.ENTER).start()
    }

    private fun toggleMode(animate: Boolean = true) {
        if (authInFlight) return
        isSignUpMode = !isSignUpMode
        binding.tvError.visibility = View.GONE
        androidx.transition.TransitionManager.endTransitions(binding.cardLogin)
        if (animate && com.eve.app.designsystem.UiArcMotion.isMotionEnabled(this)) {
            val motion = com.eve.app.designsystem.UiArcMotion.Spring.SMOOTH
            androidx.transition.TransitionManager.beginDelayedTransition(
                binding.cardLogin, androidx.transition.ChangeBounds().apply {
                    duration = motion.settlingDurationMs
                    interpolator = motion
                }
            )
        }
        if (animate) binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        if (!isSignUpMode && binding.etName.hasFocus()) binding.etEmail.requestFocus()
        binding.tilName.visibility = if (isSignUpMode) View.VISIBLE else View.GONE
        updateModeText(binding.tvTagline,
            if (isSignUpMode) "Create your account" else "Welcome back", animate)
        updateModeText(binding.tvSubtitle,
            if (isSignUpMode) "Join Eve to save your mock tests and sync performance."
            else "Sign in to save your mock tests and sync performance.", animate)
        originalSubmitText = if (isSignUpMode) "Create Account" else "Sign In"
        updateModeText(binding.btnSubmit, originalSubmitText, animate)
        updateModeText(binding.btnToggleMode,
            if (isSignUpMode) "Already have an account? Sign In"
            else "Don't have an account? Sign Up", animate)
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            androidx.transition.TransitionManager.endTransitions(binding.cardLogin)
            listOf(binding.tvTagline, binding.tvSubtitle,
                binding.btnSubmit, binding.btnToggleMode).forEach { it.animate().cancel() }
        }
        super.onDestroy()
    }

    private fun handleEmailPasswordAuth() {
        if (authInFlight) return
        lastAttemptedGoogle = false
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
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
        setLoading(true, isGoogle = false)
        lifecycleScope.launch {
            try {
                if (isSignUpMode) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null && name.isNotEmpty()) {
                        user.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        ).await()
                    }
                    onAuthSuccess(user)
                } else {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    onAuthSuccess(result.user)
                }
            } catch (e: Exception) {
                setLoading(false, isGoogle = false)
                handleAuthError(e)
            }
        }
    }

    private fun firebaseLoginWithGoogle(idToken: String) {
        setLoading(true, isGoogle = true)
        lifecycleScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                onAuthSuccess(result.user)
            } catch (e: Exception) {
                setLoading(false, isGoogle = true)
                handleAuthError(e)
            }
        }
    }

    private suspend fun onAuthSuccess(user: FirebaseUser?) {
        if (user != null) {
            recordUserStats(user)
            AnalyticsHelper.logLogin(this@LoginActivity)
            val isAdmin = try {
                com.eve.app.data.repository.AdminRepository().isAdmin(user.email)
            } catch (_: Exception) {
                false
            }
            CrashlyticsHelper.identify(user.uid, isAdmin = isAdmin)
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

    private suspend fun recordUserStats(user: FirebaseUser) {
        try {
            ApiClient.apiService.syncUser(
                mapOf(
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName ?: "")
                )
            )
        } catch (_: Exception) {
            // User statistics sync is best effort after successful Firebase authentication.
        }
    }

    private fun setLoading(loading: Boolean, isGoogle: Boolean = lastAttemptedGoogle) {
        authInFlight = loading
        if (loading) {
            if (isGoogle) {
                binding.btnGoogle.icon = null
                binding.btnGoogle.text = ""
                binding.progressGoogle.visibility = View.VISIBLE
            } else {
                binding.btnSubmit.text = ""
                binding.progressSubmit.visibility = View.VISIBLE
            }
            binding.btnSubmit.isEnabled = false
            binding.btnGoogle.isEnabled = false
            binding.btnToggleMode.isEnabled = false
            binding.etEmail.isEnabled = false
            binding.etPassword.isEnabled = false
            binding.etName.isEnabled = false
        } else {
            binding.progressGoogle.visibility = View.GONE
            binding.progressSubmit.visibility = View.GONE
            binding.btnGoogle.setIconResource(R.drawable.ic_social_google)
            binding.btnGoogle.text = originalGoogleText
            binding.btnSubmit.text = originalSubmitText
            binding.btnSubmit.isEnabled = true
            binding.btnGoogle.isEnabled = true
            binding.btnToggleMode.isEnabled = true
            binding.etEmail.isEnabled = true
            binding.etPassword.isEnabled = true
            binding.etName.isEnabled = true
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
