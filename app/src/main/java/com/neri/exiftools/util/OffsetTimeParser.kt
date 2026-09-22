package com.neri.exiftools.util

object OffsetTimeParser {
    private val pattern = Regex("""^[+-](?:[01]\d|2[0-3]):[0-5]\d$""")

    fun isValid(raw: String): Boolean {
        if (raw.isBlank()) return true
        return pattern.matches(raw.trim())
    }

    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        return value.takeIf { isValid(it) }
    }
}
