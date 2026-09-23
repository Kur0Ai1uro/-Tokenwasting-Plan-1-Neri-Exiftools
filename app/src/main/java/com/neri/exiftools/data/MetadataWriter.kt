package com.neri.exiftools.data

import androidx.exifinterface.media.ExifInterface
import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
import com.neri.exiftools.util.CustomXmp
import com.neri.exiftools.util.DateParser
import com.neri.exiftools.util.GpsTagClearList
import com.neri.exiftools.util.OffsetTimeParser
import com.neri.exiftools.util.WritableTagCatalog
import java.io.File

class MetadataWriter {
    fun write(
        file: File,
        fields: CommonExifFields,
        customFields: List<CustomField> = emptyList(),
        tagsToClear: Set<String> = emptySet(),
    ) {
        val exif = ExifInterface(file.absolutePath)
        writeText(exif, ExifInterface.TAG_MAKE, fields.make)
        writeText(exif, ExifInterface.TAG_MODEL, fields.model)
        writeText(exif, ExifInterface.TAG_LENS_MODEL, fields.lensModel)
        writeText(exif, ExifInterface.TAG_IMAGE_DESCRIPTION, fields.imageDescription)
        writeText(exif, ExifInterface.TAG_COPYRIGHT, fields.copyright)
        writeText(exif, ExifInterface.TAG_USER_COMMENT, fields.userComment)
        writeText(exif, ExifInterface.TAG_ORIENTATION, fields.orientation.trim())

        val date = DateParser.normalize(fields.dateTimeOriginal)
        writeText(exif, ExifInterface.TAG_DATETIME_ORIGINAL, date)
        writeText(exif, ExifInterface.TAG_DATETIME_DIGITIZED, date)
        writeText(exif, ExifInterface.TAG_DATETIME, date)

        val offset = OffsetTimeParser.normalize(fields.offsetTimeOriginal)
        writeText(exif, ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offset)
        writeText(exif, ExifInterface.TAG_OFFSET_TIME_DIGITIZED, offset)
        writeText(exif, ExifInterface.TAG_OFFSET_TIME, offset)

        writeGps(exif, fields)
        for (tag in tagsToClear) {
            val spec = WritableTagCatalog.resolve(tag)
            if (spec != null) writeText(exif, spec.tag, null)
        }
        val freeFields = mutableListOf<CustomField>()
        for (field in customFields) {
            val spec = WritableTagCatalog.resolve(field.tag)
            if (spec == null) {
                if (field.tag.isNotBlank() && field.value.isNotBlank()) freeFields += field
            } else {
                writeText(exif, spec.tag, WritableTagCatalog.normalize(spec.tag, field.value))
            }
        }
        val existingXmp = exif.getAttribute(ExifInterface.TAG_XMP)
        val mergedXmp = CustomXmp.merge(existingXmp, freeFields)
        if (mergedXmp != existingXmp) {
            writeText(exif, ExifInterface.TAG_XMP, mergedXmp)
        }
        try {
            exif.saveAttributes()
        } catch (error: Exception) {
            if (mergedXmp == existingXmp) throw error
            writeText(exif, ExifInterface.TAG_XMP, existingXmp)
            exif.saveAttributes()
        }
    }

    private fun writeGps(exif: ExifInterface, fields: CommonExifFields) {
        if (GpsTagClearList.shouldClearGps(fields.gpsLatitude, fields.gpsLongitude)) {
            clearGps(exif)
            return
        }
        val latitude = fields.gpsLatitude.trim().toDouble()
        val longitude = fields.gpsLongitude.trim().toDouble()
        exif.setLatLong(latitude, longitude)

        val altitudeText = fields.gpsAltitude.trim()
        if (altitudeText.isBlank()) {
            writeText(exif, ExifInterface.TAG_GPS_ALTITUDE, null)
            writeText(exif, ExifInterface.TAG_GPS_ALTITUDE_REF, null)
        } else {
            exif.setAltitude(altitudeText.toDouble())
        }
    }

    private fun clearGps(exif: ExifInterface) {
        for (tag in GpsTagClearList.TAGS) {
            writeText(exif, tag, null)
        }
    }

    private fun writeText(exif: ExifInterface, tag: String, value: String?) {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        exif.setAttribute(tag, normalized)
    }
}
