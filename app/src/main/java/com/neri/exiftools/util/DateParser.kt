package com.neri.exiftools.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateParser {
    private val outputFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    private val inputFormatters: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
    )

    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        for (formatter in inputFormatters) {
            try {
                val parsed = LocalDateTime.parse(value, formatter)
                return parsed.format(outputFormatter)
            } catch (_: DateTimeParseException) {
                continue
            }
        }
        return null
    }

    fun isValid(raw: String): Boolean {
        if (raw.isBlank()) return true
        return normalize(raw) != null
    }
}
