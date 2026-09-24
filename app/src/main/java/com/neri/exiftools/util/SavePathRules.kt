package com.neri.exiftools.util

import java.net.URLDecoder

data class SaveLocation(
    val relativePath: String = SavePathRules.DEFAULT,
    val treeUri: String? = null,
) {
    fun displayLabel(): String {
        val tree = treeUri?.takeIf { it.isNotBlank() } ?: return relativePath
        return SavePathRules.treeLabel(tree)
    }
}

object SavePathRules {
    const val DEFAULT = "Pictures/NeriExifTools/Edited"

    private val ROOTS = setOf("Pictures", "DCIM", "Download", "Movies")

    fun normalize(raw: String): String? {
        val cleaned = raw.trim()
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." }
        if (cleaned.any { it == ".." }) return null
        if (cleaned.isEmpty()) return DEFAULT
        val withRoot = if (cleaned.first() in ROOTS) cleaned else listOf("Pictures") + cleaned
        return withRoot.joinToString("/")
    }

    fun treeLabel(uri: String): String {
        val decoded = runCatching { URLDecoder.decode(uri, Charsets.UTF_8.name()) }.getOrDefault(uri)
        val afterTree = decoded.substringAfter("/tree/", "")
        val document = if (afterTree.isEmpty()) {
            decoded.substringAfterLast('/')
        } else {
            afterTree.substringAfter(':')
        }
        return document.trim('/').ifBlank { "所选文件夹" }
    }
}
