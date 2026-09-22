package com.eve.app.data.repository

import com.eve.app.util.Constants
import com.eve.app.util.isHardcodedAdmin
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore collection: admins. Document ID = lowercase email, field "email" = original email.
 * Hardcoded Constants.ADMIN_EMAILS hamesha admin rahenge; yeh collection sirf extra admins ke liye hai
 * jo app ke andar se (bina rebuild ke) add/remove kiye ja sakte hain.
 */
class AdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Hardcoded + Firestore dono check karta hai */
    suspend fun isAdmin(email: String?): Boolean {
        if (email == null) return false
        if (isHardcodedAdmin(email)) return true
        return try {
            db.collection("admins").document(email.lowercase()).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    /** Sirf Firestore me manually add kiye gaye admins (hardcoded wale is list me nahi aate) */
    suspend fun getDynamicAdmins(): List<String> =
        db.collection("admins").get().await().documents.mapNotNull { doc ->
            doc.getString("email")
        }.sorted()

    suspend fun addAdmin(email: String) {
        val clean = email.trim()
        db.collection("admins").document(clean.lowercase())
            .set(hashMapOf("email" to clean)).await()
    }

    suspend fun removeAdmin(email: String) {
        db.collection("admins").document(email.trim().lowercase()).delete().await()
    }
}
