package com.eve.app.ui.about

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eve.app.databinding.ActivityAboutBinding
import com.eve.app.util.Constants

/**
 * Phase 14: Simple "About" screen jo Play Store ke liye zaroori Privacy Policy aur
 * Terms of Service links dikhata hai (dono hosted HTML pages, GitHub Pages par).
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        binding.tvVersion.text = "Version $versionName"

        // Phase 15: debug build me version text ko long-press karo to ek test crash trigger
        // hoga — Crashlytics setup verify karne ka sabse aasan tarika (Firebase Console me
        // "Crashlytics" section me 2-3 min me report dikhega). Release build me kuch nahi hota.
        binding.tvVersion.setOnLongClickListener {
            if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                Toast.makeText(this, "Test crash triggering…", Toast.LENGTH_SHORT).show()
                throw RuntimeException("Eve: Crashlytics test crash (About screen long-press)")
            }
            true
        }

        binding.rowPrivacyPolicy.setOnClickListener { openUrl(Constants.PRIVACY_POLICY_URL) }
        binding.rowTerms.setOnClickListener { openUrl(Constants.TERMS_URL) }
        binding.rowContact.setOnClickListener { openEmail() }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No browser available - kuch nahi ho sakta, silently ignore
        }
    }

    private fun openEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Constants.SUPPORT_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, "Eve App Support")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // No email app available - silently ignore
        }
    }
}
