package com.neri.exiftools.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { MediaStore.getMediaUri(context, uri) }
                .getOrNull()
                ?.takeIf { isExternalMediaUri(it) }
                ?.let { return it }
        }
        MediaUris.mediaStoreId(uri.toString())?.let { id ->
            val candidate = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            if (mediaRowExists(candidate)) return candidate
        }
        return findByDisplayNameAndSize(uri)?.takeIf { isExternalMediaUri(it) }
    }

    fun createWriteRequest(uri: Uri): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val mediaUri = resolveMediaStoreUri(uri) ?: return null
        if (MediaUris.isReadOnlyGalleryUri(mediaUri)) return null
        return runCatching {
            val request = MediaStore.createWriteRequest(context.contentResolver, listOf(mediaUri))
            IntentSenderRequest.Builder(request.intentSender).build()
        }.getOrNull()
    }

    fun overwrite(uri: Uri, file: File): OverwriteResult {
        if (MediaUris.isReadOnlyGalleryUri(uri)) {
            return OverwriteResult.Denied(MediaUris.READ_ONLY_MESSAGE)
        }
        return try {
            writeToUri(uri, file)
            OverwriteResult.Success
        } catch (security: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && security is RecoverableSecurityException) {
                OverwriteResult.NeedUserConsent(security.userAction.actionIntent.intentSender)
            } else {
                OverwriteResult.Denied(MediaUris.userFacing(security.message))
            }
        } catch (error: Exception) {
            OverwriteResult.Denied(MediaUris.userFacing(error.message))
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
        if (name.isBlank() || name == "image") return null
        val selection = if (size > 0) {
            "${OpenableColumns.DISPLAY_NAME}=? AND ${OpenableColumns.SIZE}=?" to arrayOf(name, size.toString())
        } else {
            "${OpenableColumns.DISPLAY_NAME}=?" to arrayOf(name)
        }
        return runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                selection.first,
                selection.second,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                if (cursor.count != 1) return@use null
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }.getOrNull()
    }

    private fun mediaRowExists(uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null,
            )?.use { it.moveToFirst() } == true
        }.getOrDefault(false)
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long>? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                    (name ?: return@use null) to size
                }
        }.getOrNull()
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
