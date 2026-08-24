package com.appmelt.builder.output

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class OutputManager(private val context: Context) {

    fun installApk(apkPath: String): Boolean {
        return try {
            val file = File(apkPath)
            if (!file.exists()) {
                Toast.makeText(context, "APK file not found at: $apkPath", Toast.LENGTH_SHORT).show()
                return false
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Install intent error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun shareArtifact(artifactPath: String, title: String = "Share Built App"): Boolean {
        return try {
            val file = File(artifactPath)
            if (!file.exists()) return false

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val isAab = file.extension.equals("aab", ignoreCase = true)
            val mimeType = if (isAab) "application/octet-stream" else "application/vnd.android.package-archive"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "Built with AppMelt Mobile Android Project Builder: ${file.name}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun saveToDownloads(artifactPath: String): String? {
        return try {
            val source = File(artifactPath)
            if (!source.exists()) return null

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val dest = File(downloadsDir, source.name)
            FileInputStream(source).use { fis ->
                FileOutputStream(dest).use { fos ->
                    fis.copyTo(fos)
                }
            }
            Toast.makeText(context, "Saved to Downloads: ${dest.name}", Toast.LENGTH_SHORT).show()
            dest.absolutePath
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }
}
