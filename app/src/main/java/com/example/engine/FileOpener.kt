package com.example.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileOpener {

    /**
     * Resolves exact MIME type for PDF, Excel, PowerPoint, Word, Images, etc.
     */
    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "pdf" -> "application/pdf"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "zip" -> "application/zip"
            else -> {
                val mimeFromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                mimeFromMap ?: "*/*"
            }
        }
    }

    /**
     * Checks if a file path corresponds to an image format suitable for image previewing.
     */
    fun isImage(pathOrName: String): Boolean {
        val ext = pathOrName.substringAfterLast('.', "").lowercase()
        return ext in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
    }

    /**
     * Checks if a file path is a PDF document.
     */
    fun isPdf(pathOrName: String): Boolean {
        return pathOrName.substringAfterLast('.', "").equals("pdf", ignoreCase = true)
    }

    /**
     * Checks if a file path is an Office document (.docx, .doc, .xlsx, .xls, .pptx, .ppt).
     */
    fun isOfficeDoc(pathOrName: String): Boolean {
        val ext = pathOrName.substringAfterLast('.', "").lowercase()
        return ext in listOf("docx", "doc", "xlsx", "xls", "pptx", "ppt")
    }

    /**
     * Opens any file using Android's system viewer apps with FileProvider.
     */
    fun openFileWithSystemApp(context: Context, file: File, onFailed: ((String) -> Unit)? = null): Boolean {
        if (!file.exists()) {
            val msg = "File not found: ${file.name}"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onFailed?.invoke(msg)
            return false
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = getMimeType(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open ${file.name} with...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } catch (e: ActivityNotFoundException) {
            val errorMsg = "No app installed to open ${file.extension.uppercase()} files. Please install a compatible viewer."
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            onFailed?.invoke(errorMsg)
            return false
        } catch (e: Exception) {
            val errorMsg = "Could not open ${file.name}: ${e.localizedMessage ?: "Unknown error"}"
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            onFailed?.invoke(errorMsg)
            return false
        }
    }

    /**
     * Shares a file with other apps via Intent.ACTION_SEND
     */
    fun shareFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = getMimeType(file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies a source file to a user-selected destination Uri using Storage Access Framework (SAF).
     */
    fun exportFileToSafUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
