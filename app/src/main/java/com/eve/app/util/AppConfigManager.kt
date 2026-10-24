package com.eve.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.eve.app.BuildConfig
import com.eve.app.data.model.AppConfig
import com.eve.app.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppConfigManager {

    suspend fun fetchAppConfig(): AppConfig = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getAppConfig()
            if (response.success && response.data != null) {
                return@withContext response.data
            }
        } catch (_: Exception) {}
        AppConfig()
    }

    fun isUpdateRequired(config: AppConfig): Boolean {
        return BuildConfig.VERSION_CODE < config.minimum_supported_version_code
    }

    fun isMaintenanceActive(config: AppConfig): Boolean {
        return config.maintenance_mode
    }

    fun openUpdateLink(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/vishu762701/eve-mock-test-app/releases")
            )
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }
}
