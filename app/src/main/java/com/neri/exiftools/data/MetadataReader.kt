package com.neri.exiftools.data

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import androidx.exifinterface.media.ExifInterface
import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
import com.neri.exiftools.model.MetadataSnapshot
import com.neri.exiftools.model.TagGroup
import com.neri.exiftools.model.TagItem
import com.neri.exiftools.util.CustomXmp
import com.neri.exiftools.util.GpsConverter
import com.neri.exiftools.util.WritableTagCatalog
import java.io.File

class MetadataReader {
    fun read(file: File): MetadataSnapshot {
        val exif = ExifInterface(file.absolutePath)
        val fromInterface = readWithExifInterface(exif)
        val metadata = ImageMetadataReader.readMetadata(file)
        val groups = groupsFrom(metadata)
        val fromExtractor = readWithExtractor(metadata)
        return MetadataSnapshot(
            fields = merge(fromInterface, fromExtractor),
            groups = groups,
            customFields = readCustom(exif),
        )
    }

    private fun groupsFrom(metadata: com.drew.metadata.Metadata): List<TagGroup> {
        return metadata.directories.mapNotNull { directory ->
            val tags = directory.tags.map { tag ->
                TagItem(
                    name = tag.tagName,
                    value = tag.description?.takeIf { it.isNotBlank() } ?: tag.toString(),
                    directoryName = directory.name,
                )
            }
            if (tags.isEmpty()) null else TagGroup(directoryName = directory.name, tags = tags)
        }
    }

    private fun readCustom(exif: ExifInterface): List<CustomField> {
        val fromTags = WritableTagCatalog.tags.mapNotNull { spec ->
            val value = exif.getAttribute(spec.tag)?.trim().orEmpty()
            if (value.isEmpty()) null else CustomField(tag = spec.tag, value = value)
        }
        val known = fromTags.map { it.tag }.toSet()
        val fromXmp = CustomXmp.read(exif.getAttribute(ExifInterface.TAG_XMP))
            .filter { it.tag !in known }
        return fromTags + fromXmp
    }

    private fun readWithExifInterface(exif: ExifInterface): CommonExifFields {
        val latLong = exif.latLong
        val altitude = if (exif.hasAttribute(ExifInterface.TAG_GPS_ALTITUDE)) {
            val value = exif.getAltitude(Double.NaN)
            if (value.isNaN()) "" else GpsConverter.formatDecimal(value)
        } else {
            ""
        }
        return CommonExifFields(
            dateTimeOriginal = firstAttribute(
                exif,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME,
            ),
            offsetTimeOriginal = firstAttribute(
                exif,
                ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                ExifInterface.TAG_OFFSET_TIME,
            ),
            make = attribute(exif, ExifInterface.TAG_MAKE),
            model = attribute(exif, ExifInterface.TAG_MODEL),
            lensModel = attribute(exif, ExifInterface.TAG_LENS_MODEL),
            imageDescription = attribute(exif, ExifInterface.TAG_IMAGE_DESCRIPTION),
            copyright = attribute(exif, ExifInterface.TAG_COPYRIGHT),
            userComment = attribute(exif, ExifInterface.TAG_USER_COMMENT),
            orientation = attribute(exif, ExifInterface.TAG_ORIENTATION),
            gpsLatitude = latLong?.getOrNull(0)?.let(GpsConverter::formatDecimal).orEmpty(),
            gpsLongitude = latLong?.getOrNull(1)?.let(GpsConverter::formatDecimal).orEmpty(),
            gpsAltitude = altitude,
        )
    }

    private fun readWithExtractor(metadata: com.drew.metadata.Metadata): CommonExifFields {
        val ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
        val subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val gps = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
        val geo = gps?.geoLocation
        val altitude = if (gps?.containsTag(GpsDirectory.TAG_ALTITUDE) == true) {
            gps.getDouble(GpsDirectory.TAG_ALTITUDE)
        } else {
            null
        }
        val altitudeRef = gps?.getInteger(GpsDirectory.TAG_ALTITUDE_REF)
        val signedAltitude = altitude?.let { value ->
            val signed = if (altitudeRef != null && altitudeRef != 0) -value else value
            GpsConverter.formatDecimal(signed)
        }.orEmpty()

        return CommonExifFields(
            dateTimeOriginal = firstString(
                subIfd?.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL),
                ifd0?.getString(ExifIFD0Directory.TAG_DATETIME),
            ),
            offsetTimeOriginal = firstString(
                subIfd?.getDescription(TAG_OFFSET_TIME_ORIGINAL),
                subIfd?.getDescription(TAG_OFFSET_TIME),
            ),
            make = ifd0?.getString(ExifIFD0Directory.TAG_MAKE).orEmpty(),
            model = ifd0?.getString(ExifIFD0Directory.TAG_MODEL).orEmpty(),
            lensModel = firstString(
                subIfd?.getString(ExifSubIFDDirectory.TAG_LENS_MODEL),
                subIfd?.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL),
            ),
            imageDescription = ifd0?.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION).orEmpty(),
            copyright = ifd0?.getString(ExifIFD0Directory.TAG_COPYRIGHT).orEmpty(),
            userComment = subIfd?.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT).orEmpty(),
            orientation = ifd0?.getInteger(ExifIFD0Directory.TAG_ORIENTATION)?.toString().orEmpty(),
            gpsLatitude = geo?.latitude?.let(GpsConverter::formatDecimal).orEmpty(),
            gpsLongitude = geo?.longitude?.let(GpsConverter::formatDecimal).orEmpty(),
            gpsAltitude = signedAltitude,
        )
    }

    private fun merge(primary: CommonExifFields, fallback: CommonExifFields): CommonExifFields {
        return CommonExifFields(
            dateTimeOriginal = primary.dateTimeOriginal.ifBlank { fallback.dateTimeOriginal },
            offsetTimeOriginal = primary.offsetTimeOriginal.ifBlank { fallback.offsetTimeOriginal },
            make = primary.make.ifBlank { fallback.make },
            model = primary.model.ifBlank { fallback.model },
            lensModel = primary.lensModel.ifBlank { fallback.lensModel },
            imageDescription = primary.imageDescription.ifBlank { fallback.imageDescription },
            copyright = primary.copyright.ifBlank { fallback.copyright },
            userComment = primary.userComment.ifBlank { fallback.userComment },
            orientation = primary.orientation.ifBlank { fallback.orientation },
            gpsLatitude = primary.gpsLatitude.ifBlank { fallback.gpsLatitude },
            gpsLongitude = primary.gpsLongitude.ifBlank { fallback.gpsLongitude },
            gpsAltitude = primary.gpsAltitude.ifBlank { fallback.gpsAltitude },
        )
    }

    private fun attribute(exif: ExifInterface, tag: String): String {
        return exif.getAttribute(tag)?.trim().orEmpty()
    }

    private fun firstAttribute(exif: ExifInterface, vararg tags: String): String {
        for (tag in tags) {
            val value = attribute(exif, tag)
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun firstString(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
    }

    private companion object {
        const val TAG_OFFSET_TIME = 0x9010
        const val TAG_OFFSET_TIME_ORIGINAL = 0x9011
    }
}
