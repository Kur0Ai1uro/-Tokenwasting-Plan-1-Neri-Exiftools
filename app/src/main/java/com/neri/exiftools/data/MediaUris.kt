package com.neri.exiftools.data

import android.net.Uri

object MediaUris {
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
}
