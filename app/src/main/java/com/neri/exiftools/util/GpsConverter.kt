package com.neri.exiftools.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object GpsConverter {
    fun decimalToDmsRational(decimal: Double): String {
        val absValue = abs(decimal)
        val degrees = absValue.toInt()
        val minutesFull = (absValue - degrees) * 60.0
        val minutes = minutesFull.toInt()
        val seconds = (minutesFull - minutes) * 60.0
        val secondsScaled = (seconds * 10_000.0).roundToLong().coerceAtLeast(0L)
        return "$degrees/1,$minutes/1,$secondsScaled/10000"
    }

    fun latitudeRef(decimal: Double): String = if (decimal >= 0.0) "N" else "S"

    fun longitudeRef(decimal: Double): String = if (decimal >= 0.0) "E" else "W"

    fun dmsToDecimal(dms: String, ref: String? = null): Double? {
        val cleaned = dms.trim()
        if (cleaned.isEmpty()) return null

        val fromRational = parseRationalDms(cleaned)
        val fromSymbols = parseSymbolicDms(cleaned)
        val fromCsv = parseCsvDms(cleaned)
        val value = fromRational ?: fromSymbols ?: fromCsv ?: cleaned.toDoubleOrNull()
        if (value == null || value.isNaN()) return null

        val signed = when (ref?.trim()?.uppercase()) {
            "S", "W" -> -abs(value)
            "N", "E" -> abs(value)
            else -> value
        }
        return signed
    }

    fun formatDecimal(value: Double): String {
        val text = "%.7f".format(Locale.US, value).trimEnd('0').trimEnd('.')
        return if (text == "-0") "0" else text
    }

    private fun parseRationalDms(raw: String): Double? {
        if (!raw.contains('/')) return null
        val parts = raw.split(',', ' ')
            .map { it.trim() }
            .filter { it.contains('/') }
        if (parts.size < 2) return null
        val numbers = parts.mapNotNull { parseRational(it) }
        if (numbers.size < 2) return null
        val degrees = numbers[0]
        val minutes = numbers.getOrElse(1) { 0.0 }
        val seconds = numbers.getOrElse(2) { 0.0 }
        return degrees + minutes / 60.0 + seconds / 3600.0
    }

    private fun parseCsvDms(raw: String): Double? {
        if (!raw.contains(',') || raw.contains('/') || raw.contains('°')) return null
        val numbers = raw.split(',').map { it.trim() }.mapNotNull { it.toDoubleOrNull() }
        if (numbers.size < 2) return null
        return numbers[0] + numbers.getOrElse(1) { 0.0 } / 60.0 + numbers.getOrElse(2) { 0.0 } / 3600.0
    }

    private fun parseSymbolicDms(raw: String): Double? {
        if (!raw.contains('°') && !raw.contains('\'')) return null
        val regex = Regex("""(-?\d+(?:\.\d+)?)\s*°(?:\s*(\d+(?:\.\d+)?)')?(?:\s*(\d+(?:\.\d+)?)")?""")
        val match = regex.find(raw.replace('′', '\'').replace('″', '"')) ?: return null
        val degrees = match.groupValues[1].toDouble()
        val minutes = match.groupValues[2].toDoubleOrNull() ?: 0.0
        val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0
        val sign = if (degrees < 0) -1.0 else 1.0
        return sign * (abs(degrees) + minutes / 60.0 + seconds / 3600.0)
    }

    private fun parseRational(value: String): Double? {
        val pieces = value.split('/')
        if (pieces.size != 2) return value.toDoubleOrNull()
        val numerator = pieces[0].toDoubleOrNull() ?: return null
        val denominator = pieces[1].toDoubleOrNull() ?: return null
        if (denominator == 0.0) return null
        return numerator / denominator
    }
}
