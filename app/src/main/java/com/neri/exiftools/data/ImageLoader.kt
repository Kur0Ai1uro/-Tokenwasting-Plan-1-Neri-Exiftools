package com.neri.exiftools.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.neri.exiftools.model.ImageFormat
import com.neri.exiftools.model.ImageInfo
import com.neri.exiftools.util.ImageFormatDetector
import java.io.File
import java.io.IOException

class ImageLoader(private val context: Context) {
    fun cleanupStaleCache(keep: File? = null) {
        val now = System.currentTimeMillis()
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("neri_") && file != keep && now - file.lastModified() > 30_000L) {
                file.delete()
            }
        }
    }

    fun copyToCache(uri: Uri, displayName: String? = null): File {
        val name = displayName ?: queryDisplayName(uri).first
        val extensionHint = name.substringAfterLast('.', missingDelimiterValue = "img")
        val target = File(context.cacheDir, "neri_working_${System.currentTimeMillis()}.$extensionHint")
        openOriginalStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output, COPY_BUFFER) }
        } ?: throw IOException("无法读取所选图片")
        return target
    }

    fun inspect(
        uri: Uri,
        workingFile: File,
        displayName: String? = null,
        sizeFromQuery: Long = -1L,
    ): ImageInfo {
        val name: String
        val queriedSize: Long
        if (displayName != null) {
            name = displayName
            queriedSize = sizeFromQuery
        } else {
            val queried = queryDisplayName(uri)
            name = queried.first
            queriedSize = queried.second
        }
        val mime = context.contentResolver.getType(MediaUris.withoutRequireOriginal(uri)).orEmpty()
        val formatFromBytes = ImageFormatDetector.detect(workingFile)
        val format = if (formatFromBytes != ImageFormat.UNSUPPORTED) {
            formatFromBytes
        } else {
            ImageFormatDetector.detectMime(mime)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(workingFile.absolutePath, bounds)
        return ImageInfo(
            uri = MediaUris.withoutRequireOriginal(uri),
            displayName = name,
            mimeType = mime.ifBlank { format.mimeType },
            sizeBytes = if (queriedSize > 0) queriedSize else workingFile.length(),
            width = bounds.outWidth.coerceAtLeast(0),
            height = bounds.outHeight.coerceAtLeast(0),
            format = format,
        )
    }

    fun decodePreview(
        file: File,
        knownWidth: Int = 0,
        knownHeight: Int = 0,
        maxSize: Int = 1080,
    ): Bitmap? {
        val width: Int
        val height: Int
        if (knownWidth > 0 && knownHeight > 0) {
            width = knownWidth
            height = knownHeight
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            width = bounds.outWidth
            height = bounds.outHeight
        }
        if (width <= 0 || height <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(width, height, maxSize)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun openOriginalStream(uri: Uri): java.io.InputStream? {
        val safeUri = MediaUris.withoutRequireOriginal(uri)
        val resolver = context.contentResolver
        return runCatching { resolver.openInputStream(safeUri) }.getOrNull()
            ?: runCatching { resolver.openInputStream(safeUri.buildUpon().clearQuery().build()) }.getOrNull()
    }

    fun queryDisplayName(uri: Uri): Pair<String, Long> {
        val safeUri = MediaUris.withoutRequireOriginal(uri)
        return readDisplayName(safeUri)
            ?: readDisplayName(safeUri.buildUpon().clearQuery().build())
            ?: ("image" to 0L)
    }

    private fun readDisplayName(uri: Uri): Pair<String, Long>? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                (name ?: "image") to size
            }
        }.getOrNull()
    }

    private fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        while (width / sample > maxSize || height / sample > maxSize) {
            sample *= 2
        }
        return sample
    }

    companion object {
        const val COPY_BUFFER = 64 * 1024
    }
}
