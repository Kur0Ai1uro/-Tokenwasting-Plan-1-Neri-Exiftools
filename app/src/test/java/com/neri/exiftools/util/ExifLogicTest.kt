package com.neri.exiftools.util

import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GpsConverterTest {
    @Test
    fun decimalToDmsAndBack_northEast() {
        val latitude = 31.230416
        val longitude = 121.473701
        val latDms = GpsConverter.decimalToDmsRational(latitude)
        val lonDms = GpsConverter.decimalToDmsRational(longitude)
        val parsedLat = GpsConverter.dmsToDecimal(latDms, GpsConverter.latitudeRef(latitude))
        val parsedLon = GpsConverter.dmsToDecimal(lonDms, GpsConverter.longitudeRef(longitude))
        assertNotNull(parsedLat)
        assertNotNull(parsedLon)
        assertTrue(abs(parsedLat!! - latitude) < 0.00002)
        assertTrue(abs(parsedLon!! - longitude) < 0.00002)
        assertEquals("N", GpsConverter.latitudeRef(latitude))
        assertEquals("E", GpsConverter.longitudeRef(longitude))
    }

    @Test
    fun dmsToDecimal_appliesSouthWestRefs() {
        val latitude = GpsConverter.dmsToDecimal("40/1,26/1,460000/10000", "S")
        val longitude = GpsConverter.dmsToDecimal("3° 42' 0\"", "W")
        assertNotNull(latitude)
        assertNotNull(longitude)
        assertTrue(latitude!! < 0)
        assertTrue(longitude!! < 0)
        // 40° 26' 46" = 40.446111...
        assertTrue(abs(latitude + 40.446111) < 0.00002)
        assertTrue(abs(longitude + 3.7) < 0.00002)
    }

    @Test
    fun formatDecimal_trimsTrailingZeros() {
        assertEquals("31.5", GpsConverter.formatDecimal(31.5))
        assertEquals("0", GpsConverter.formatDecimal(0.0))
    }
}

class DateParserTest {
    @Test
    fun normalize_acceptsCommonFormats() {
        assertEquals("2024:05:01 13:04:05", DateParser.normalize("2024:05:01 13:04:05"))
        assertEquals("2024:05:01 13:04:05", DateParser.normalize("2024-05-01 13:04:05"))
        assertEquals("2024:05:01 13:04:05", DateParser.normalize("2024/05/01 13:04:05"))
        assertEquals("2024:05:01 13:04:00", DateParser.normalize("2024-05-01 13:04"))
    }

    @Test
    fun normalize_rejectsInvalidValues() {
        assertNull(DateParser.normalize("not-a-date"))
        assertNull(DateParser.normalize("2024-13-40 99:99:99"))
        assertTrue(DateParser.isValid(""))
        assertFalse(DateParser.isValid("yesterday"))
    }
}

class GpsTagClearListTest {
    @Test
    fun shouldClearGps_onlyWhenBothBlank() {
        assertTrue(GpsTagClearList.shouldClearGps("", ""))
        assertTrue(GpsTagClearList.shouldClearGps("  ", ""))
        assertFalse(GpsTagClearList.shouldClearGps("31.2", ""))
        assertFalse(GpsTagClearList.shouldClearGps("", "121.4"))
    }

    @Test
    fun tags_includeCoreGpsFields() {
        assertTrue(GpsTagClearList.TAGS.contains("GPSLatitude"))
        assertTrue(GpsTagClearList.TAGS.contains("GPSLatitudeRef"))
        assertTrue(GpsTagClearList.TAGS.contains("GPSLongitude"))
        assertTrue(GpsTagClearList.TAGS.contains("GPSLongitudeRef"))
        assertTrue(GpsTagClearList.TAGS.contains("GPSAltitude"))
        assertTrue(GpsTagClearList.TAGS.contains("GPSProcessingMethod"))
    }
}

class FieldValidatorTest {
    @Test
    fun validate_acceptsEmptyFields() {
        assertTrue(FieldValidator.validate(CommonExifFields()).isEmpty())
    }

