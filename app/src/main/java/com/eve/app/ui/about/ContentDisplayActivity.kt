package com.eve.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.model.AppContent
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.databinding.ActivityContentDisplayBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContentDisplayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTENT_TYPE = "extra_content_type"
    }

    private lateinit var binding: ActivityContentDisplayBinding
    private val repo = AppContentRepository()
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContentDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val type = intent.getStringExtra(EXTRA_CONTENT_TYPE) ?: AppContentRepository.TYPE_PRIVACY
        loadContent(type)
    }

    private fun loadContent(type: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.INVISIBLE

        lifecycleScope.launch {
            val content = repo.getContent(type)
            displayContent(type, content)
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
        }
    }

    private fun displayContent(type: String, content: AppContent) {
        binding.tvHeaderTitle.text = content.title.ifBlank {
            when (type) {
                AppContentRepository.TYPE_PRIVACY -> "Privacy Policy"
                AppContentRepository.TYPE_TERMS -> "Terms of Service"
                AppContentRepository.TYPE_CONTACT -> "Contact Us"
                else -> "Content"
            }
        }
        binding.tvTitle.text = binding.tvHeaderTitle.text
        if (content.updatedAt > 0) {
            binding.tvLastUpdated.text = "Last updated: ${dateFormat.format(Date(content.updatedAt))}"
            binding.tvLastUpdated.visibility = View.VISIBLE
        } else {
            binding.tvLastUpdated.visibility = View.GONE
        }

        binding.tvBody.text = content.body

        if (type == AppContentRepository.TYPE_CONTACT) {
            binding.layoutContactActions.visibility = View.VISIBLE

            if (content.supportEmail.isNotBlank()) {
                binding.cardEmail.visibility = View.VISIBLE
                binding.tvEmailValue.text = content.supportEmail
                binding.cardEmail.setOnClickListener {
                    openEmail(content.supportEmail)
                }
            } else {
                binding.cardEmail.visibility = View.GONE
            }

            if (content.phone.isNotBlank()) {
                binding.cardPhone.visibility = View.VISIBLE
                binding.tvPhoneValue.text = content.phone
                binding.cardPhone.setOnClickListener {
                    openDialer(content.phone)
                }
            } else {
                binding.cardPhone.visibility = View.GONE
            }

            if (content.website.isNotBlank()) {
                binding.cardWebsite.visibility = View.VISIBLE
                binding.tvWebsiteValue.text = content.website
                binding.cardWebsite.setOnClickListener {
                    openUrl(content.website)
                }
            } else {
                binding.cardWebsite.visibility = View.GONE
            }
        } else {
            binding.layoutContactActions.visibility = View.GONE
        }
    }

    private fun openEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Eve App Support")
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDialer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formatted)))
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}
