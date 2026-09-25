package com.eve.app.data.repository

import android.content.Context
import android.net.Uri
import com.eve.app.data.model.HomeBanner
import com.eve.app.util.ExamImageHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Task A: Firestore repository for home promotional banners.
 * Handles real-time observation, upload validation (<= 5MB), reordering, and deletion.
 */
class HomeBannerRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bannersCollection = db.collection("home_banners")

    fun observeBanners(): Flow<List<HomeBanner>> = callbackFlow {
        val listener = bannersCollection
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
                    val storagePath = doc.getString("storagePath")
                    val order = doc.getLong("order")?.toInt() ?: 0
                    val uploadedAt = doc.getLong("uploadedAt") ?: 0L
                    val uploadedBy = doc.getString("uploadedBy") ?: ""
                    val active = doc.getBoolean("active") ?: true
                    HomeBanner(id, imageUrl, storagePath, order, uploadedAt, uploadedBy, active)
                }?.sortedBy { it.order } ?: emptyList()

                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getBanners(): List<HomeBanner> {
        val snap = bannersCollection.get().await()
        return snap.documents.mapNotNull { doc ->
            val id = doc.id
            val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
            val storagePath = doc.getString("storagePath")
            val order = doc.getLong("order")?.toInt() ?: 0
            val uploadedAt = doc.getLong("uploadedAt") ?: 0L
            val uploadedBy = doc.getString("uploadedBy") ?: ""
            val active = doc.getBoolean("active") ?: true
            HomeBanner(id, imageUrl, storagePath, order, uploadedAt, uploadedBy, active)
        }.sortedBy { it.order }
    }

    suspend fun uploadBanner(context: Context, uri: Uri, userEmail: String): Result<String> = runCatching {
        // Validate image size <= 5MB
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val size = stream.available()
            if (size > 5 * 1024 * 1024) {
                throw IllegalArgumentException("Banner image size exceeds 5MB limit.")
            }
        } ?: throw IllegalArgumentException("Cannot open image file.")

        val base64 = ExamImageHelper.uriToBannerBase64(context, uri)
            ?: throw IllegalStateException("Failed to process banner image.")

        // Compute next order
        val existing = getBanners()
        val nextOrder = if (existing.isEmpty()) 0 else (existing.maxOf { it.order } + 1)

        val docRef = bannersCollection.document()
        val data = hashMapOf(
            "id" to docRef.id,
            "imageUrl" to base64,
            "order" to nextOrder,
            "uploadedAt" to System.currentTimeMillis(),
            "uploadedBy" to userEmail,
            "active" to true
        )
        docRef.set(data).await()

        // Also keep legacy 'home_banner/active' synchronized for backwards compatibility
        try {
            db.collection("home_banner").document("active").set(data).await()
        } catch (_: Exception) { }

        docRef.id
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> = runCatching {
        bannersCollection.document(bannerId).delete().await()

        // Sync legacy banner if it was deleted
        try {
            val remaining = getBanners()
            if (remaining.isNotEmpty()) {
                val first = remaining.first()
                val data = hashMapOf(
                    "id" to first.id,
                    "imageUrl" to first.imageUrl,
                    "order" to first.order,
                    "uploadedAt" to first.uploadedAt,
                    "uploadedBy" to first.uploadedBy,
                    "active" to true
                )
                db.collection("home_banner").document("active").set(data).await()
            } else {
                db.collection("home_banner").document("active").delete().await()
            }
        } catch (_: Exception) { }
    }

    suspend fun reorderBanner(bannerId: String, moveUp: Boolean): Result<Unit> = runCatching {
        val list = getBanners().toMutableList()
        val index = list.indexOfFirst { it.id == bannerId }
        if (index == -1) return@runCatching

        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in list.indices) return@runCatching

        // Swap order values
        val currentBanner = list[index]
        val targetBanner = list[targetIndex]

        val batch = db.batch()
        batch.update(bannersCollection.document(currentBanner.id), "order", targetBanner.order)
        batch.update(bannersCollection.document(targetBanner.id), "order", currentBanner.order)
        batch.commit().await()
    }
}
