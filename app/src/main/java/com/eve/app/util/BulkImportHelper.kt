package com.eve.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object BulkImportHelper {

    const val EXAM_TEMPLATE = "eve_questions_template.csv"
    const val DAILY_TEMPLATE = "eve_daily_gk_template.csv"

    fun shareTemplate(context: Context, assetName: String) {
        val out = File(context.cacheDir, assetName)
        context.assets.open(assetName).use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            out
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Eve question template")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Template share / save karo"))
    }

    fun displayName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) return it.getString(0) ?: ""
        }
        return uri.lastPathSegment.orEmpty()
    }
}
