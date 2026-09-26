package com.eve.app.ui.login

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.databinding.ActivityLoginBinding
import com.eve.app.ui.home.MainActivity
import com.eve.app.util.AnalyticsHelper
import com.eve.app.util.CrashlyticsHelper
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
import com.eve.app.data.remote.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

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
                if (e.statusCode != 12501) { // 12501 = SIGN_IN_CANCELLED by user
                    showError("Sign-in failed (code ${e.statusCode}). Please try again.", isGoogle = true)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Task B: Hold splash screen until auth check resolves
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { isCheckingAuth }

        super.onCreate(savedInstanceState)

        // Session check: existing authenticated users skip straight to Home (Task B & Task F)
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
            isCheckingAuth = false
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Auth check resolved; user genuinely landed on Login
        isCheckingAuth = false

        // Task C: Staggered Entrance Animation
        playStaggeredEntranceAnimation()

        // Task E: Button micro-interactions (press scale + haptics)
        setupButtonPressFeedback(binding.btnSubmit) {
            handleEmailPasswordAuth()
        }
        setupButtonPressFeedback(binding.btnGoogle) {
            launchGoogleSignIn()
        }

        // Toggle between Sign In and Sign Up mode
        binding.btnToggleMode.setOnClickListener {
            toggleMode()
        }
    }

    private fun areAnimationsEnabled(): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale > 0f
        } catch (_: Throwable) {
            true
        }
    }

    /**
     * Task C: Staggered Entrance Animation sequence.
     * Total sequence completes within ~550ms.
     * Logo -> Tagline -> Input Fields -> Buttons.
     * Respects accessibility settings (skips if animations disabled).
     */
    private fun playStaggeredEntranceAnimation() {
        if (!areAnimationsEnabled()) {
            binding.cardLogin.alpha = 1f
            binding.tvLogo.alpha = 1f
            binding.tvTagline.alpha = 1f
            binding.tvSubtitle.alpha = 1f
            binding.tilEmail.alpha = 1f
            binding.tilPassword.alpha = 1f
            binding.layoutBtnSubmit.alpha = 1f
            binding.btnToggleMode.alpha = 1f
            binding.dividerLayout.alpha = 1f
            binding.layoutBtnGoogle.alpha = 1f
            return
        }

        val interpolator = FastOutSlowInInterpolator()
        val density = resources.displayMetrics.density
        val translateY = 14f * density

        // 1. Logo: fade in + scale from 0.92 to 1.0 (starts at 0ms, duration 300ms)
        binding.tvLogo.alpha = 0f
        binding.tvLogo.scaleX = 0.92f
        binding.tvLogo.scaleY = 0.92f
        binding.tvLogo.animate()
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(300)
            .setInterpolator(interpolator)
            .start()

        // 2. Tagline and subtitle: fade in + translateY (starts at 90ms, duration 320ms)
        binding.tvTagline.alpha = 0f
        binding.tvTagline.translationY = translateY
        binding.tvTagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(90)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        binding.tvSubtitle.alpha = 0f
        binding.tvSubtitle.translationY = translateY
        binding.tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(110)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        // 3. Input fields: fade in + translateY (starts at 170ms, duration 320ms)
        binding.tilEmail.alpha = 0f
        binding.tilEmail.translationY = translateY
        binding.tilEmail.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(170)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        binding.tilPassword.alpha = 0f
        binding.tilPassword.translationY = translateY
        binding.tilPassword.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(190)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        // 4. Buttons and divider: fade in + translateY (starts at 240ms, duration 320ms)
        binding.layoutBtnSubmit.alpha = 0f
        binding.layoutBtnSubmit.translationY = translateY
        binding.layoutBtnSubmit.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(240)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        binding.btnToggleMode.alpha = 0f
        binding.btnToggleMode.translationY = translateY
        binding.btnToggleMode.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(260)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        binding.dividerLayout.alpha = 0f
        binding.dividerLayout.translationY = translateY
        binding.dividerLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(280)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()

        binding.layoutBtnGoogle.alpha = 0f
        binding.layoutBtnGoogle.translationY = translateY
        binding.layoutBtnGoogle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(300)
            .setDuration(320)
            .setInterpolator(interpolator)
            .start()
    }

    /**
     * Task E: Subtle scale-down (~0.96) on ACTION_DOWN with light haptic tick,
     * restoring scale on release/cancel, and triggering onClick.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtonPressFeedback(view: View, onClick: () -> Unit) {
        view.setOnTouchListener { v, event ->
            if (!v.isEnabled) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                    v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    if (event.x in 0f..v.width.toFloat() && event.y in 0f..v.height.toFloat()) {
                        onClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }
            }
            true
        }
    }

    private fun launchGoogleSignIn() {
        lastAttemptedGoogle = true
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener {
            signInLauncher.launch(client.signInIntent)
        }
    }

    private fun toggleMode() {
        isSignUpMode = !isSignUpMode
        binding.tvError.visibility = View.GONE

        if (isSignUpMode) {
            binding.tilName.visibility = View.VISIBLE
            originalSubmitText = "Create Account"
            binding.btnSubmit.text = originalSubmitText
            binding.btnToggleMode.text = "Already have an account? Sign In"
        } else {
            binding.tilName.visibility = View.GONE
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

    /**
     * Task E: Success handoff with subtle cross-fade / scale-up (< 300ms) into Home.
     */
    private suspend fun onAuthSuccess(user: FirebaseUser?) {
        if (user != null) {
            recordUserStats(user)
            AnalyticsHelper.logLogin(this@LoginActivity)
            CrashlyticsHelper.identify(user.uid, isAdmin = false)
        }

        if (areAnimationsEnabled()) {
            binding.contentLayout.animate()
                .alpha(0f)
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(260)
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
            // Stats optional
        }
    }

    /**
     * Task E: Button morphing loading state.
     * Label fades out, spinner fades in inside button bounds, button non-interactive.
     */
    private fun setLoading(loading: Boolean, isGoogle: Boolean = lastAttemptedGoogle) {
        if (loading) {
            if (isGoogle) {
                binding.btnGoogle.text = ""
                binding.progressGoogle.alpha = 0f
                binding.progressGoogle.visibility = View.VISIBLE
                binding.progressGoogle.animate().alpha(1f).setDuration(180).start()
            } else {
                binding.btnSubmit.text = ""
                binding.progressSubmit.alpha = 0f
                binding.progressSubmit.visibility = View.VISIBLE
                binding.progressSubmit.animate().alpha(1f).setDuration(180).start()
            }
            binding.btnSubmit.isEnabled = false
            binding.btnGoogle.isEnabled = false
            binding.btnToggleMode.isEnabled = false
            binding.etEmail.isEnabled = false
            binding.etPassword.isEnabled = false
            binding.etName.isEnabled = false
        } else {
            binding.progressGoogle.animate().alpha(0f).setDuration(150).withEndAction {
                binding.progressGoogle.visibility = View.GONE
                binding.btnGoogle.text = originalGoogleText
            }.start()

            binding.progressSubmit.animate().alpha(0f).setDuration(150).withEndAction {
                binding.progressSubmit.visibility = View.GONE
                binding.btnSubmit.text = originalSubmitText
            }.start()

            binding.btnSubmit.isEnabled = true
            binding.btnGoogle.isEnabled = true
            binding.btnToggleMode.isEnabled = true
            binding.etEmail.isEnabled = true
            binding.etPassword.isEnabled = true
            binding.etName.isEnabled = true
        }
    }

    /**
     * Task E: Error handling with subtle horizontal shake animation on the button (~300ms, 2-3 cycles).
     */
    private fun showError(msg: String, isGoogle: Boolean = lastAttemptedGoogle) {
        binding.tvError.text = msg
        binding.tvError.alpha = 0f
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.animate().alpha(1f).setDuration(200).start()

        val targetButton: View = if (isGoogle) binding.layoutBtnGoogle else binding.layoutBtnSubmit
        if (areAnimationsEnabled()) {
            val density = resources.displayMetrics.density
            val shift1 = -10f * density
            val shift2 = 10f * density
            val shift3 = -6f * density
            val shift4 = 6f * density
            val shift5 = -3f * density
            val shift6 = 3f * density

            ObjectAnimator.ofFloat(targetButton, "translationX", 0f, shift1, shift2, shift3, shift4, shift5, shift6, 0f).apply {
                duration = 320
                interpolator = FastOutSlowInInterpolator()
                start()
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) {
            binding.ambientBackground.stopAmbientMotion()
        }
    }
}
