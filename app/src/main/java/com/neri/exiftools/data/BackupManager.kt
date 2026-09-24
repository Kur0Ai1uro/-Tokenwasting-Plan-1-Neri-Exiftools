package com.neri.exiftools.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.neri.exiftools.model.ImageFormat
import com.neri.exiftools.util.SaveLocation
import com.neri.exiftools.util.SavePathRules
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

    fun saveEditedCopy(
        source: File,
        displayName: String,
        format: ImageFormat,
        location: SaveLocation = SaveLocation(),
    ): BackupResult {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = displayName.substringBeforeLast('.').ifBlank { "image" }
        val fileName = "edited_${stamp}_$safeName.${format.extension}"
        val tree = location.treeUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        if (tree != null) {
            val uri = createInTree(tree, fileName, format.mimeType)
            writeFileToUri(source, uri)
            return BackupResult(uri = uri, displayName = fileName, savedPath = location.displayLabel())
        }
        val relative = SavePathRules.normalize(location.relativePath) ?: SavePathRules.DEFAULT
        val uri = insertImage(
            fileName = fileName,
            mimeType = format.mimeType,
            relativePath = relative,
        ) ?: throw IOException("音理在 $relative 里建不了新文件")
        writeFileToUri(source, uri)
        return BackupResult(uri = uri, displayName = fileName, savedPath = relative)
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

        val picturesRoot = relativePath.substringBefore('/')
        val nested = relativePath.substringAfter('/', "")
        val base = when (picturesRoot) {
            "DCIM" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            "Download" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            "Movies" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        }
        val folder = File(base, if (picturesRoot == "Pictures" || picturesRoot !in STORAGE_ROOTS) {
            relativePath.removePrefix("Pictures/")
        } else {
            nested
        })
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

    private fun createInTree(tree: Uri, fileName: String, mimeType: String): Uri {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        return DocumentsContract.createDocument(resolver, parent, mimeType, fileName)
            ?: throw IOException("音理在这个文件夹里建不了新文件")
    }

    private fun writeFileToUri(source: File, uri: Uri) {
        val resolver = context.contentResolver
        resolver.openOutputStream(uri, "w")?.use { output ->
            source.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
        } ?: throw IOException("无法写入备份文件")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (uri.authority == MediaStore.AUTHORITY || uri.authority == "media")
        ) {
            val done = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
        }
    }

    data class BackupResult(
        val uri: Uri,
        val displayName: String,
        val savedPath: String = "",
    )

    private companion object {
        const val PICTURES_DIR = "Pictures"
        const val BACKUP_DIR = "NeriExifTools/Backup"
        val STORAGE_ROOTS = setOf("Pictures", "DCIM", "Download", "Movies")
    }
}
