package com.eve.app.ui.login

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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
import com.eve.app.util.EveMotionHelper
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

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) {
                    setLoading(false)
                    showError("Google token not received", isGoogle = true)
                } else {
                    firebaseLoginWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                setLoading(false)
                if (e.statusCode != 12501) {
                    showError("Sign-in failed (code ${e.statusCode}). Please try again.", isGoogle = true)
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
        SystemBarHelper.syncSystemBars(this)
        isCheckingAuth = false

        if (BuildConfig.DEBUG && DebugCrashReporter.hasCrash(this)) {
            DebugCrashReporter.showCrashDialog(this)
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
        playEntranceAnimation()
        applyUiStudioConfig()
    }

    private fun applyUiStudioConfig() {
        com.eve.app.uistudio.StudioRenderer.apply(binding.root, "login", com.eve.app.data.repository.UiStudioRepository.getInstance().currentConfig)
    }

    private fun playEntranceAnimation() {
        if (!EveMotionHelper.areAnimationsEnabled(this)) return
        binding.cardLogin.alpha = 0f
        binding.cardLogin.translationY = 14f * resources.displayMetrics.density
        binding.cardLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(EveMotionHelper.DURATION_STANDARD_MS)
            .setInterpolator(EveMotionHelper.standardInterpolator)
            .start()
    }

    private fun launchGoogleSignIn() {
        lastAttemptedGoogle = true
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, options)
        client.signOut().addOnCompleteListener {
            signInLauncher.launch(client.signInIntent)
        }
    }

    private fun toggleMode() {
        isSignUpMode = !isSignUpMode
        binding.tvError.visibility = View.GONE
        if (EveMotionHelper.areAnimationsEnabled(this)) {
            val transition = androidx.transition.AutoTransition().apply {
                duration = 200
                interpolator = FastOutSlowInInterpolator()
            }
            androidx.transition.TransitionManager.beginDelayedTransition(binding.cardLogin, transition)
        }
        binding.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        if (isSignUpMode) {
            binding.tilName.visibility = View.VISIBLE
            binding.tvTagline.text = "Create your account"
            binding.tvSubtitle.text = "Join Eve to save your mock tests and sync performance."
            originalSubmitText = "Create Account"
            binding.btnSubmit.text = originalSubmitText
            binding.btnToggleMode.text = "Already have an account? Sign In"
            binding.etName.requestFocus()
        } else {
            binding.tilName.visibility = View.GONE
            binding.tvTagline.text = "Welcome back"
            binding.tvSubtitle.text = "Sign in to save your mock tests and sync performance."
            originalSubmitText = "Sign In"
            binding.btnSubmit.text = originalSubmitText
            binding.btnToggleMode.text = "Don't have an account? Sign Up"
        }
    }

    private fun handleEmailPasswordAuth() {
        lastAttemptedGoogle = false
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
        val name = binding.etName.text?.toString()?.trim().orEmpty()

        if (isSignUpMode && name.isEmpty()) {
            showError("Please enter your name.", isGoogle = false)
            binding.etName.requestFocus()
            return
        }
        if (email.isEmpty()) {
            showError("Please enter your email address.", isGoogle = false)
            binding.etEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.", isGoogle = false)
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            showError("Please enter your password.", isGoogle = false)
            binding.etPassword.requestFocus()
            return
        }
        if (password.length < 6) {
            showError("Password must be at least 6 characters.", isGoogle = false)
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
                handleAuthError(e, isGoogle = false)
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
                handleAuthError(e, isGoogle = true)
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

        if (EveMotionHelper.areAnimationsEnabled(this)) {
            binding.contentLayout.animate()
                .alpha(0f)
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(240)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction { goHome() }
                .start()
        } else {
            goHome()
        }
    }

    private fun handleAuthError(e: Exception, isGoogle: Boolean) {
        val friendlyMessage = when (e) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
        showError(friendlyMessage, isGoogle)
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

    private fun showError(message: String, isGoogle: Boolean = lastAttemptedGoogle) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        val target = if (isGoogle) binding.layoutBtnGoogle else binding.layoutBtnSubmit
        if (EveMotionHelper.areAnimationsEnabled(this)) {
            val density = resources.displayMetrics.density
            ObjectAnimator.ofFloat(
                target,
                "translationX",
                0f,
                -8f * density,
                8f * density,
                -4f * density,
                4f * density,
                0f
            ).apply {
                duration = 260
                interpolator = FastOutSlowInInterpolator()
                start()
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