    @Test
    fun validate_rejectsPartialGpsAndBadDate() {
        val errors = FieldValidator.validate(
            CommonExifFields(
                dateTimeOriginal = "bad",
                gpsLatitude = "31.2",
                orientation = "9",
            ),
        )
        assertTrue(errors.any { it.contains("拍摄时间") })
        assertTrue(errors.any { it.contains("纬度") || it.contains("经度") })
        assertTrue(errors.any { it.contains("方向") })
    }

    @Test
    fun validate_rejectsOutOfRangeCoordinates() {
        val errors = FieldValidator.validate(
            CommonExifFields(gpsLatitude = "100", gpsLongitude = "200"),
        )
        assertTrue(errors.any { it.contains("纬度") })
        assertTrue(errors.any { it.contains("经度") })
    }

    @Test
    fun validate_rejectsBadCustomNumbers() {
        val errors = FieldValidator.validate(
            CommonExifFields(),
            listOf(
                CustomField("PhotographicSensitivity", "iso"),
                CustomField("FNumber", "wide"),
                CustomField("NotARealTag", "1"),
            ),
        )
        assertTrue(errors.any { it.contains("ISO") })
        assertTrue(errors.any { it.contains("光圈") })
        assertTrue(errors.any { it.contains("NotARealTag") })
    }
}

class WritableTagCatalogTest {
    @Test
    fun normalize_keepsTextAndIntegers() {
        assertEquals("音理", WritableTagCatalog.normalize("Artist", " 音理 "))
        assertEquals("100", WritableTagCatalog.normalize("PhotographicSensitivity", "100"))
        assertNull(WritableTagCatalog.normalize("PhotographicSensitivity", "1.5"))
        assertNull(WritableTagCatalog.normalize("Unknown", "1"))
    }

    @Test
    fun normalizeRational_acceptsFractionAndDecimal() {
        assertEquals("1/125", WritableTagCatalog.normalizeRational("1/125"))
        assertEquals("14/5", WritableTagCatalog.normalizeRational("2.8"))
        assertEquals("-1/3", WritableTagCatalog.normalizeRational("-1/3"))
        assertNull(WritableTagCatalog.normalizeRational("1/0"))
        assertNull(WritableTagCatalog.normalizeRational("快门"))
    }
}

class ImageFormatDetectorTest {
    @Test
    fun detect_jpegPngWebpAndUnknown() {
        assertEquals(
            com.neri.exiftools.model.ImageFormat.JPEG,
            ImageFormatDetector.detect(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())),
        )
        assertEquals(
            com.neri.exiftools.model.ImageFormat.PNG,
            ImageFormatDetector.detect(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            ),
        )
        val webp = ByteArray(12)
        "RIFF".toByteArray().copyInto(webp, 0)
        "WEBP".toByteArray().copyInto(webp, 8)
        assertEquals(com.neri.exiftools.model.ImageFormat.WEBP, ImageFormatDetector.detect(webp))
        assertEquals(
            com.neri.exiftools.model.ImageFormat.UNSUPPORTED,
            ImageFormatDetector.detect("ftypheic".toByteArray()),
        )
    }
}

class MediaUrisTest {
    @Test
    fun pickerUri_isDetected() {
        val colorOs = "content://media/picker/0/com.coloros.gallery3d.photopicker/media/2036?requireOriginal=1"
        assertTrue(com.neri.exiftools.data.MediaUris.isPickerUri(colorOs))
        assertFalse(com.neri.exiftools.data.MediaUris.isPickerUri("content://media/external/images/media/12"))
    }
}

class OffsetTimeParserTest {
    @Test
    fun acceptsStandardOffsets() {
        assertTrue(OffsetTimeParser.isValid("+08:00"))
        assertTrue(OffsetTimeParser.isValid("-05:30"))
        assertTrue(OffsetTimeParser.isValid(""))
        assertFalse(OffsetTimeParser.isValid("8:00"))
        assertFalse(OffsetTimeParser.isValid("+25:00"))
    }
}
