package com.neri.exiftools.util

import com.neri.exiftools.model.ImageFormat
import java.io.File

object ImageFormatDetector {
    fun detect(file: File): ImageFormat {
        val header = ByteArray(16)
        val read = file.inputStream().use { it.read(header) }
        if (read <= 0) return ImageFormat.UNSUPPORTED
        return detect(header)
    }

    fun detect(bytes: ByteArray): ImageFormat {
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return ImageFormat.JPEG
        }
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
        ) {
            return ImageFormat.PNG
        }
        if (isWebP(bytes)) {
            return ImageFormat.WEBP
        }
        return ImageFormat.UNSUPPORTED
    }

    fun detectMime(mimeType: String?): ImageFormat {
        return when (mimeType?.lowercase()) {
            "image/jpeg", "image/jpg" -> ImageFormat.JPEG
            "image/png" -> ImageFormat.PNG
            "image/webp" -> ImageFormat.WEBP
            else -> ImageFormat.UNSUPPORTED
        }
    }

    private fun isWebP(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val riff = bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII)
        val webp = bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII)
        return riff == "RIFF" && webp == "WEBP"
    }
}
