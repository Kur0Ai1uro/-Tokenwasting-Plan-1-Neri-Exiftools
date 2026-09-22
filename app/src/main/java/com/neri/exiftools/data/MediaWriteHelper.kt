package com.neri.exiftools.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.IntentSenderRequest
import java.io.File
import java.io.IOException

class MediaWriteHelper(private val context: Context) {
    fun resolveMediaStoreUri(uri: Uri): Uri? {
        if (isExternalMediaUri(uri)) return uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull()?.let { return it }
        }
        return findByDisplayNameAndSize(uri)
    }

    fun createWriteRequest(uri: Uri): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val mediaUri = resolveMediaStoreUri(uri) ?: return null
        val request = MediaStore.createWriteRequest(context.contentResolver, listOf(mediaUri))
        return IntentSenderRequest.Builder(request.intentSender).build()
    }

    fun overwrite(uri: Uri, file: File): OverwriteResult {
        return try {
            writeToUri(uri, file)
            OverwriteResult.Success
        } catch (security: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && security is RecoverableSecurityException) {
                OverwriteResult.NeedUserConsent(security.userAction.actionIntent.intentSender)
            } else {
                OverwriteResult.Denied(security.message ?: "没有写入原图的权限")
            }
        } catch (io: IOException) {
            OverwriteResult.Denied(io.message ?: "无法覆盖原图")
        }
    }

    fun writeToUri(uri: Uri, file: File) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("无法打开原图进行写入")
        stream.use { output ->
            file.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
        }
    }

    private fun findByDisplayNameAndSize(uri: Uri): Uri? {
        val (name, size) = queryNameAndSize(uri) ?: return null
        val projection = arrayOf(MediaStore.Images.Media._ID, OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${OpenableColumns.DISPLAY_NAME}=? AND ${OpenableColumns.SIZE}=?",
            arrayOf(name, size.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return null
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long>? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) ?: return null
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    return name to size
                }
            }
        return null
    }

    private fun isExternalMediaUri(uri: Uri): Boolean {
        val path = uri.path.orEmpty()
        if (path.contains("picker", ignoreCase = true)) return false
        val mediaAuthority = uri.authority == MediaStore.AUTHORITY || uri.authority == "media"
        return mediaAuthority && (path.contains("external") || path.contains("/images/media"))
    }

    sealed class OverwriteResult {
        data object Success : OverwriteResult()
        data class NeedUserConsent(val intentSender: IntentSender) : OverwriteResult()
        data class Denied(val reason: String) : OverwriteResult()
    }
}
