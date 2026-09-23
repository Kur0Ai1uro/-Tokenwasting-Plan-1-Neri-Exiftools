package com.neri.exiftools.data

import android.net.Uri
import java.net.URLDecoder

object MediaUris {
    const val READ_ONLY_MESSAGE = "相册只让音理看这张图，原图改不了。"

    fun withoutRequireOriginal(uri: Uri): Uri {
        val raw = uri.toString()
        val names = uri.queryParameterNames
        val hasFlag = raw.contains("requireOriginal", ignoreCase = true) ||
            names.any { it.equals("requireOriginal", ignoreCase = true) }
        if (!hasFlag) return uri
        val builder = uri.buildUpon().clearQuery()
        for (name in names) {
            if (name.equals("requireOriginal", ignoreCase = true)) continue
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        return builder.build()
    }

    fun isPickerUri(value: String): Boolean = value.contains("picker", ignoreCase = true)

    fun isPickerUri(uri: Uri): Boolean {
        val haystack = listOf(uri.authority, uri.path, uri.toString()).joinToString("\n")
        return isPickerUri(haystack)
    }

    fun isReadOnlyGalleryUri(value: String): Boolean {
        val lower = value.lowercase()
        if (lower.contains("picker")) return true
        return GALLERY_PROVIDERS.any { lower.contains(it) }
    }

    fun isReadOnlyGalleryUri(uri: Uri): Boolean = isReadOnlyGalleryUri(uri.toString())

    fun mediaStoreId(value: String): Long? {
        val decoded = decodeUri(value)
        MEDIA_ID.find(decoded)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
        IMAGE_DOCUMENT.find(decoded)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
        if (isPickerUri(decoded)) {
            decoded.substringBefore('?').substringAfterLast('/').toLongOrNull()?.let { return it }
        }
        return null
    }

    fun userFacing(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return READ_ONLY_MESSAGE
        if (text.any { it in '\u4e00'..'\u9fff' }) return text
        return READ_ONLY_MESSAGE
    }

    private fun decodeUri(value: String): String {
        var current = value
        repeat(2) {
            if (!current.contains('%')) return current
            current = runCatching { URLDecoder.decode(current, Charsets.UTF_8.name()) }.getOrDefault(current)
        }
        return current
    }

    private val MEDIA_ID = Regex(
        """content://media/(?:[^/?#]+/)*images/media/(\d+)""",
        RegexOption.IGNORE_CASE,
    )
    private val IMAGE_DOCUMENT = Regex("""image:(\d+)""", RegexOption.IGNORE_CASE)

    private val GALLERY_PROVIDERS = listOf(
        "com.google.android.apps.photos.contentprovider",
        "com.miui.gallery",
        "com.huawei.gallery",
        "com.huawei.filemanager",
        "com.coloros.gallery",
        "com.oplus.gallery",
        "com.oneplus.gallery",
        "com.vivo.gallery",
        "com.sec.android.gallery3d",
        "com.samsung.android.gallery",
        "com.transsion.gallery",
        "com.nothing.gallery",
    )
}
