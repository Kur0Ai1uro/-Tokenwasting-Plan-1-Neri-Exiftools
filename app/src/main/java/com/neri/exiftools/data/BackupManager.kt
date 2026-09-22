package com.neri.exiftools.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.neri.exiftools.model.ImageFormat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {
    fun backup(source: File, displayName: String, format: ImageFormat): BackupResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = displayName.substringBeforeLast('.').ifBlank { "image" }
        val fileName = "backup_${stamp}_$safeName.${format.extension}"
        val mimeType = format.mimeType
        val uri = insertImage(
            fileName = fileName,
            mimeType = mimeType,
            relativePath = "$PICTURES_DIR/$BACKUP_DIR",
        ) ?: throw IOException("无法在相册中创建备份")
        writeFileToUri(source, uri)
        return BackupResult(uri = uri, displayName = fileName)
    }

    fun saveEditedCopy(source: File, displayName: String, format: ImageFormat): BackupResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = displayName.substringBeforeLast('.').ifBlank { "image" }
        val fileName = "edited_${stamp}_$safeName.${format.extension}"
        val uri = insertImage(
            fileName = fileName,
            mimeType = format.mimeType,
            relativePath = "$PICTURES_DIR/$EDITED_DIR",
        ) ?: throw IOException("无法另存为新图片")
        writeFileToUri(source, uri)
        return BackupResult(uri = uri, displayName = fileName)
    }

    private fun insertImage(fileName: String, mimeType: String, relativePath: String): Uri? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            return uri
        }

        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val folder = File(pictures, relativePath.removePrefix("${Environment.DIRECTORY_PICTURES}/"))
        if (!folder.exists() && !folder.mkdirs()) {
            throw IOException("无法创建备份目录")
        }
        val outFile = File(folder, fileName)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DATA, outFile.absolutePath)
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun writeFileToUri(source: File, uri: Uri) {
        val resolver = context.contentResolver
        resolver.openOutputStream(uri, "w")?.use { output ->
            source.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
        } ?: throw IOException("无法写入备份文件")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
        }
    }

    data class BackupResult(
        val uri: Uri,
        val displayName: String,
    )

    private companion object {
        const val PICTURES_DIR = "Pictures"
        const val BACKUP_DIR = "NeriExifTools/Backup"
        const val EDITED_DIR = "NeriExifTools/Edited"
    }
}
