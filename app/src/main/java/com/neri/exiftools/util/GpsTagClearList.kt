package com.neri.exiftools.util

object GpsTagClearList {
    val TAGS: List<String> = listOf(
        "GPSVersionID",
        "GPSLatitudeRef",
        "GPSLatitude",
        "GPSLongitudeRef",
        "GPSLongitude",
        "GPSAltitudeRef",
        "GPSAltitude",
        "GPSTimeStamp",
        "GPSSatellites",
        "GPSStatus",
        "GPSMeasureMode",
        "GPSDOP",
        "GPSSpeedRef",
        "GPSSpeed",
        "GPSTrackRef",
        "GPSTrack",
        "GPSImgDirectionRef",
        "GPSImgDirection",
        "GPSMapDatum",
        "GPSDestLatitudeRef",
        "GPSDestLatitude",
        "GPSDestLongitudeRef",
        "GPSDestLongitude",
        "GPSDestBearingRef",
        "GPSDestBearing",
        "GPSDestDistanceRef",
        "GPSDestDistance",
        "GPSProcessingMethod",
        "GPSAreaInformation",
        "GPSDateStamp",
        "GPSDifferential",
        "GPSHPositioningError",
    )

    fun shouldClearGps(latitude: String, longitude: String): Boolean {
        return latitude.isBlank() && longitude.isBlank()
    }
}
