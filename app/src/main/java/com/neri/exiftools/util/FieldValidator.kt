package com.neri.exiftools.util

import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField

object FieldValidator {
    fun validate(fields: CommonExifFields, customFields: List<CustomField> = emptyList()): List<String> {
        val errors = mutableListOf<String>()

        if (fields.dateTimeOriginal.isNotBlank() && DateParser.normalize(fields.dateTimeOriginal) == null) {
            errors += "拍摄时间格式应为 yyyy:MM:dd HH:mm:ss"
        }
        if (!OffsetTimeParser.isValid(fields.offsetTimeOriginal)) {
            errors += "时区偏移格式应为 +08:00"
        }

        val latitudeText = fields.gpsLatitude.trim()
        val longitudeText = fields.gpsLongitude.trim()
        if (latitudeText.isBlank() xor longitudeText.isBlank()) {
            errors += "纬度和经度需要同时填写或同时留空"
        }
        if (latitudeText.isNotBlank()) {
            val latitude = latitudeText.toDoubleOrNull()
            if (latitude == null) {
                errors += "纬度必须是数字"
            } else if (latitude !in -90.0..90.0) {
                errors += "纬度需在 -90 到 90 之间"
            }
        }
        if (longitudeText.isNotBlank()) {
            val longitude = longitudeText.toDoubleOrNull()
            if (longitude == null) {
                errors += "经度必须是数字"
            } else if (longitude !in -180.0..180.0) {
                errors += "经度需在 -180 到 180 之间"
            }
        }
        if (fields.gpsAltitude.isNotBlank() && fields.gpsAltitude.trim().toDoubleOrNull() == null) {
            errors += "海拔必须是数字"
        }
        if (fields.orientation.isNotBlank() && fields.orientation.trim() !in ORIENTATIONS) {
            errors += "方向应为 1–8 或留空"
        }
        errors += validateCustom(customFields)
        return errors
    }

    private fun validateCustom(fields: List<CustomField>): List<String> {
        val errors = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (field in fields) {
            val spec = WritableTagCatalog.resolve(field.tag)
            val key = (spec?.tag ?: field.tag).trim()
            if (key.isEmpty()) {
                errors += "字段名不能空着"
                continue
            }
            if (!seen.add(key.lowercase())) {
                errors += "「${spec?.label ?: key}」重复了"
                continue
            }
            if (spec == null || field.value.isBlank()) continue
            if (WritableTagCatalog.normalize(spec.tag, field.value) == null) {
                errors += when (spec.kind) {
                    TagValueKind.INTEGER -> "「${spec.label}」要写成整数"
                    TagValueKind.RATIONAL -> "「${spec.label}」要写成数字或分数，比如 ${spec.hint}"
                    TagValueKind.TEXT -> "「${spec.label}」没写对"
                }
            }
        }
        return errors
    }

    private val ORIENTATIONS = (1..8).map { it.toString() }
}
