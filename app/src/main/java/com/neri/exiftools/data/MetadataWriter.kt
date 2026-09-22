package com.neri.exiftools.data

import androidx.exifinterface.media.ExifInterface
import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
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
            if (WritableTagCatalog.find(tag) != null) {
                writeText(exif, tag, null)
            }
        }
        for (field in customFields) {
            if (WritableTagCatalog.find(field.tag) == null) continue
            writeText(exif, field.tag, WritableTagCatalog.normalize(field.tag, field.value))
        }
        exif.saveAttributes()
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
