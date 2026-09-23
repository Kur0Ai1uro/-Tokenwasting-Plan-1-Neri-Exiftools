package com.neri.exiftools.util

import com.neri.exiftools.model.CustomField
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object CustomXmp {
    private const val START = "<!--neri-custom-fields-->"
    private const val END = "<!--/neri-custom-fields-->"

    fun read(xmp: String?): List<CustomField> {
        if (xmp.isNullOrBlank() || !xmp.contains(START)) return emptyList()
        val body = xmp.substringAfter(START).substringBefore(END)
        return body.lineSequence().mapNotNull { line ->
            val parts = line.split('\t', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val name = decode(parts[0]).trim()
            val value = decode(parts[1])
            if (name.isEmpty() || value.isEmpty()) null else CustomField(name, value)
        }.toList()
    }

    fun merge(existing: String?, fields: List<CustomField>): String? {
        val kept = fields.filter { it.tag.isNotBlank() && it.value.isNotBlank() }
        val stripped = strip(existing)
        if (kept.isEmpty()) {
            return when {
                existing.isNullOrBlank() -> null
                stripped == existing -> existing
                stripped.isNullOrBlank() -> null
                else -> stripped
            }
        }
        val block = buildString {
            append(START)
            append('\n')
            kept.forEach { field ->
                append(encode(field.tag.trim()))
                append('\t')
                append(encode(field.value))
                append('\n')
            }
            append(END)
        }
        val base = stripped.orEmpty()
        if (base.isBlank()) return packet(block)
        val close = "</rdf:RDF>"
        return if (base.contains(close)) base.replace(close, "$block$close") else base + block
    }

    private fun strip(existing: String?): String? {
        if (existing.isNullOrBlank() || !existing.contains(START)) return existing
        val before = existing.substringBefore(START)
        val after = existing.substringAfter(END, "")
        return (before + after).trim().ifBlank { null }
    }

    private fun packet(block: String): String {
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
             <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              $block
             </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)
}
