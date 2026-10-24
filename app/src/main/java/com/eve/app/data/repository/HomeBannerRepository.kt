package com.eve.app.data.repository

import android.content.Context
import android.net.Uri
import com.eve.app.data.model.HomeBanner
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repository for home promotional banners via Cloudflare Worker API.
 * Uploads media to Supabase Storage via Worker and manages metadata in D1.
 */
class HomeBannerRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    fun observeBanners(): Flow<List<HomeBanner>> = flow {
        try {
            val res = api.getBanners()
            if (res.success && res.data != null) {
                emit(res.data.sortedBy { it.order })
            } else {
                emit(emptyList())
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getBanners(): List<HomeBanner> {
        val res = api.getBanners()
        return (res.data ?: emptyList()).sortedBy { it.order }
    }

    suspend fun uploadBanner(
        context: Context,
        uri: Uri,
        linkUrl: String,
        linkLabel: String
    ): Result<HomeBanner> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot open image file.")
        if (bytes.size > 5 * 1024 * 1024) {
            throw IllegalArgumentException("Banner image size exceeds 5MB limit.")
        }

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val reqBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val fileName = if (mimeType.equals("image/png", ignoreCase = true)) "banner.png" else "banner.jpg"
        val filePart = MultipartBody.Part.createFormData("file", fileName, reqBody)
        val textType = "text/plain".toMediaTypeOrNull()
        val res = api.uploadBanner(
            file = filePart,
            linkUrl = linkUrl.toRequestBody(textType),
            linkLabel = linkLabel.toRequestBody(textType)
        )

        if (!res.success || res.data == null) {
            throw IllegalStateException(res.error ?: "Failed to upload banner")
        }
        res.data
    }

    suspend fun updateBannerLink(bannerId: String, linkUrl: String, linkLabel: String): Result<Unit> = runCatching {
        val res = api.updateBannerLink(bannerId, mapOf("linkUrl" to linkUrl, "linkLabel" to linkLabel))
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to update banner link")
        }
    }

    suspend fun deleteBanner(bannerId: String): Result<String?> = runCatching {
        val res = api.deleteBanner(bannerId)
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to delete banner")
        }
        res.message
    }

    suspend fun reorderBanner(bannerId: String, moveUp: Boolean): Result<Unit> = runCatching {
        val res = api.reorderBanner(bannerId, mapOf("moveUp" to moveUp))
        if (!res.success) {
            throw IllegalStateException(res.error ?: "Failed to reorder banner")
        }
    }
}
