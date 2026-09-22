package com.neri.exiftools.model

import android.graphics.Bitmap
import android.net.Uri

enum class ImageFormat(
    val displayName: String,
    val mimeType: String,
    val extension: String,
) {
    JPEG("JPEG", "image/jpeg", "jpg"),
    PNG("PNG", "image/png", "png"),
    WEBP("WebP", "image/webp", "webp"),
    UNSUPPORTED("不支持", "application/octet-stream", ""),
}

data class ImageInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val format: ImageFormat,
)

data class TagItem(
    val name: String,
    val value: String,
    val directoryName: String,
)

data class TagGroup(
    val directoryName: String,
    val tags: List<TagItem>,
)

data class CustomField(
    val tag: String,
    val value: String,
)

data class MetadataSnapshot(
    val fields: CommonExifFields,
    val groups: List<TagGroup>,
    val customFields: List<CustomField>,
)

data class CommonExifFields(
    val dateTimeOriginal: String = "",
    val offsetTimeOriginal: String = "",
    val make: String = "",
    val model: String = "",
    val lensModel: String = "",
    val imageDescription: String = "",
    val copyright: String = "",
    val userComment: String = "",
    val orientation: String = "",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val gpsAltitude: String = "",
)

data class LoadedImage(
    val info: ImageInfo,
    val fields: CommonExifFields,
    val groups: List<TagGroup>,
    val customFields: List<CustomField> = emptyList(),
    val workingFile: java.io.File,
    val preview: Bitmap? = null,
)
