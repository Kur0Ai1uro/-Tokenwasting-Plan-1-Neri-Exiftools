package com.neri.exiftools.util

import kotlin.math.abs
import kotlin.math.round

enum class TagValueKind {
    TEXT,
    INTEGER,
    RATIONAL,
}

data class WritableTag(
    val tag: String,
    val label: String,
    val hint: String,
    val kind: TagValueKind,
)

object WritableTagCatalog {
    val tags: List<WritableTag> = listOf(
        WritableTag("Artist", "作者", "写下作者名", TagValueKind.TEXT),
        WritableTag("Software", "软件", "写下软件名", TagValueKind.TEXT),
        WritableTag("CameraOwnerName", "相机主人", "写下主人名字", TagValueKind.TEXT),
        WritableTag("BodySerialNumber", "机身序列号", "机身上的编号", TagValueKind.TEXT),
        WritableTag("LensMake", "镜头厂商", "镜头是谁做的", TagValueKind.TEXT),
        WritableTag("LensSerialNumber", "镜头序列号", "镜头上的编号", TagValueKind.TEXT),
        WritableTag("ImageUniqueID", "图像编号", "给这张照片一个编号", TagValueKind.TEXT),
        WritableTag("SpectralSensitivity", "光谱灵敏度", "", TagValueKind.TEXT),
        WritableTag("RelatedSoundFile", "关联音频", "音频文件名", TagValueKind.TEXT),
        WritableTag("SubSecTimeOriginal", "拍摄亚秒", "例如 123", TagValueKind.TEXT),
        WritableTag("SubSecTime", "修改亚秒", "例如 123", TagValueKind.TEXT),
        WritableTag("SubSecTimeDigitized", "数字化亚秒", "例如 123", TagValueKind.TEXT),
        WritableTag("GPSProcessingMethod", "定位方式", "例如 GPS", TagValueKind.TEXT),
        WritableTag("GPSAreaInformation", "定位区域", "", TagValueKind.TEXT),
        WritableTag("GPSMapDatum", "地图基准", "例如 WGS-84", TagValueKind.TEXT),
        WritableTag("GPSSatellites", "定位卫星", "", TagValueKind.TEXT),
        WritableTag("GPSDateStamp", "GPS 日期", "yyyy:MM:dd", TagValueKind.TEXT),
        WritableTag("PhotographicSensitivity", "ISO", "例如 100", TagValueKind.INTEGER),
        WritableTag("ISOSpeedRatings", "ISO（旧）", "例如 100", TagValueKind.INTEGER),
        WritableTag("FocalLengthIn35mmFilm", "35mm 等效焦距", "单位是毫米", TagValueKind.INTEGER),
        WritableTag("WhiteBalance", "白平衡", "0 自动，1 手动", TagValueKind.INTEGER),
        WritableTag("Flash", "闪光灯", "例如 0", TagValueKind.INTEGER),
        WritableTag("ExposureProgram", "曝光程序", "例如 2", TagValueKind.INTEGER),
        WritableTag("ExposureMode", "曝光模式", "0 自动，1 手动", TagValueKind.INTEGER),
        WritableTag("MeteringMode", "测光模式", "例如 5", TagValueKind.INTEGER),
        WritableTag("LightSource", "光源", "例如 1", TagValueKind.INTEGER),
        WritableTag("SceneCaptureType", "场景类型", "0 标准，1 风景", TagValueKind.INTEGER),
        WritableTag("Contrast", "对比度", "0 普通，1 柔和，2 强烈", TagValueKind.INTEGER),
        WritableTag("Saturation", "饱和度", "0 普通，1 低，2 高", TagValueKind.INTEGER),
        WritableTag("Sharpness", "锐度", "0 普通，1 柔和，2 强烈", TagValueKind.INTEGER),
        WritableTag("ExposureTime", "曝光时间", "1/125", TagValueKind.RATIONAL),
        WritableTag("FNumber", "光圈", "2.8 或 28/10", TagValueKind.RATIONAL),
        WritableTag("FocalLength", "焦距（毫米）", "50 或 50/1", TagValueKind.RATIONAL),
        WritableTag("ExposureBiasValue", "曝光补偿", "0.3 或 -1/3", TagValueKind.RATIONAL),
        WritableTag("ApertureValue", "光圈值", "例如 4.0", TagValueKind.RATIONAL),
        WritableTag("ShutterSpeedValue", "快门速度值", "例如 6.9", TagValueKind.RATIONAL),
        WritableTag("BrightnessValue", "亮度", "例如 3.2", TagValueKind.RATIONAL),
        WritableTag("MaxApertureValue", "最大光圈", "例如 2.8", TagValueKind.RATIONAL),
        WritableTag("DigitalZoomRatio", "数码变焦", "例如 1", TagValueKind.RATIONAL),
        WritableTag("SubjectDistance", "主体距离（米）", "例如 2.5", TagValueKind.RATIONAL),
    )

    private val byTag = tags.associateBy { it.tag }

    fun find(tag: String): WritableTag? = byTag[tag]

    fun normalize(tag: String, raw: String): String? {
        val spec = byTag[tag] ?: return null
        val text = raw.trim()
        if (text.isEmpty()) return null
        return when (spec.kind) {
            TagValueKind.TEXT -> text
            TagValueKind.INTEGER -> text.toLongOrNull()?.toString()
            TagValueKind.RATIONAL -> normalizeRational(text)
        }
    }

    fun normalizeRational(text: String): String? {
        val value = text.trim()
        val fraction = FRACTION.matchEntire(value)
        if (fraction != null) {
            val denominator = fraction.groupValues[2].toLongOrNull() ?: return null
            if (denominator == 0L) return null
            return value
        }
        val number = value.toDoubleOrNull() ?: return null
        if (number.isNaN() || number.isInfinite()) return null
        val sign = if (number < 0) "-" else ""
        var numerator = round(abs(number) * RATIONAL_SCALE).toLong()
        var denominator = RATIONAL_SCALE
        val divisor = gcd(numerator, denominator)
        numerator /= divisor
        denominator /= divisor
        return "$sign$numerator/$denominator"
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val next = x % y
            x = y
            y = next
        }
        return if (x == 0L) 1L else x
    }

    private val FRACTION = Regex("""^(-?\d+)/(-?\d+)$""")
    private const val RATIONAL_SCALE = 1000L
}
