package com.eve.app.data.repository

import com.eve.app.data.model.AppContent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AppContentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        const val TYPE_PRIVACY = "privacy_policy"
        const val TYPE_TERMS = "terms_of_service"
        const val TYPE_CONTACT = "contact_us"
    }

    suspend fun getContent(type: String): AppContent {
        return try {
            val doc = db.collection("app_content").document(type).get().await()
            if (doc.exists()) {
                AppContent(
                    title = doc.getString("title").orEmpty(),
                    body = doc.getString("body").orEmpty(),
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    updatedBy = doc.getString("updatedBy").orEmpty(),
                    supportEmail = doc.getString("supportEmail").orEmpty(),
                    phone = doc.getString("phone").orEmpty(),
                    website = doc.getString("website").orEmpty(),
                    address = doc.getString("address").orEmpty()
                )
            } else {
                getDefaultContent(type)
            }
        } catch (_: Exception) {
            getDefaultContent(type)
        }
    }

    suspend fun saveContent(type: String, content: AppContent) {
        val data = hashMapOf(
            "title" to content.title,
            "body" to content.body,
            "updatedAt" to System.currentTimeMillis(),
            "updatedBy" to content.updatedBy,
            "supportEmail" to content.supportEmail,
            "phone" to content.phone,
            "website" to content.website,
            "address" to content.address
        )
        db.collection("app_content").document(type).set(data).await()
    }

    fun getDefaultContent(type: String): AppContent = when (type) {
        TYPE_PRIVACY -> AppContent(
            title = "Privacy Policy",
            body = """Eve ("the App", "we", "us") is a free mock test / exam preparation app for Government and entrance exams in India. This Privacy Policy explains what information the App collects and how it is used.

Information We Collect:
• Google Sign-In: When you sign in, we receive your name, email address, and profile photo from your Google account via Firebase Authentication.
• Test Attempts: We store your exam attempts, scores, selected answers, and attempt dates so you can view your test history.
• Device / Notification Token: To send you optional notifications about new exams or study reminders, we store a Firebase Cloud Messaging token linked to your account.

How We Use Your Information:
• To let you sign in and securely identify your account.
• To show your personal test history, scores, and exam progress.
• To send optional notifications if you enable them.

Data Storage & Sharing:
All data is stored securely using Google Firebase (Firestore, Authentication, Cloud Messaging). We do not run third-party advertising trackers or sell your personal data.

Children's Privacy:
The App is intended for students preparing for competitive exams and is not directed at children under 13.

Contact Us:
For questions or data deletion requests, contact us at pronlike9@gmail.com.""",
            updatedAt = 1727000000000L,
            updatedBy = "admin"
        )
        TYPE_TERMS -> AppContent(
            title = "Terms of Service",
            body = """Terms of Service for Eve

1. Acceptance of Terms:
By downloading, accessing, or using the Eve app, you agree to be bound by these Terms of Service. If you do not agree, please do not use the app.

2. Description of Service:
Eve provides free practice mock tests, past question papers, and study resources for competitive and entrance exams in India. All services are offered free of charge.

3. User Conduct:
You agree to use the app only for lawful study and preparation purposes. You may not attempt to reverse engineer, disrupt, or bypass authentication or scoring systems.

4. Intellectual Property:
Question papers, practice questions, and study material are provided for educational purposes. App trademarks and logos belong to Eve.

5. Disclaimer of Warranties:
The app is provided on an "as is" and "as available" basis without warranties of any kind. While we strive for accuracy, Eve does not guarantee that question answers are error-free or that competitive exam patterns will not change.

6. Changes to Terms:
We may update these Terms periodically. Continued use of the app signifies acceptance of updated terms.""",
            updatedAt = 1727000000000L,
            updatedBy = "admin"
        )
        TYPE_CONTACT -> AppContent(
            title = "Contact Us",
            body = "Have questions, feedback, or need help with your exam preparation? Reach out to our support team through any of the channels below.",
            updatedAt = 1727000000000L,
            updatedBy = "admin",
            supportEmail = "pronlike9@gmail.com",
            phone = "",
            website = "https://vishu762701.github.io/eve-mock-test-app",
            address = "India"
        )
        else -> AppContent()
    }
}
